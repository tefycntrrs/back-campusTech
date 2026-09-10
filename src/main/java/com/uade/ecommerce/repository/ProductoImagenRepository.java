package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio de la clase ProductoImagen
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProductoIdOrderByOrdenAsc(Long productoId);
}
