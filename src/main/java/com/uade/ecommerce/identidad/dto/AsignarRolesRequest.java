package com.uade.ecommerce.identidad.dto;

import com.uade.ecommerce.identidad.model.NombreRol;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Body de PUT /api/usuarios/{id}/roles. Reemplaza todos los roles del usuario.
 * Solo un ADMIN puede usarlo (la ruta está cerrada en SecurityConfig).
 */
@Data
public class AsignarRolesRequest {

    @NotEmpty(message = "Hay que indicar al menos un rol")
    private Set<NombreRol> roles = new LinkedHashSet<>();
}
