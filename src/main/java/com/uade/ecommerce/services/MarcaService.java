package com.uade.ecommerce.services;

import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.repository.MarcaRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaService(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    public List<Marca> getAllMarcas() {
        return marcaRepository.findAll();
    }

    public Marca createMarca(Marca marca) {
        return marcaRepository.save(marca);
    }
}