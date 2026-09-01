package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
}