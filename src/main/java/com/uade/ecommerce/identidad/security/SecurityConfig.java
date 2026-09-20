package com.uade.ecommerce.identidad.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración central de Spring Security: acá vive "quién puede hacer qué".
 *
 * <p>Esta es la versión inicial y es <b>permisiva a propósito</b>: al agregar
 * spring-boot-starter-security, Spring bloquea toda la API por defecto. Mientras se implementan
 * los roles y el JWT, esta configuración deja todo abierto para que nada se rompa. La matriz de
 * acceso real (rutas públicas, autenticadas y de ADMIN) se activa más adelante.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Cadena de filtros de seguridad de la API.
     *
     * <ul>
     *   <li>CSRF desactivado: la API es sin estado y va a usar tokens (JWT) que viajan en un
     *       header, no cookies de sesión. Las cookies que el navegador manda solas son el vector
     *       del ataque CSRF (Clase 04).</li>
     *   <li>Sesión STATELESS: el servidor no guarda sesiones; cada request se identifica sola.</li>
     *   <li>permitAll: temporal, hasta activar la matriz de acceso.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * Un único PasswordEncoder para toda la app. Antes había un "new BCryptPasswordEncoder()"
     * escondido dentro de UsuarioService; como bean se inyecta donde haga falta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}