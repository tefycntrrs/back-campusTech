package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private String sku;
    private Boolean activo;

    private Long categoriaId;
    private String categoriaNombre;

    private Long marcaId;
    private String marcaNombre;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductoResponse from(Producto producto) {

        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getSku(),
                producto.getActivo(),

                producto.getCategoria() != null
                        ? producto.getCategoria().getId()
                        : null,

                producto.getCategoria() != null
                        ? producto.getCategoria().getNombre()
                        : null,

                producto.getMarca() != null
                        ? producto.getMarca().getId()
                        : null,

                producto.getMarca() != null
                        ? producto.getMarca().getNombre()
                        : null,

                producto.getCreatedAt(),
                producto.getUpdatedAt()
        );
    }
}