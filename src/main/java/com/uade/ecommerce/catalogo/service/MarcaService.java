package com.uade.ecommerce.catalogo.service;

import com.uade.ecommerce.catalogo.dto.MarcaRequest;
import com.uade.ecommerce.catalogo.dto.MarcaResponse;
import com.uade.ecommerce.catalogo.model.Marca;
import com.uade.ecommerce.catalogo.repository.MarcaRepository;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import com.uade.ecommerce.shared.exception.DuplicateResourceException;
import com.uade.ecommerce.shared.exception.MarcaNotFoundException;
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

    public List<MarcaResponse> getAllMarcas() {
        return marcaRepository.findAll()
                .stream()
                .map(MarcaResponse::from)
                .toList();
    }

    public MarcaResponse getMarcaById(Long id) {
        return MarcaResponse.from(buscarMarca(id));
    }

    public MarcaResponse createMarca(MarcaRequest request) {
        String nombre = validarNombre(request.getNombre());

        if (marcaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Marca", "La marca ya existe");
        }

        Marca marca = new Marca();
        marca.setNombre(nombre);
        if (request.getActivo() != null) {
            marca.setActivo(request.getActivo());
        }

        return MarcaResponse.from(marcaRepository.save(marca));
    }

    public MarcaResponse updateMarca(Long id, MarcaRequest request) {
        Marca marca = buscarMarca(id);
        String nombre = validarNombre(request.getNombre());

        if (marcaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DuplicateResourceException("Marca", "La marca ya existe");
        }

        marca.setNombre(nombre);

        if (request.getActivo() != null) {
            marca.setActivo(request.getActivo());
        }

        return MarcaResponse.from(marcaRepository.save(marca));
    }

    /**
     * Uso interno: devuelve la entidad para poder trabajar con ella (por ejemplo, modificarla
     * en updateMarca). Es privado a propósito: la entidad Marca no sale de este service,
     * hacia afuera solo viajan DTOs.
     */
    private Marca buscarMarca(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new MarcaNotFoundException(id));
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