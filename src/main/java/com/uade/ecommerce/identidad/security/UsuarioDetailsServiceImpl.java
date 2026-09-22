package com.uade.ecommerce.identidad.security;

import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre el modelo propio (Usuario) y lo que Spring Security necesita para autenticar.
 *
 * <p>No se hace que Usuario implemente UserDetails directamente: se arma un
 * org.springframework.security.core.userdetails.User a partir del usuario, así el modelo de
 * dominio no queda atado al framework de seguridad.</p>
 *
 * <p>Ojo con el nombre del método: {@code loadUserByUsername} es de la interfaz de Spring y ahí
 * "username" significa "el identificador con el que uno se loguea", no el campo username de
 * nuestra entidad. En esta app se entra con <b>email</b>, así que es por email que se busca.
 * Antes esto estaba cruzado (el login pedía email pero acá se buscaba por username) y hacía que
 * la autenticación de Spring nunca encontrara al usuario.</p>
 */
@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    /** Spring Security espera los roles con el prefijo ROLE_ para que funcione hasRole(...). */
    private static final String PREFIJO_ROL = "ROLE_";

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con email " + email));

        // Builder de Spring Security (punto 3 de la consigna: "User" + "Builder")
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .authorities(usuario.getRoles().stream()
                        .map(rol -> new SimpleGrantedAuthority(PREFIJO_ROL + rol.getNombre().name()))
                        .toList())
                .disabled(Boolean.FALSE.equals(usuario.getActivo()))
                .build();
    }
}
