package com.uade.ecommerce.controller;

import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.services.MarcaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marcas")
public class MarcaController {

    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping
    public List<Marca> getAllMarcas() {
        return marcaService.getAllMarcas();
    }

    @PostMapping
    public Marca createMarca(@RequestBody Marca marca) {
        return marcaService.createMarca(marca);
    }
}