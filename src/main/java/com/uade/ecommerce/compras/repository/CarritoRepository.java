package com.uade.ecommerce.compras.repository;

import com.uade.ecommerce.compras.model.Carrito;
import com.uade.ecommerce.compras.model.EstadoCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoRepository
        extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioIdAndEstado(
            Long usuarioId,
            EstadoCarrito estado
    );
}