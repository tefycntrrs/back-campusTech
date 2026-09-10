package com.uade.ecommerce.exception;

/**
 * El usuario pedido no existe. Hereda de {"@link ResourceNotFoundException"}, así que sale como
 * HTTP 404 sin necesidad de un handler propio: lo único que agrega es el mensaje en español.
 *
 * Tiene dos constructores porque al usuario se lo busca de dos formas: por id
 * (GET /api/usuarios/{id}) y por email (GET /api/usuarios/buscar?email=...).
 */
public class UsuarioNotFoundException extends ResourceNotFoundException {

    /** Búsqueda por id: GET /api/usuarios/{id}. */
    public UsuarioNotFoundException(Long id) {
        super("Usuario", "El usuario con id " + id + " no existe");
    }

    /** Búsqueda por email: GET /api/usuarios/buscar?email=... */
    public UsuarioNotFoundException(String email) {
        super("Usuario", "El usuario con email " + email + " no existe");
    }

    /**
     * Búsqueda por username: GET /api/usuarios/buscar?username=...
     * Es un método estático y no un constructor porque email y username son los dos String
     * y Java no puede distinguir dos constructores con la misma firma.
     */
    public static UsuarioNotFoundException porUsername(String username) {
        return new UsuarioNotFoundException("username", username);
    }

    private UsuarioNotFoundException(String campo, String valor) {
        super("Usuario", "El usuario con " + campo + " " + valor + " no existe");
    }
}
