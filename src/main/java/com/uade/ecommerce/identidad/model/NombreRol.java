package com.uade.ecommerce.identidad.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Roles que puede tener un usuario dentro de la app.
 */
public enum NombreRol {
    USER,
    ADMIN,
    VENDEDOR;

    @JsonCreator
    public static NombreRol fromValor(String valor) {
        if (valor == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(rol -> rol.name().equalsIgnoreCase(valor.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "rol debe ser uno de: " + Arrays.stream(values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))
                ));
    }
}
