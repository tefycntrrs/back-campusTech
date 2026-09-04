package com.uade.ecommerce.exception;

/**
 * Categoría de excepción "recurso no encontrado". El GlobalExceptionHandler la traduce a HTTP 404.
 *
 * Se lanza cuando el cliente pide algo que no está en la base: un producto, una categoría,
 * una marca, un usuario o, más adelante, un pedido. Es la clase base de las excepciones
 * específicas de cada entidad ({"@link ProductoNotFoundException}, {"@link UsuarioNotFoundException},
 * etc.), así que el handler necesita un solo @ExceptionHandler para todas: si mañana se agrega
 * PedidoNotFoundException, funciona sin tocar el handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    /** Nombre de la entidad que no se encontró ("Producto", "Usuario"...). Sirve para loguear. */
    private final String recurso;

    /** Arma el mensaje solo: "El recurso Pedido con id 7 no existe". */
    public ResourceNotFoundException(String recurso, Object id) {
        super("El recurso " + recurso + " con id " + id + " no existe");
        this.recurso = recurso;
    }

    /** Igual que el anterior, pero con un mensaje escrito a mano (lo usan las subclases). */
    public ResourceNotFoundException(String recurso, String mensaje) {
        super(mensaje);
        this.recurso = recurso;
    }

    public String getRecurso() {
        return recurso;
    }
}
