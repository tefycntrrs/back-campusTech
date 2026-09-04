package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.DuplicateResourceException;
import com.uade.ecommerce.exception.MarcaNotFoundException;
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

    public Marca getMarcaById(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new MarcaNotFoundException(id));
    }

    public Marca createMarca(Marca marca) {
        String nombre = validarNombre(marca.getNombre());

        if (marcaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Marca", "La marca ya existe");
        }

        marca.setNombre(nombre);
        return marcaRepository.save(marca);
    }

    public Marca updateMarca(Long id, Marca request) {
        Marca marca = getMarcaById(id);
        String nombre = validarNombre(request.getNombre());

        if (marcaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DuplicateResourceException("Marca", "La marca ya existe");
        }

        marca.setNombre(nombre);

        if (request.getActivo() != null) {
            marca.setActivo(request.getActivo());
        }

        return marcaRepository.save(marca);
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ArgumentInvalidException(
                    "nombre",
                    "El nombre de la marca es obligatorio"
            );
        }

        return nombre.trim();
    }
}
