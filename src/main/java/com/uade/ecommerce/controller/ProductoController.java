package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CreateProductoRequest;
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

    //Obtener todos los productos -> 200 con la lista, 204 si no hay ninguno
    @GetMapping
    public ResponseEntity<List<Producto>> getAllProductos() {
        List<Producto> productos = productoService.getAllProductos();

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }

    //Obtener un producto por id -> 200, o 404 desde el GlobalExceptionHandler
    @GetMapping("/{id}")
    public ResponseEntity<Producto> getProductoById(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.getProductoById(id));
    }

    //Crear un nuevo producto -> 201 Created + header Location
    @PostMapping
    public ResponseEntity<Producto> createProducto(
            @RequestBody CreateProductoRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Producto producto = productoService.createProducto(request);

        URI location = uriBuilder
                .path("/api/productos/{id}")
                .buildAndExpand(producto.getId())
                .toUri();

        return ResponseEntity.created(location).body(producto);
    }

    //Actualizar un producto -> 200 con el producto actualizado
    @PutMapping("/{id}")
    public ResponseEntity<Producto> updateProducto(
            @PathVariable Long id,
            @RequestBody CreateProductoRequest request
    ) {
        return ResponseEntity.ok(productoService.updateProducto(id, request));
    }
}
