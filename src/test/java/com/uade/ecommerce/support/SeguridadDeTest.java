package com.uade.ecommerce.support;

import com.uade.ecommerce.identidad.security.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Utilidades para los tests que ahora tienen que autenticarse.
 *
 * <p>Ojo con el {@code springSecurity()}: sin eso, {@code webAppContextSetup(context).build()}
 * arma un MockMvc <b>sin la cadena de filtros de Spring Security</b>, así que la matriz de rutas
 * de SecurityConfig no se ejecutaría y los tests darían un falso verde. Con esta configuración
 * el request pasa por los mismos filtros que en producción, JWT incluido.</p>
 */
public final class SeguridadDeTest {

    private SeguridadDeTest() {
    }

    /** MockMvc con la cadena de filtros de seguridad real enchufada. */
    public static MockMvc mockMvcConSeguridad(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    /**
     * Arma el header Authorization de un usuario que ya existe en la base, emitiendo un JWT de
     * verdad con el mismo JwtService que usa el login. Así los tests ejercitan el token completo
     * (firma incluida) sin tener que pasar por el endpoint de login en cada caso.
     */
    public static String bearer(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            String email
    ) {
        return "Bearer " + jwtService.generarToken(userDetailsService.loadUserByUsername(email));
    }
}
