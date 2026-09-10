package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CategoriaRequest;
import com.uade.ecommerce.dto.CategoriaResponse;
import com.uade.ecommerce.dto.ProductoResponse;
import com.uade.ecommerce.model.Categoria;
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
    public ResponseEntity<List<CategoriaResponse>> getAllCategorias() {
        List<CategoriaResponse> categorias = categoriaService.getAllCategorias()
                .stream()
                .map(CategoriaResponse::from)
                .toList();

        if (categorias.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> getCategoriaById(@PathVariable Long id) {
        return ResponseEntity.ok(CategoriaResponse.from(categoriaService.getCategoriaById(id)));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> createCategoria(
            @RequestBody CategoriaRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Categoria creada = categoriaService.createCategoria(request);

        URI location = uriBuilder
                .path("/api/categorias/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity.created(location).body(CategoriaResponse.from(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> updateCategoria(
            @PathVariable Long id,
            @RequestBody CategoriaRequest request
    ) {
        return ResponseEntity.ok(
                CategoriaResponse.from(categoriaService.updateCategoria(id, request))
        );
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProductoResponse>> getProductosByCategoria(@PathVariable Long id) {
        List<ProductoResponse> productos = productoService.getProductosByCategoria(id)
                .stream()
                .map(ProductoResponse::from)
                .toList();

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }
}
