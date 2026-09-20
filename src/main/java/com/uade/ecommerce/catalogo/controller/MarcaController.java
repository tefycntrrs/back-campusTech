package com.uade.ecommerce.catalogo.controller;

import com.uade.ecommerce.catalogo.dto.MarcaRequest;
import com.uade.ecommerce.catalogo.dto.MarcaResponse;
import com.uade.ecommerce.catalogo.service.MarcaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/marcas")
public class MarcaController {

    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping
    public ResponseEntity<List<MarcaResponse>> getAllMarcas() {
        List<MarcaResponse> marcas = marcaService.getAllMarcas();

        if (marcas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(marcas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarcaResponse> getMarcaById(@PathVariable Long id) {
        return ResponseEntity.ok(marcaService.getMarcaById(id));
    }

    @PostMapping
    public ResponseEntity<MarcaResponse> createMarca(
            @Valid @RequestBody MarcaRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        MarcaResponse creada = marcaService.createMarca(request);

        URI location = uriBuilder
                .path("/api/marcas/{id}")
                .buildAndExpand(creada.getMarcaId())
                .toUri();

        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarcaResponse> updateMarca(
            @PathVariable Long id,
            @Valid @RequestBody MarcaRequest request
    ) {
        return ResponseEntity.ok(marcaService.updateMarca(id, request));
    }
}