package com.uade.ecommerce.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Imagen que llega dentro de CreateProductoRequest.imagenes.
 *
 * Solo se maneja la URL (no hay carga de archivos en esta fase).
 *
 * Este DTO se valida únicamente cuando llega dentro de la lista de imágenes (por el @Valid de
 * CreateProductoRequest.imagenes). Ahí toda imagen necesita url, tanto al crear como al
 * actualizar, así que el @NotBlank de la url no choca con el PUT parcial: si el PUT no manda
 * imágenes, este DTO ni siquiera se evalúa. "Al menos una imagen" lo sigue exigiendo
 * ProductoService.
 */
@Data
public class ImagenProductoRequest {

    // La columna producto_imagenes.url tiene largo 500
    @NotBlank(message = "Cada imagen necesita una url")
    @Size(max = 500, message = "La url de la imagen no puede superar los 500 caracteres")
    private String url;

    /** Opcional: si no viene, se usa el orden de llegada en la lista (0 es la primera). */
    @PositiveOrZero(message = "El orden no puede ser negativo")
    private Integer orden;

    /** Opcional: si ninguna imagen lo marca, la primera queda como portada. */
    private Boolean principal;
}