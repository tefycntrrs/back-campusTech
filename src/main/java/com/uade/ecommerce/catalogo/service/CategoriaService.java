package com.uade.ecommerce.catalogo.service;

import com.uade.ecommerce.catalogo.dto.CategoriaRequest;
import com.uade.ecommerce.catalogo.dto.CategoriaResponse;
import com.uade.ecommerce.catalogo.model.Categoria;
import com.uade.ecommerce.catalogo.repository.CategoriaRepository;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import com.uade.ecommerce.shared.exception.CategoriaNotFoundException;
import com.uade.ecommerce.shared.exception.DuplicateResourceException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaResponse> getAllCategorias() {
        return categoriaRepository.findAll()
                .stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    public CategoriaResponse getCategoriaById(Long id) {
        return CategoriaResponse.from(buscarCategoria(id));
    }

    public CategoriaResponse createCategoria(CategoriaRequest request) {
        String nombre = validarNombre(request.getNombre());

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Categoria", "La categoría ya existe");
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    public CategoriaResponse updateCategoria(Long id, CategoriaRequest request) {
        Categoria categoria = buscarCategoria(id);
        String nombre = validarNombre(request.getNombre());

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DuplicateResourceException("Categoria", "La categoría ya existe");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());

        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    /**
     * Uso interno: devuelve la entidad para poder modificarla en updateCategoria.
     * Es privado a propósito: la entidad Categoria no sale de este service.
     */
    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new CategoriaNotFoundException(id));
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ArgumentInvalidException(
                    "nombre",
                    "El nombre de la categoría es obligatorio"
            );
        }

        return nombre.trim();
    }
}