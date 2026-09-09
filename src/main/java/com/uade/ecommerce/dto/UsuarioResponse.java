package com.uade.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uade.ecommerce.model.Sexo;
import com.uade.ecommerce.model.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * DTO de salida del usuario: es lo que devuelven todos los endpoints de /api/usuarios.
 *
 * Hace tres cosas que la entidad Usuario no puede hacer sola:
 * 
 *   no incluye la contraseña, ni siquiera codificada;
 *   agrega la edad, que no está guardada en la base sino calculada desde la fecha de nacimiento;
 *   expone solo los campos públicos, así un cambio en la entidad no cambia la API sin querer.
 * 
 *
 * Es un record: los datos son de solo lectura, no tiene setters.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaNacimiento,
        Integer edad,
        Sexo sexo,
        Boolean activo,
        LocalDateTime createdAt
) {

    /** Convierte la entidad al DTO. Lo usa el controller tanto para uno como para la lista. */
    public static UsuarioResponse from(Usuario usuario) {
        // Period.between calcula los años cumplidos entre la fecha de nacimiento y hoy
        Integer edad = usuario.getFechaNacimiento() == null
                ? null
                : Period.between(usuario.getFechaNacimiento(), LocalDate.now()).getYears();

        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getFechaNacimiento(),
                edad,
                usuario.getSexo(),
                usuario.getActivo(),
                usuario.getCreatedAt()
        );
    }
}
