package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemCarritoRepository
        extends JpaRepository<ItemCarrito, Long> {

    Optional<ItemCarrito> findByCarritoIdAndProductoId(
            Long carritoId,
            Long productoId
    );
}