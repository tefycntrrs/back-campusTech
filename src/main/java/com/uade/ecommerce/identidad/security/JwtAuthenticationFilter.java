package com.uade.ecommerce.identidad.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que traduce el header {@code Authorization: Bearer <token>} en un usuario autenticado.
 *
 * <p>Corre una sola vez por request (de ahí {@link OncePerRequestFilter}) y antes del filtro de
 * autorización de Spring Security, así que para cuando se evalúa la matriz de rutas de
 * {@link SecurityConfig} ya se sabe quién está pidiendo.</p>
 *
 * <p>Si no hay token, o el token es inválido o venció, el filtro <b>no corta la cadena ni tira
 * una excepción</b>: simplemente deja el request sin autenticar y sigue. Quien decide si eso es
 * un problema es la matriz de rutas: si la ruta era pública pasa igual, y si era protegida el
 * {@code AuthenticationEntryPoint} devuelve 401.</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTORIZACION = "Authorization";
    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = tokenDelHeader(request);

        // Si ya hay una autenticación en el contexto no se pisa
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.subjectDeTokenValido(token)
                    .ifPresent(email -> autenticar(email, request));
        }

        filterChain.doFilter(request, response);
    }

    private String tokenDelHeader(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTORIZACION);

        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }

        return header.substring(PREFIJO_BEARER.length()).trim();
    }

    /**
     * Los roles se releen de la base en cada request en vez de sacarlos del token: así un cambio
     * de rol o una baja de usuario tienen efecto inmediato y no recién cuando el token expira.
     */
    private void autenticar(String email, HttpServletRequest request) {
        UserDetails usuario;

        try {
            usuario = userDetailsService.loadUserByUsername(email);
        } catch (UsernameNotFoundException excepcion) {
            // El token es válido pero el usuario ya no existe: queda sin autenticar
            return;
        }

        if (!usuario.isEnabled()) {
            return;
        }

        UsernamePasswordAuthenticationToken autenticacion =
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario,
                        null, // la contraseña no se guarda en el contexto
                        usuario.getAuthorities()
                );

        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }
}
