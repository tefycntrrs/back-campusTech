package com.uade.ecommerce.compras.repository;

import com.uade.ecommerce.compras.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Historial de compras de un usuario, del pedido más nuevo al más viejo. */
    List<Pedido> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    /** El número de pedido es único (columna unique), así que hay como mucho un resultado. */
    Optional<Pedido> findByNumero(String numero);
}