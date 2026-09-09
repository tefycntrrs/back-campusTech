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

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> getAllProductos() {

        List<ProductoResponse> productos = productoService
                .getAllProductos()
                .stream()
                .map(ProductoResponse::from)
                .toList();

        if (productos.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        }

        return ResponseEntity.ok(productos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> getProductoById(
            @PathVariable Long id
    ) {

        Producto producto = productoService.getProductoById(id);

        return ResponseEntity.ok(
                ProductoResponse.from(producto)
        );
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> createProducto(
            @RequestBody CreateProductoRequest request,
            UriComponentsBuilder uriBuilder
    ) {

        Producto producto =
                productoService.createProducto(request);

        URI location = uriBuilder
                .path("/api/productos/{id}")
                .buildAndExpand(producto.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(ProductoResponse.from(producto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> updateProducto(
            @PathVariable Long id,
            @RequestBody CreateProductoRequest request
    ) {

        Producto producto =
                productoService.updateProducto(id, request);

        return ResponseEntity.ok(
                ProductoResponse.from(producto)
        );
    }
}