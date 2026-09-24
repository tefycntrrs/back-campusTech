package com.uade.ecommerce.catalogo.controller;

import com.uade.ecommerce.catalogo.dto.CreateProductoRequest;
import com.uade.ecommerce.catalogo.dto.ProductoResponse;
import com.uade.ecommerce.catalogo.service.ProductoService;
import com.uade.ecommerce.identidad.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Endpoints del catálogo.
 *
 * <p>Los GET son públicos (se puede mirar el catálogo sin cuenta); publicar, modificar y dar de
 * baja piden estar autenticado, y quién es el usuario sale del token, nunca del request. Antes
 * PUT y DELETE recibían {@code ?usuarioId=...}: eso no era seguridad, porque cualquiera podía
 * escribir el id del dueño y editarle el producto (ítem 17).</p>
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final UsuarioAutenticado usuarioAutenticado;

    public ProductoController(
            ProductoService productoService,
            UsuarioAutenticado usuarioAutenticado
    ) {
        this.productoService = productoService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    //Obtener el catálogo -> 200 con los activos ordenados por nombre, 204 si no hay ninguno
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getCatalogo() {

        List<ProductoResponse> productos = productoService.getCatalogo();

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }

    //Obtener un producto por id -> 200, o 404 desde el GlobalExceptionHandler
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.getProductoPublico(id));
    }

    //Cantidad de productos activos en el catálogo -> 200 con el numero
    @GetMapping("/total")
    public ResponseEntity<Long> getTotalActivos() {
        return ResponseEntity.ok(productoService.getTotalActivos());
    }

    //Crear un nuevo producto -> 201 Created + header Location. El vendedor es quien está logueado
    @PostMapping
    public ResponseEntity<ProductoResponse> createProducto(
            @Valid @RequestBody CreateProductoRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        ProductoResponse producto = productoService.createProducto(
                request,
                usuarioAutenticado.idRequerido()
        );

        URI location = uriBuilder
                .path("/api/productos/{id}")
                .buildAndExpand(producto.getId())
                .toUri();

        return ResponseEntity.created(location).body(producto);
    }

    //Actualizar un producto -> 200 con el producto actualizado (403 si no es el dueño)
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> updateProducto(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductoRequest request
    ) {
        return ResponseEntity.ok(productoService.updateProducto(
                id,
                usuarioAutenticado.idRequerido(),
                request
        ));
    }

    //Dar de baja un producto (soft delete) -> 204 (403 si no es el dueño)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id, usuarioAutenticado.idRequerido());

        return ResponseEntity.noContent().build();
    }
}
