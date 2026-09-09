package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.MarcaRequest;
import com.uade.ecommerce.dto.MarcaResponse;
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
    public ResponseEntity<List<MarcaResponse>> getAllMarcas() {

        List<MarcaResponse> marcas =
                marcaService.getAllMarcas()
                        .stream()
                        .map(MarcaResponse::from)
                        .toList();

        if (marcas.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NO_CONTENT)
                    .build();
        }

        return ResponseEntity.ok(marcas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MarcaResponse> getMarcaById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                MarcaResponse.from(
                        marcaService.getMarcaById(id)
                )
        );
    }

    @PostMapping
    public ResponseEntity<MarcaResponse> createMarca(
            @RequestBody MarcaRequest request,
            UriComponentsBuilder uriBuilder
    ) {

        Marca marca = new Marca();
        marca.setNombre(request.getNombre());
        marca.setActivo(request.getActivo());

        Marca creada =
                marcaService.createMarca(marca);

        URI location = uriBuilder
                .path("/api/marcas/{id}")
                .buildAndExpand(creada.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(MarcaResponse.from(creada));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarcaResponse> updateMarca(
            @PathVariable Long id,
            @RequestBody MarcaRequest request
    ) {

        Marca marca = new Marca();
        marca.setNombre(request.getNombre());
        marca.setActivo(request.getActivo());

        Marca actualizada =
                marcaService.updateMarca(id, marca);

        return ResponseEntity.ok(
                MarcaResponse.from(actualizada)
        );
    }
}