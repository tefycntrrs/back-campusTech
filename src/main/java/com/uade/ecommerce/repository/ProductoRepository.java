package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

}
