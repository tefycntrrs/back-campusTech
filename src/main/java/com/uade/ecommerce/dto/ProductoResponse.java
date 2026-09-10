package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    private List<Long> categoriaIds;

    private Long marcaId;
    private String marcaNombre;

    private Long vendedorId;
    private String vendedorUsername;

    private List<ImagenProductoResponse> imagenes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductoResponse from(Producto producto) {

        List<ImagenProductoResponse> imagenes = producto.getImagenes()
                .stream()
                .map(ImagenProductoResponse::from)
                .toList();

        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getSku(),
                producto.getActivo(),

                producto.getCategoriaIds(),

                producto.getMarca() != null
                        ? producto.getMarca().getId()
                        : null,

                producto.getMarca() != null
                        ? producto.getMarca().getNombre()
                        : null,

                producto.getVendedorId(),

                producto.getVendedorUsername(),

                imagenes,

                producto.getCreatedAt(),
                producto.getUpdatedAt()
        );
    }
}
