package com.uade.ecommerce.exception;

/**
 * Categoría de excepción "operación no permitida". El GlobalExceptionHandler la traduce a HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String mensaje) {
        super(mensaje);
    }
}
