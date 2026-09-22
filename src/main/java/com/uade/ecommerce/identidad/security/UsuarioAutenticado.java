package com.uade.ecommerce.identidad.security;

import com.uade.ecommerce.identidad.model.NombreRol;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Responde "¿quién está haciendo este request?" leyendo el contexto de seguridad.
 *
 * <p>Es la pieza que reemplaza al viejo {@code ?usuarioId=...} (ítems 17 y 18): antes el cliente
 * decía quién era y cualquiera podía escribir el id de otro; ahora la identidad sale del token
 * que firmó el servidor y no se puede falsificar.</p>
 */
@Component
public class UsuarioAutenticado {

    private static final String PREFIJO_ROL = "ROLE_";

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticado(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El usuario dueño del request.
     *
     * @throws AuthenticationCredentialsNotFoundException si no hay nadie autenticado; el
     *         GlobalExceptionHandler la traduce a 401. En la práctica no debería pasar porque la
     *         matriz de SecurityConfig ya frena los requests anónimos antes de llegar acá.
     */
    public Usuario requerido() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        // El usuario anónimo también es "authenticated" para Spring, por eso se descarta aparte
        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || autenticacion instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario autenticado");
        }

        // El name del Authentication es el subject del token: el email
        return usuarioRepository.findByEmailIgnoreCase(autenticacion.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                        "El usuario autenticado ya no existe"));
    }

    /** Atajo para los services, que trabajan con el id del usuario. */
    public Long idRequerido() {
        return requerido().getId();
    }

    /** Un ADMIN puede operar sobre recursos ajenos; el resto solo sobre los propios. */
    public boolean esAdmin() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null) {
            return false;
        }

        return autenticacion.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch((PREFIJO_ROL + NombreRol.ADMIN.name())::equals);
    }
}
