package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.AgregarItemCarritoRequest;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.ProductoNotFoundException;
import com.uade.ecommerce.exception.ResourceNotFoundException;
import com.uade.ecommerce.exception.UsuarioNotFoundException;
import com.uade.ecommerce.model.*;
import com.uade.ecommerce.repository.CarritoRepository;
import com.uade.ecommerce.repository.ItemCarritoRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import com.uade.ecommerce.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ItemCarritoRepository itemCarritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;

    public CarritoService(
            CarritoRepository carritoRepository,
            ItemCarritoRepository itemCarritoRepository,
            UsuarioRepository usuarioRepository,
            ProductoRepository productoRepository
    ) {
        this.carritoRepository = carritoRepository;
        this.itemCarritoRepository =
                itemCarritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
    }

    public Carrito agregarProducto(
            Long usuarioId,
            AgregarItemCarritoRequest request
    ) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(
                        () -> new UsuarioNotFoundException(
                                usuarioId
                        )
                );

        Producto producto = productoRepository
                .findById(request.getProductoId())
                .orElseThrow(
                        () -> new ProductoNotFoundException(
                                request.getProductoId()
                        )
                );

        Carrito carrito = carritoRepository
                .findByUsuarioIdAndEstado(
                        usuarioId,
                        EstadoCarrito.ACTIVO
                )
                .orElseGet(
                        () -> crearCarrito(usuario)
                );

        ItemCarrito item = itemCarritoRepository
                .findByCarritoIdAndProductoId(
                        carrito.getId(),
                        producto.getId()
                )
                .orElse(null);

        int nuevaCantidad;

        if (item == null) {
            nuevaCantidad = request.getCantidad();
        } else {
            nuevaCantidad =
                    item.getCantidad()
                            + request.getCantidad();
        }

        validarStock(
                producto,
                nuevaCantidad
        );

        if (item == null) {

            item = new ItemCarrito();

            item.setCarrito(carrito);
            item.setProducto(producto);
            item.setCantidad(
                    request.getCantidad()
            );
            item.setPrecioReferencia(
                    producto.getPrecio()
            );

            carrito.getItems().add(item);

        } else {

            item.setCantidad(nuevaCantidad);

            // Actualizamos el precio de referencia
            // al precio vigente del producto.
            item.setPrecioReferencia(
                    producto.getPrecio()
            );
        }

        carritoRepository.save(carrito);

        return carrito;
    }

    public Carrito obtenerCarritoActivo(
            Long usuarioId
    ) {

        if (!usuarioRepository.existsById(usuarioId)) {
            throw new UsuarioNotFoundException(usuarioId);
        }

        return carritoRepository
                .findByUsuarioIdAndEstado(
                        usuarioId,
                        EstadoCarrito.ACTIVO
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Carrito",
                                "El usuario no tiene un carrito activo"
                        )
                );
    }

    public Carrito eliminarItem(
            Long usuarioId,
            Long itemId
    ) {

        Carrito carrito =
                obtenerCarritoActivo(usuarioId);

        ItemCarrito item = carrito
                .getItems()
                .stream()
                .filter(
                        actual ->
                                actual.getId()
                                        .equals(itemId)
                )
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "ItemCarrito",
                                "El item con id "
                                        + itemId
                                        + " no pertenece al carrito"
                        )
                );

        carrito.getItems().remove(item);

        carritoRepository.save(carrito);

        return carrito;
    }

    public Carrito vaciarCarrito(
            Long usuarioId
    ) {

        Carrito carrito =
                obtenerCarritoActivo(usuarioId);

        carrito.getItems().clear();

        carritoRepository.save(carrito);

        return carrito;
    }

    private Carrito crearCarrito(
            Usuario usuario
    ) {

        Carrito carrito = new Carrito();

        carrito.setUsuario(usuario);
        carrito.setEstado(
                EstadoCarrito.ACTIVO
        );

        return carritoRepository.save(carrito);
    }

    private void validarStock(
            Producto producto,
            int cantidad
    ) {

        if (cantidad > producto.getStock()) {

            throw new ArgumentInvalidException(
                    "cantidad",
                    "No hay stock suficiente. Stock disponible: "
                            + producto.getStock()
            );
        }
    }
}