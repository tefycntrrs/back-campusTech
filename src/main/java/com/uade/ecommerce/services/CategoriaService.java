package com.uade.ecommerce.services;

import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.repository.CategoriaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> getAllCategorias() {
        return categoriaRepository.findAll();
    }

    public Categoria getCategoriaById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "La categoría no existe"
                        )
                );
    }

    public Categoria createCategoria(Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la categoría es obligatorio"
            );
        }

        String nombre = categoria.getNombre().trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La categoría ya existe"
            );
        }

        categoria.setNombre(nombre);
        return categoriaRepository.save(categoria);
    }
}