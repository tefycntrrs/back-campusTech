package com.uade.ecommerce.compras.repository;

import com.uade.ecommerce.compras.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
}
