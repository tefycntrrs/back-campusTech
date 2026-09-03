package com.uade.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
// Clase para crear un nuevo producto
public class CreateProductoRequest {

    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private String sku;
    private Boolean activo;
    private Long categoriaId;
    private Long marcaId;
}