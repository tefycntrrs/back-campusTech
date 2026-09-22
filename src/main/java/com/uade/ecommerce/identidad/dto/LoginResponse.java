package com.uade.ecommerce.identidad.dto;

import com.uade.ecommerce.identidad.model.Usuario;

/**
 * Respuesta del login. Además de los datos del usuario devuelve el JWT, que es lo que el front
 * tiene que guardar y mandar después en cada request como {@code Authorization: Bearer <token>}.
 *
 * <p>{@code tipo} es fijo ("Bearer") y está para que el cliente sepa cómo armar el header sin
 * tener que adivinarlo, y {@code expiraEnMs} para que pueda anticipar el vencimiento.</p>
 */
public record LoginResponse(
        String mensaje,
        String token,
        String tipo,
        long expiraEnMs,
        UsuarioResponse usuario
) {

    private static final String TIPO_BEARER = "Bearer";

    public static LoginResponse from(String token, long expiraEnMs, Usuario usuario) {
        return new LoginResponse(
                "Login exitoso",
                token,
                TIPO_BEARER,
                expiraEnMs,
                UsuarioResponse.from(usuario)
        );
    }
}
