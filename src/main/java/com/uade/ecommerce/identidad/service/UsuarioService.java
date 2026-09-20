package com.uade.ecommerce.identidad.service;

import com.uade.ecommerce.identidad.dto.CreateUsuarioRequest;
import com.uade.ecommerce.identidad.dto.LoginRequest;
import com.uade.ecommerce.identidad.dto.LoginResponse;
import com.uade.ecommerce.identidad.dto.UsuarioResponse;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import com.uade.ecommerce.shared.exception.CredencialesInvalidasException;
import com.uade.ecommerce.shared.exception.DuplicateResourceException;
import com.uade.ecommerce.shared.exception.UsuarioNotFoundException;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> getAllUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    public UsuarioResponse getUsuarioById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        return UsuarioResponse.from(usuario);
    }

    public UsuarioResponse getUsuarioByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ArgumentInvalidException("username", "El username es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> UsuarioNotFoundException.porUsername(username));

        return UsuarioResponse.from(usuario);
    }

    public UsuarioResponse getUsuarioByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ArgumentInvalidException("email", "El email es obligatorio");
        }

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsuarioNotFoundException(email));

        return UsuarioResponse.from(usuario);
    }

    /**
     * Registro de usuario. Las validaciones de formato las hace @Valid sobre el DTO;
     * acá van las reglas de negocio (edad y email único).
     */
    public UsuarioResponse registrarUsuario(CreateUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Usuario", "email", email);
        }

        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("Usuario", "username", username);
        }

        validarFechaNacimiento(request.getFechaNacimiento());

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellido(request.getApellido().trim());
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setSexo(request.getSexo());

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Login por email y contraseña. La comparación de la contraseña se hace acá adentro con la
     * entidad: el hash nunca sale del service, hacia afuera solo viaja el LoginResponse.
     * Los tres motivos de fallo (email inexistente, contraseña incorrecta, usuario inactivo)
     * devuelven la misma excepción, para no revelar cuál fue.
     */
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException();
        }

        return LoginResponse.from(usuario);
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