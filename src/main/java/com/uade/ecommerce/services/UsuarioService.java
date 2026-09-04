package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateUsuarioRequest;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.DuplicateResourceException;
import com.uade.ecommerce.exception.UsuarioNotFoundException;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@Transactional
public class UsuarioService {

    private static final int EDAD_MINIMA = 13;
    private static final int EDAD_MAXIMA = 120;

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario getUsuarioById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    public Usuario getUsuarioByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ArgumentInvalidException("email", "El email es obligatorio");
        }

        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsuarioNotFoundException(email));
    }

    /**
     * Registro de usuario. Las validaciones de formato las hace @Valid sobre el DTO;
     * acá van las reglas de negocio (edad y email único).
     */
    public Usuario registrarUsuario(CreateUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Usuario", "email", email);
        }

        validarFechaNacimiento(request.getFechaNacimiento());

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setSexo(request.getSexo());

        return usuarioRepository.save(usuario);
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();

        if (edad < EDAD_MINIMA) {
            throw new ArgumentInvalidException(
                    "fechaNacimiento",
                    "El usuario debe tener al menos " + EDAD_MINIMA + " años"
            );
        }

        if (edad > EDAD_MAXIMA) {
            throw new ArgumentInvalidException(
                    "fechaNacimiento",
                    "La fecha de nacimiento no es válida"
            );
        }
    }
}
