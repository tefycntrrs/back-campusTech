package com.uade.ecommerce.controller;

import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.services.MarcaService;
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
    public ResponseEntity<List<Marca>> getAllMarcas() {
        List<Marca> marcas = marcaService.getAllMarcas();

        if (marcas.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(marcas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Marca> getMarcaById(@PathVariable Long id) {
        return ResponseEntity.ok(marcaService.getMarcaById(id));
    }

    @PostMapping
    public ResponseEntity<Marca> createMarca(
            @RequestBody Marca marca,
            UriComponentsBuilder uriBuilder
    ) {
        Marca creada = marcaService.createMarca(marca);

        URI location = uriBuilder
                .path("/api/marcas/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Marca> updateMarca(
            @PathVariable Long id,
            @RequestBody Marca marca
    ) {
        return ResponseEntity.ok(marcaService.updateMarca(id, marca));
    }
}
