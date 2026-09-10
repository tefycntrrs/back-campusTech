package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.ResourceNotFoundException;
import com.uade.ecommerce.model.Carrito;
import com.uade.ecommerce.model.DetallePedido;
import com.uade.ecommerce.model.EstadoCarrito;
import com.uade.ecommerce.model.ItemCarrito;
import com.uade.ecommerce.model.Pedido;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.CarritoRepository;
import com.uade.ecommerce.repository.PedidoRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PedidoService {

    private final CarritoRepository carritoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;

    public PedidoService(
            CarritoRepository carritoRepository,
            ProductoRepository productoRepository,
            PedidoRepository pedidoRepository
    ) {
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Todo el checkout corre en una sola transaccion: si un producto no pasa
     * la revalidacion de stock, la excepcion hace rollback de lo que ya se
     * habia descontado para los productos anteriores del mismo carrito.
     */
    public Pedido checkout(Long carritoId) {

        Carrito carrito = carritoRepository
                .findById(carritoId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Carrito",
                                carritoId
                        )
                );

        if (carrito.getEstado() != EstadoCarrito.ACTIVO) {
            throw new ArgumentInvalidException(
                    "carritoId",
                    "El carrito ya fue procesado o no esta activo"
            );
        }

        if (carrito.getItems().isEmpty()) {
            throw new ArgumentInvalidException(
                    "carritoId",
                    "El carrito no tiene items para procesar el checkout"
            );
        }

        // Paso 1: releer cada producto desde la base y revalidar stock,
        // porque pudo cambiar desde que se agrego al carrito. Se valida
        // todo antes de descontar nada.
        List<Producto> productosActuales = new ArrayList<>();

        for (ItemCarrito item : carrito.getItems()) {

            Producto producto = productoRepository
                    .findById(item.getProducto().getId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException(
                                    "Producto",
                                    item.getProducto().getId()
                            )
                    );

            ValidacionesStock.validarProductoActivo(producto);
            ValidacionesStock.validarStockSuficiente(
                    producto,
                    item.getCantidad()
            );

            productosActuales.add(producto);
        }

        // Paso 2: recien ahora que TODOS los items pasaron la validacion,
        // se copian cantidad/precio a cada DetallePedido y se descuenta stock.
        Pedido pedido = new Pedido();
        pedido.setNumero(generarNumeroPedido());
        pedido.setUsuario(carrito.getUsuario());

        BigDecimal subtotalPedido = BigDecimal.ZERO;

        for (int i = 0; i < carrito.getItems().size(); i++) {

            ItemCarrito item = carrito.getItems().get(i);
            Producto producto = productosActuales.get(i);

            BigDecimal precioUnitario = producto.getPrecio();
            BigDecimal subtotalItem = precioUnitario.multiply(
                    BigDecimal.valueOf(item.getCantidad())
            );

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalItem);

            pedido.getDetalles().add(detalle);

            producto.setStock(
                    producto.getStock() - item.getCantidad()
            );
            productoRepository.save(producto);

            subtotalPedido = subtotalPedido.add(subtotalItem);
        }

        // Por ahora no hay costos adicionales (envio, descuentos), asi que
        // el total del pedido coincide con el subtotal de sus items.
        pedido.setSubtotal(subtotalPedido);
        pedido.setTotal(subtotalPedido);

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        carrito.setEstado(EstadoCarrito.FINALIZADO);
        carritoRepository.save(carrito);

        return pedidoGuardado;
    }

    private String generarNumeroPedido() {
        return "PED-" + System.currentTimeMillis();
    }
}
