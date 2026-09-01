package com.uade.ecommerce.exception;

public class CategoriaNotFoundException extends RuntimeException {

    public CategoriaNotFoundException(Long id) {
        super("La categoría con id " + id + " no existe");
    }
}