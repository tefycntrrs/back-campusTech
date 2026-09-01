package com.uade.ecommerce.exception;

public class MarcaNotFoundException extends RuntimeException {
    public MarcaNotFoundException(Long id) {
        super("La marca con id " + id + " no existe");
    }
}
