package com.uade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Sexo declarado por el usuario en el registro.
 */
public enum Sexo {

    MASCULINO,
    FEMENINO,
    OTRO,
    PREFIERO_NO_DECIR;

    /**
     * Acepta el valor sin importar mayúsculas/minúsculas y, si no existe,
     * devuelve un mensaje con las opciones válidas en lugar del error de Jackson.
     */
    @JsonCreator
    public static Sexo fromValor(String valor) {
        if (valor == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(sexo -> sexo.name().equalsIgnoreCase(valor.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "sexo debe ser uno de: " + Arrays.stream(values())
                                .map(Enum::name)
                                .collect(Collectors.joining(", "))
                ));
    }
}
