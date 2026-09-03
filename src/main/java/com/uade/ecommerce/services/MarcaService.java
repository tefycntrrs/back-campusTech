package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.MarcaNotFoundException;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.repository.MarcaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Marca getMarcaById(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new MarcaNotFoundException(id));
    }

    public Marca createMarca(Marca marca) {
        if (marca.getNombre() == null || marca.getNombre().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la marca es obligatorio"
            );
        }

        String nombre = marca.getNombre().trim();

        if (marcaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La marca ya existe"
            );
        }

        marca.setNombre(nombre);
        return marcaRepository.save(marca);
    }

    public Marca updateMarca(Long id, Marca request) {
        Marca marca = getMarcaById(id);

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la marca es obligatorio"
            );
        }

        String nombre = request.getNombre().trim();

        if (marcaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La marca ya existe"
            );
        }

        marca.setNombre(nombre);

        if (request.getActivo() != null) {
            marca.setActivo(request.getActivo());
        }

        return marcaRepository.save(marca);
    }
}
