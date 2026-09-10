package com.uade.ecommerce.dto;

import lombok.Data;

/**
 * Imagen que llega dentro de CreateProductoRequest.imagenes.
 *
 * Solo se maneja la URL (no hay carga de archivos en esta fase). Sin anotaciones de
 * validación, por el mismo motivo que CreateProductoRequest: el DTO se reusa en el PUT
 * parcial. Las reglas ("la url es obligatoria", "al menos una imagen") las aplica
 * ProductoService, que distingue crear de actualizar.
 */
@Data
public class ImagenProductoRequest {

    private String url;

    /** Opcional: si no viene, se usa el orden de llegada en la lista. */
    private Integer orden;

    /** Opcional: si ninguna imagen lo marca, la primera queda como portada. */
    private Boolean principal;
}
