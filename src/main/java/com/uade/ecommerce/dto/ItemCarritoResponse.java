package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.ItemCarrito;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ItemCarritoResponse {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private Integer stockDisponible;

    private BigDecimal precioReferencia;
    private BigDecimal subtotal;

    public static ItemCarritoResponse from(ItemCarrito item) {

        BigDecimal subtotal =
                item.getPrecioReferencia()
                        .multiply(
                                BigDecimal.valueOf(item.getCantidad())
                        );

        return new ItemCarritoResponse(
                item.getId(),
                item.getProducto().getId(),
                item.getProducto().getNombre(),
                item.getCantidad(),

                // Siempre toma el stock actual del producto
                item.getProducto().getStock(),

                item.getPrecioReferencia(),
                subtotal
        );
    }
}