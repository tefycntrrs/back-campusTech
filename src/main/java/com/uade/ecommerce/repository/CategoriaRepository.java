package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

// Repositorio de la clase Categoria
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
}