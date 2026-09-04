package com.uade.ecommerce.controller;

import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.services.CategoriaService;
import com.uade.ecommerce.services.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final ProductoService productoService;

    public CategoriaController(
            CategoriaService categoriaService,
            ProductoService productoService
    ) {
        this.categoriaService = categoriaService;
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<List<Categoria>> getAllCategorias() {
        List<Categoria> categorias = categoriaService.getAllCategorias();

        if (categorias.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> getCategoriaById(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.getCategoriaById(id));
    }

    @PostMapping
    public ResponseEntity<Categoria> createCategoria(
            @RequestBody Categoria categoria,
            UriComponentsBuilder uriBuilder
    ) {
        Categoria creada = categoriaService.createCategoria(categoria);

        URI location = uriBuilder
                .path("/api/categorias/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> updateCategoria(
            @PathVariable Long id,
            @RequestBody Categoria categoria
    ) {
        return ResponseEntity.ok(categoriaService.updateCategoria(id, categoria));
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> getProductosByCategoria(@PathVariable Long id) {
        List<Producto> productos = productoService.getProductosByCategoria(id);

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }
}
