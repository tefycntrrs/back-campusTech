package com.uade.ecommerce.exception;

public class ProductoNotFoundException extends RuntimeException {

    public ProductoNotFoundException(Long id) {
        super("El producto con id " + id + " no existe");
    }
}
