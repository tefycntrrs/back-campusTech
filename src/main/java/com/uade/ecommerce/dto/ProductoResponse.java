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

    private Long categoriaId;
    private String categoriaNombre;

    private Long marcaId;
    private String marcaNombre;

    private Long vendedorId;
    private String vendedorNombre;

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

                producto.getVendedor() != null
                        ? producto.getVendedor().getId()
                        : null,

                producto.getVendedor() != null
                        ? producto.getVendedor().getNombre() + " " + producto.getVendedor().getApellido()
                        : null,

                imagenes,

                producto.getCreatedAt(),
                producto.getUpdatedAt()
        );
    }
}
