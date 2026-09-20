package com.uade.ecommerce.catalogo.repository;

import com.uade.ecommerce.catalogo.model.ProductoImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositorio de la clase ProductoImagen
@Repository
public interface ProductoImagenRepository extends JpaRepository<ProductoImagen, Long> {

    List<ProductoImagen> findByProductoIdOrderByOrdenAsc(Long productoId);
}
