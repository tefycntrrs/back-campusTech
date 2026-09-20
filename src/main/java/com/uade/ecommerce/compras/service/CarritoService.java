package com.uade.ecommerce.compras.service;

import com.uade.ecommerce.catalogo.model.Producto;
import com.uade.ecommerce.catalogo.repository.ProductoRepository;
import com.uade.ecommerce.compras.dto.AgregarItemCarritoRequest;
import com.uade.ecommerce.compras.dto.CarritoResponse;
import com.uade.ecommerce.compras.model.Carrito;
import com.uade.ecommerce.compras.model.EstadoCarrito;
import com.uade.ecommerce.compras.model.ItemCarrito;
import com.uade.ecommerce.compras.repository.CarritoRepository;
import com.uade.ecommerce.compras.repository.ItemCarritoRepository;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.shared.exception.ProductoNotFoundException;
import com.uade.ecommerce.shared.exception.ResourceNotFoundException;
import com.uade.ecommerce.shared.exception.UsuarioNotFoundException;
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

    public CarritoResponse agregarProducto(
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

        ValidacionesStock.validarCantidadPositiva(
                request.getCantidad()
        );

        ValidacionesStock.validarProductoActivo(producto);

        int nuevaCantidad;

        if (item == null) {
            nuevaCantidad = request.getCantidad();
        } else {
            nuevaCantidad =
                    item.getCantidad()
                            + request.getCantidad();
        }

        ValidacionesStock.validarStockSuficiente(
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

        return CarritoResponse.from(carrito);
    }

    public CarritoResponse obtenerCarritoActivo(
            Long usuarioId
    ) {

        return CarritoResponse.from(
                buscarCarritoActivo(usuarioId)
        );
    }

    public CarritoResponse eliminarItem(
            Long usuarioId,
            Long itemId
    ) {

        Carrito carrito =
                buscarCarritoActivo(usuarioId);

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

        return CarritoResponse.from(carrito);
    }

    public CarritoResponse vaciarCarrito(
            Long usuarioId
    ) {

        Carrito carrito =
                buscarCarritoActivo(usuarioId);

        carrito.getItems().clear();

        carritoRepository.save(carrito);

        return CarritoResponse.from(carrito);
    }

    /**
     * Uso interno: devuelve la entidad Carrito para poder modificarla (eliminarItem y
     * vaciarCarrito pasan por acá). Es privado a propósito: la entidad no sale del service,
     * hacia afuera solo viajan DTOs.
     */
    private Carrito buscarCarritoActivo(
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
}