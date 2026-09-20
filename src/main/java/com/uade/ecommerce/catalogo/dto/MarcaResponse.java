package com.uade.ecommerce.catalogo.dto;

import com.uade.ecommerce.catalogo.model.Marca;
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