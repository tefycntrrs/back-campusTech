package com.uade.ecommerce.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaRequest {

    @NotBlank(message = "El nombre de categoria es obligatorio.")
    @Size(max = 100, message = "El nombre de categoria no puede superar los 100 caracteres.")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
    private String descripcion;
    private Boolean activo;

}
