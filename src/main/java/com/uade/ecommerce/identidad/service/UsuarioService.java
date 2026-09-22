package com.uade.ecommerce.identidad.service;

import com.uade.ecommerce.identidad.dto.CreateUsuarioRequest;
import com.uade.ecommerce.identidad.dto.LoginRequest;
import com.uade.ecommerce.identidad.dto.LoginResponse;
import com.uade.ecommerce.identidad.dto.UsuarioResponse;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import com.uade.ecommerce.shared.exception.CredencialesInvalidasException;
import com.uade.ecommerce.shared.exception.DuplicateResourceException;
import com.uade.ecommerce.shared.exception.UsuarioNotFoundException;
import com.uade.ecommerce.identidad.model.NombreRol;
import com.uade.ecommerce.identidad.model.Rol;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.RolRepository;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.identidad.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class UsuarioService {

    private static final int EDAD_MINIMA = 13;
    private static final int EDAD_MAXIMA = 120;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

        Rol rolPorDefecto = rolRepository.findByNombre(NombreRol.USER)
                .orElseGet(() -> rolRepository.save(new Rol(NombreRol.USER)));

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fechaNacimiento(request.getFechaNacimiento())
                .sexo(request.getSexo())
                .roles(new LinkedHashSet<>(Set.of(rolPorDefecto)))
                .build();

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    /**
     * Login por email y contraseña. Ya no se compara el hash a mano: la autenticación la hace el
     * AuthenticationManager de Spring Security (ítem 14), que busca al usuario con el
     * UserDetailsService, compara la contraseña con el PasswordEncoder y verifica que la cuenta
     * esté habilitada.
     *
     * <p>Todas las AuthenticationException se traducen a la misma CredencialesInvalidasException
     * (401): email inexistente, contraseña incorrecta y usuario inactivo devuelven exactamente la
     * misma respuesta, para no revelarle a quien prueba contraseñas cuál de las tres falló.</p>
     *
     * <p>Si la autenticación pasa, se emite el JWT que el cliente va a mandar de ahí en más.</p>
     */
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication autenticacion;

        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (AuthenticationException excepcion) {
            throw new CredencialesInvalidasException();
        }

        String token = jwtService.generarToken((UserDetails) autenticacion.getPrincipal());

        // Se relee la entidad para poder devolver el perfil completo (roles, edad, etc.)
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(CredencialesInvalidasException::new);

        return LoginResponse.from(token, jwtService.getDuracionMs(), usuario);
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