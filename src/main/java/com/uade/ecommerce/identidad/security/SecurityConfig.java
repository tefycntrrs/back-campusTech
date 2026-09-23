package com.uade.ecommerce.identidad.security;

import com.uade.ecommerce.identidad.model.NombreRol;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Configuración central de Spring Security: acá vive "quién puede hacer qué".
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROL_ADMIN = NombreRol.ADMIN.name();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            // Es el mismo resolver que usa el DispatcherServlet: por él pasan los @ControllerAdvice
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    /**
     * Cadena de filtros de seguridad de la API.
     *
     * <ul>
     *   <li>CSRF desactivado: la API es sin estado y usa tokens (JWT) que viajan en un header, no
     *       cookies de sesión. Las cookies que el navegador manda solas son el vector del ataque
     *       CSRF (Clase 04); sin cookies, no hay nada que proteger.</li>
     *   <li>Sesión STATELESS: el servidor no guarda sesiones, cada request se identifica sola
     *       con su token.</li>
     *   <li>El filtro de JWT va <b>antes</b> del de usuario y contraseña, así el usuario ya está
     *       resuelto cuando se evalúa la matriz de acceso de abajo.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Ítem 19: los errores que Spring Security tira dentro de la cadena de filtros
                // nunca llegarían al @RestControllerAdvice, porque ocurren antes del controller.
                // Delegándolos al resolver del DispatcherServlet terminan en la misma clase
                // (GlobalExceptionHandler) y el cliente recibe siempre el mismo ErrorResponse.
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, excepcion) ->
                                handlerExceptionResolver.resolveException(request, response, null, excepcion))
                        .accessDeniedHandler((request, response, excepcion) ->
                                handlerExceptionResolver.resolveException(request, response, null, excepcion)))

                .authorizeHttpRequests(auth -> auth

                        // --- Público: entrar a la app ---
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll()

                        // --- Público: mirar el catálogo sin estar logueado ---
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categorias", "/api/categorias/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/marcas", "/api/marcas/**").permitAll()

                        // --- Solo ADMIN: el ABM del catálogo ---
                        // Va después de los GET de arriba: el primer matcher que coincide gana,
                        // así que los GET ya quedaron resueltos como públicos.
                        .requestMatchers("/api/categorias/**").hasRole(ROL_ADMIN)
                        .requestMatchers("/api/marcas/**").hasRole(ROL_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole(ROL_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/roles").hasRole(ROL_ADMIN)

                        // --- Todo lo demás pide estar logueado ---
                        // Publicar un producto, el carrito, el checkout y los pedidos entran acá.
                        .anyRequest().authenticated())

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

    /**
     * El que efectivamente autentica en el login (ítem 14).
     *
     * <p>El DaoAuthenticationProvider hace los tres pasos que antes estaban escritos a mano en
     * UsuarioService: busca el usuario con el UserDetailsService, compara la contraseña con el
     * PasswordEncoder y revisa que la cuenta esté habilitada. Si algo falla tira una
     * AuthenticationException y el usuario nunca queda autenticado.</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(userDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(proveedor);
    }
}
