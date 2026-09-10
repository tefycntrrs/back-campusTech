package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Pedido;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class PedidoResponse {

    private Long id;
    private String numero;
    private String estado;
    private Long usuarioId;
    private List<DetallePedidoResponse> detalles;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime createdAt;

    public static PedidoResponse from(Pedido pedido) {

        List<DetallePedidoResponse> detalles =
                pedido.getDetalles()
                        .stream()
                        .map(DetallePedidoResponse::from)
                        .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getNumero(),
                pedido.getEstado().name(),

                pedido.getUsuario() != null
                        ? pedido.getUsuario().getId()
                        : null,

                detalles,
                pedido.getSubtotal(),
                pedido.getTotal(),
                pedido.getCreatedAt()
        );
    }
}
