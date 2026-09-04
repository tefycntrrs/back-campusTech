package com.uade.ecommerce.exception;

/**
 * Categoría de excepción "argumento inválido". El GlobalExceptionHandler la traduce a HTTP 400.
 *
 * Se lanza cuando el JSON llegó bien formado pero los datos rompen una regla de negocio:
 * un nombre vacío, un precio menor o igual a 0, un stock negativo, un usuario menor de 13 años.
 * Es distinta de {[@link DuplicateResourceException} (409): acá el dato está mal, allá el dato
 * está bien pero ya lo usa otro registro.
 *
 * Las validaciones de formato (email, largo mínimo, fecha en el pasado) no se lanzan desde
 * acá: las hace Spring con @Valid sobre los DTO y el handler las traduce igual a un 400.
 */
public class ArgumentInvalidException extends RuntimeException {

    /**
     * Campo del request que causó el error ("precio", "fechaNacimiento"...).
     * El handler lo usa para armar el objeto "errores" de la respuesta, así el front
     * sabe qué input marcar. Queda en null cuando el error no es de un campo puntual.
     */
    private final String campo;

    /** Para errores que no corresponden a un campo específico. */
    public ArgumentInvalidException(String mensaje) {
        super(mensaje);
        this.campo = null;
    }

    /** Para errores de un campo concreto: la respuesta sale con "errores": { campo: mensaje }. */
    public ArgumentInvalidException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
