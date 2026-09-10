package com.uade.ecommerce.dto;

import com.uade.ecommerce.model.Usuario;


public record LoginResponse(
        String mensaje,
        UsuarioResponse usuario
) {

    public static LoginResponse from(Usuario usuario) {
        return new LoginResponse("Login exitoso", UsuarioResponse.from(usuario));
    }
}
