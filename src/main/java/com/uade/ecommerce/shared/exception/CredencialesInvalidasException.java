package com.uade.ecommerce.shared.exception;


public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
