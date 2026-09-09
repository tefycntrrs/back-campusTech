package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Marca;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarcaResponse {

    private Long marcaId;
    private String nombre;
    private Boolean activo;

    public static MarcaResponse from(Marca marca) {
        return new MarcaResponse(
                marca.getId(),
                marca.getNombre(),
                marca.getActivo()
        );
    }
}