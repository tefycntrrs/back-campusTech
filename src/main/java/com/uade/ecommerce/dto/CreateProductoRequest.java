package com.uade.ecommerce.dto;

import lombok.Data;

@Data
//Clase para crear un nuevo producto
public class CreateProductoRequest {

    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer stock;
    private Long categoriaId;
}
