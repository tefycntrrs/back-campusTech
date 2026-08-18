package com.uade.ecommerce.controller;

import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.services.CategoriaService;
import com.uade.ecommerce.services.ProductoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final ProductoService productoService;

    //Constructor de la clase CategoriaController
    public CategoriaController(CategoriaService categoriaService, ProductoService productoService) {
        this.categoriaService = categoriaService;
        this.productoService = productoService;
    }

    //Obtener todas las categorias
    @GetMapping
    public List<Categoria> getAllCategorias() {
        return categoriaService.getAllCategorias();
    }

    //Obtener una categoria por su id
    @GetMapping("/{id}")
    public Categoria getCategoriaById(@PathVariable Long id) {
        return categoriaService.getCategoriaById(id);
    }

    //Crear una nueva categoria
    @PostMapping
    public Categoria createCategoria(@RequestBody Categoria categoria) {
        return categoriaService.createCategoria(categoria);
    }

    //Obtener todos los productos de una categoria
    @GetMapping("/{id}/productos")
    public List<Producto> getProductosByCategoria(@PathVariable Long id) {
        return productoService.getProductosByCategoria(id);
    }
}
