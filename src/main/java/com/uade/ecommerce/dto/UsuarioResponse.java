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
 * <p>Hace tres cosas que la entidad Usuario no puede hacer sola:</p>
 * <ul>
 *   <li>no incluye la contraseña, ni siquiera codificada;</li>
 *   <li>agrega la edad, que no está guardada en la base sino calculada desde la fecha de nacimiento;</li>
 *   <li>expone solo los campos públicos, así un cambio en la entidad no cambia la API sin querer.</li>
 * </ul>
 *
 * <p>Es un record: los datos son de solo lectura, no tiene setters.</p>
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String apellido,
        String username,
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
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getFechaNacimiento(),
                edad,
                usuario.getSexo(),
                usuario.getActivo(),
                usuario.getCreatedAt()
        );
    }
}
