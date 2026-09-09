package com.uade.ecommerce.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.uade.ecommerce.model.Sexo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de entrada del registro: es el JSON que recibe POST /api/usuarios/registro.
 *
 * No se recibe directamente la entidad Usuario para que el cliente no pueda mandar campos
 * que no le corresponden (id, activo, createdAt) ni una contraseña ya codificada.
 *
 * Las anotaciones de abajo son las validaciones de formato y las dispara el @Valid del
 * controller: si alguna falla, Spring corta antes de entrar al service y el GlobalExceptionHandler
 * devuelve un 400 con el detalle campo por campo. Las reglas de negocio (email único, edad mínima)
 * no están acá, están en UsuarioService.
 */
@Data
public class CreateUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    // Llega en texto plano y se guarda con passwordEncoder.encode(); nunca se persiste así
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;

    // @Past = tiene que ser anterior a hoy. El @JsonFormat fija el formato aceptado: 1999-05-20
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    // Tipado como enum y no como String: un valor inventado se rechaza al parsear el JSON
    @NotNull(message = "El sexo es obligatorio (MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR)")
    private Sexo sexo;
}
