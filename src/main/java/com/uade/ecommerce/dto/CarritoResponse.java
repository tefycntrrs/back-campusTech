package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Carrito;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CarritoResponse {

    private Long id;

    private String estado;

    private Long usuarioId;

    private String guestToken;

    private List<ItemCarritoResponse> items;

    private BigDecimal total;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static CarritoResponse from(Carrito carrito) {

        List<ItemCarritoResponse> items =
                carrito.getItems()
                        .stream()
                        .map(ItemCarritoResponse::from)
                        .toList();

        BigDecimal total =
                items.stream()
                        .map(ItemCarritoResponse::getSubtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new CarritoResponse(
                carrito.getId(),
                carrito.getEstado().name(),

                carrito.getUsuario() != null
                        ? carrito.getUsuario().getId()
                        : null,

                carrito.getGuestToken(),
                items,
                total,
                carrito.getCreatedAt(),
                carrito.getUpdatedAt()
        );
    }
}