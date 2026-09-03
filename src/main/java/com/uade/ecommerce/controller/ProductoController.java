package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.services.ProductoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    //Constructor de la clase ProductoController
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    //Obtener todos los productos
    @GetMapping
    public List<Producto> getAllProductos() {
        return productoService.getAllProductos();
    }

    //Crear un nuevo producto
    @PostMapping
    public Producto createProducto(@RequestBody CreateProductoRequest request) {
        return productoService.createProducto(request);
    }

    //Actualizar un producto
    @PutMapping("/{id}")
    public Producto updateProducto(
            @PathVariable Long id,
            @RequestBody CreateProductoRequest request
    ) {
        return productoService.updateProducto(id, request);
    }
}
