package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.DuplicateResourceException;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.repository.CategoriaRepository;
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

    public List<Categoria> getAllCategorias() {
        return categoriaRepository.findAll();
    }

    public Categoria getCategoriaById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new CategoriaNotFoundException(id));
    }

    public Categoria createCategoria(Categoria categoria) {
        String nombre = validarNombre(categoria.getNombre());

        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new DuplicateResourceException("Categoria", "La categoría ya existe");
        }

        categoria.setNombre(nombre);
        return categoriaRepository.save(categoria);
    }

    public Categoria updateCategoria(Long id, Categoria request) {
        Categoria categoria = getCategoriaById(id);
        String nombre = validarNombre(request.getNombre());

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new DuplicateResourceException("Categoria", "La categoría ya existe");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(request.getDescripcion());

        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        return categoriaRepository.save(categoria);
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
