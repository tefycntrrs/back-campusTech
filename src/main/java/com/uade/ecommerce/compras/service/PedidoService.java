package com.uade.ecommerce.compras.service;

import com.uade.ecommerce.catalogo.model.Producto;
import com.uade.ecommerce.catalogo.repository.ProductoRepository;
import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.model.*;
import com.uade.ecommerce.compras.repository.CarritoRepository;
import com.uade.ecommerce.compras.repository.PedidoRepository;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import com.uade.ecommerce.shared.exception.ForbiddenException;
import com.uade.ecommerce.shared.exception.PedidoNotFoundException;
import com.uade.ecommerce.shared.exception.ResourceNotFoundException;
import com.uade.ecommerce.shared.exception.UsuarioNotFoundException;
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
    private final UsuarioRepository usuarioRepository;

    public PedidoService(
            CarritoRepository carritoRepository,
            ProductoRepository productoRepository,
            PedidoRepository pedidoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.carritoRepository = carritoRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Todo el checkout corre en una sola transaccion: si un producto no pasa
     * la revalidacion de stock, la excepcion hace rollback de lo que ya se
     * habia descontado para los productos anteriores del mismo carrito.
     */
    public PedidoResponse checkout(Long carritoId, Long usuarioId) {

        Carrito carrito = carritoRepository
                .findById(carritoId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Carrito",
                                carritoId
                        )
                );

        // Ítem 18: el carrito tiene que ser del usuario autenticado. Sin esto, conociendo (o
        // adivinando) el id de un carrito ajeno cualquiera podía dispararle el checkout.
        if (!carrito.getUsuario().getId().equals(usuarioId)) {
            throw new ForbiddenException("El carrito pertenece a otro usuario");
        }

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

        return PedidoResponse.from(pedidoGuardado);
    }

    /**
     * Detalle de un pedido: es lo que resuelve el header Location que devuelve el checkout.
     * Solo lo puede ver su dueño (o un ADMIN).
     */
    public PedidoResponse getPedidoById(Long id, Long usuarioId, boolean esAdmin) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));

        verificarPropietario(pedido, usuarioId, esAdmin);

        return PedidoResponse.from(pedido);
    }

    /** Búsqueda por número de pedido (PED-1758300000000). Mismas reglas de acceso. */
    public PedidoResponse getPedidoByNumero(String numero, Long usuarioId, boolean esAdmin) {
        Pedido pedido = pedidoRepository.findByNumero(numero)
                .orElseThrow(() -> new PedidoNotFoundException(numero));

        verificarPropietario(pedido, usuarioId, esAdmin);

        return PedidoResponse.from(pedido);
    }

    /**
     * Un pedido dice qué compró alguien y por cuánto, así que no alcanza con estar logueado:
     * tiene que ser el dueño. El ADMIN queda exceptuado para poder dar soporte.
     */
    private void verificarPropietario(Pedido pedido, Long usuarioId, boolean esAdmin) {
        if (esAdmin) {
            return;
        }

        if (!pedido.getUsuario().getId().equals(usuarioId)) {
            throw new ForbiddenException("El pedido pertenece a otro usuario");
        }
    }

    /**
     * Historial de un usuario, del más nuevo al más viejo. Primero se verifica que el usuario
     * exista (404 si no), para distinguir "usuario inexistente" de "usuario sin pedidos".
     */
    public List<PedidoResponse> getPedidosByUsuario(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new UsuarioNotFoundException(usuarioId);
        }

        return pedidoRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(PedidoResponse::from)
                .toList();
    }

    private String generarNumeroPedido() {
        return "PED-" + System.currentTimeMillis();
    }
}