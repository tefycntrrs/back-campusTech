package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Categoria;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoriaResponse {

    private Long categoriaId;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActivo()
        );
    }
}