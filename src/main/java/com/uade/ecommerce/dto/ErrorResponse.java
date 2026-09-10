package com.uade.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de salida de los errores: el cuerpo único que devuelve la API cuando algo falla.
 *
 * <p>Lo arma siempre el GlobalExceptionHandler, nunca un controller. Gracias a esto todos los
 * errores tienen la misma forma (404, 400, 409 o 500), y el front puede leerlos de una sola manera.</p>
 *
 * <p>El @JsonInclude(NON_NULL) hace que los campos en null no aparezcan en el JSON: así un 404
 * sale limpio y solo los errores de campos traen el mapa "errores".</p>
 *
 * <pre>
 * {
 *   "timestamp": "2026-09-03T21:00:31.178",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Hay campos inválidos en la petición",
 *   "path": "/api/usuarios/registro",
 *   "errores": { "email": "El email no tiene un formato válido" }
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        /* Momento en que se produjo el error */
        LocalDateTime timestamp,
        /* Código HTTP numérico: 400, 404, 409, 500 */
        int status,
        /* Nombre del código HTTP: "Bad Request", "Not Found"... */
        String error,
        /* Explicación en español de qué pasó */
        String message,
        /* URL que se estaba pidiendo, para ubicar el error */
        String path,
        /* Detalle campo por campo. Va en null (y no se serializa) si el error no es de campos */
        Map<String, String> errores
) {

    /** Error general: sin detalle por campo (404, 409, 500). */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    /** Error de validación: incluye el mapa campo -> mensaje. */
    public static ErrorResponse of(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> errores
    ) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, errores);
    }
}
