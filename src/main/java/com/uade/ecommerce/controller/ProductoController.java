package com.uade.ecommerce.controller;

import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.services.ProductoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // GET http://localhost:8080/api/productos
    @GetMapping
    public List<Producto> getAllProductos() {
        return productoService.getAllProductos();
    }

    // POST http://localhost:8080/api/productos
    @PostMapping
    public Producto createProducto(@RequestBody Producto producto) {
        return productoService.createProducto(producto);
    }
}
