package com.uade.ecommerce.exception;


public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
