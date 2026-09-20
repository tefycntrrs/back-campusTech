package com.uade.ecommerce.catalogo.controller;

import com.uade.ecommerce.catalogo.dto.CategoriaRequest;
import com.uade.ecommerce.catalogo.dto.CategoriaResponse;
import com.uade.ecommerce.catalogo.dto.ProductoResponse;
import com.uade.ecommerce.catalogo.service.CategoriaService;
import com.uade.ecommerce.catalogo.service.ProductoService;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<CategoriaResponse>> getAllCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getAllCategorias();

        if (categorias.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> getCategoriaById(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.getCategoriaById(id));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> createCategoria(
            @Valid @RequestBody CategoriaRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        CategoriaResponse creada = categoriaService.createCategoria(request);

        URI location = uriBuilder
                .path("/api/categorias/{id}")
                .buildAndExpand(creada.getCategoriaId())
                .toUri();

        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> updateCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request
    ) {
        return ResponseEntity.ok(categoriaService.updateCategoria(id, request));
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable Long id) {
        List<ProductoResponse> productos = productoService.getProductosByCategoria(id);

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }
}