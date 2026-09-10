package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.ProductoImagen;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImagenProductoResponse {

    private Long id;
    private String url;
    private Integer orden;
    private Boolean principal;

    public static ImagenProductoResponse from(ProductoImagen imagen) {
        return new ImagenProductoResponse(
                imagen.getId(),
                imagen.getUrl(),
                imagen.getOrden(),
                imagen.getPrincipal()
        );
    }
}
