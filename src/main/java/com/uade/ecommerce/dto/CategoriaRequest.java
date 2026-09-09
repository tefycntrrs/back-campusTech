package com.uade.ecommerce.dto;

import lombok.Data;

@Data
public class CategoriaRequest {

    private String nombre;
    private String descripcion;
    private Boolean activo;

}
