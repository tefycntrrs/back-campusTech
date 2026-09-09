package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Carrito;
import com.uade.ecommerce.model.EstadoCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository
        extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioIdAndEstado(
            Long usuarioId,
            EstadoCarrito estado
    );
}