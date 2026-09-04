package com.uade.ecommerce.exception;

/**
 * Categoría de excepción "recurso duplicado". El GlobalExceptionHandler la traduce a HTTP 409.
 *
 * Se lanza cuando se intenta guardar un valor que tiene que ser único y ya está usado:
 * el email de un usuario, el SKU de un producto, el nombre de una categoría o de una marca.
 * Los servicios la chequean antes de guardar (con los métodos existsBy... del repositorio),
 * así el error sale con un mensaje claro en vez de reventar contra la restricción UNIQUE
 * de la base de datos.
 */
public class DuplicateResourceException extends RuntimeException {

    /** Nombre de la entidad duplicada ("Usuario", "Producto"...). */
    private final String recurso;

    /** Arma el mensaje solo: "Ya existe Usuario con email 'pedro@uade.edu.ar'". */
    public DuplicateResourceException(String recurso, String campo, Object valor) {
        super("Ya existe " + recurso + " con " + campo + " '" + valor + "'");
        this.recurso = recurso;
    }

    /** Igual que el anterior, pero con un mensaje escrito a mano. */
    public DuplicateResourceException(String recurso, String mensaje) {
        super(mensaje);
        this.recurso = recurso;
    }

    public String getRecurso() {
        return recurso;
    }
}
