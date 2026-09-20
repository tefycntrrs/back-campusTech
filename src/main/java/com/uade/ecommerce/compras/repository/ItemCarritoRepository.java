package com.uade.ecommerce.compras.repository;

import com.uade.ecommerce.compras.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ItemCarritoRepository
        extends JpaRepository<ItemCarrito, Long> {

    Optional<ItemCarrito> findByCarritoIdAndProductoId(
            Long carritoId,
            Long productoId
    );
}