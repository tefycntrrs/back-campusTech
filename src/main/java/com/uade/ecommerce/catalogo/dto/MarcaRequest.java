package com.uade.ecommerce.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MarcaRequest {

    @NotBlank(message = "La marca es obligatoria.")
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres.")
    private String nombre;

    private Boolean activo;
}