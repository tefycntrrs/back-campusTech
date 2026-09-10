package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.dto.ProductoResponse;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.services.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    //Constructor de la clase ProductoController
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    //Obtener el catálogo -> 200 con los activos ordenados por nombre, 204 si no hay ninguno
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getCatalogo() {

        List<ProductoResponse> productos = productoService
                .getCatalogo()
                .stream()
                .map(ProductoResponse::from)
                .toList();

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }

    //Obtener un producto por id -> 200, o 404 desde el GlobalExceptionHandler
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(@PathVariable Long id) {
        Producto producto = productoService.getProductoPublico(id);
        return ResponseEntity.ok(ProductoResponse.from(producto));
    }

    //Crear un nuevo producto -> 201 Created + header Location
    @PostMapping
    public ResponseEntity<ProductoResponse> createProducto(
            @RequestBody CreateProductoRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Producto producto = productoService.createProducto(request);

        URI location = uriBuilder
                .path("/api/productos/{id}")
                .buildAndExpand(producto.getId())
                .toUri();

        return ResponseEntity.created(location).body(ProductoResponse.from(producto));
    }

    //Actualizar un producto -> 200 con el producto actualizado (403 si no es el dueño)
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> updateProducto(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId,
            @RequestBody CreateProductoRequest request
    ) {
        Producto producto = productoService.updateProducto(id, usuarioId, request);
        return ResponseEntity.ok(ProductoResponse.from(producto));
    }

    //Dar de baja un producto (soft delete) -> 204 (403 si no es el dueño)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProducto(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId
    ) {
        productoService.eliminarProducto(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
