package com.uade.ecommerce;

import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.support.SeguridadDeTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SuppressWarnings("java:S2068")
class UsuarioLoginTest {

    private static final String TEST_PASS = "SecretPass!2026";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = SeguridadDeTest.mockMvcConSeguridad(context);
        usuarioRepository.deleteAll();

        String registro = """
                {
                  "nombre": "Pedro",
                  "apellido": "Marzano",
                  "username": "pedrom",
                  "email": "pedro@uade.edu.ar",
                  "password": "%s",
                  "fechaNacimiento": "1999-05-20",
                  "sexo": "MASCULINO"
                }
                """.formatted(TEST_PASS);

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro))
                .andExpect(status().isCreated());
    }

    @Test
    void loginCorrectoDevuelveLosDatosDelUsuarioSinLaContrasena() throws Exception {
        String body = """
                {
                  "email": "pedro@uade.edu.ar",
                  "password": "%s"
                }
                """.formatted(TEST_PASS);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Login exitoso"))
                .andExpect(jsonPath("$.usuario.id").exists())
                .andExpect(jsonPath("$.usuario.username").value("pedrom"))
                .andExpect(jsonPath("$.usuario.email").value("pedro@uade.edu.ar"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginAceptaElEmailEnMayusculas() throws Exception {
        String body = """
                {
                  "email": "PEDRO@UADE.EDU.AR",
                  "password": "%s"
                }
                """.formatted(TEST_PASS);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value("pedro@uade.edu.ar"));
    }

    @Test
    void contrasenaIncorrectaDevuelve401() throws Exception {
        String body = """
                {
                  "email": "pedro@uade.edu.ar",
                  "password": "WrongSecretPass!99"
                }
                """;

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"));
    }

    @Test
    void emailInexistenteDevuelveElMismo401QueLaContrasenaIncorrecta() throws Exception {
        String body = """
                {
                  "email": "noexiste@uade.edu.ar",
                  "password": "%s"
                }
                """.formatted(TEST_PASS);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"));
    }

    @Test
    void rechazaUnLoginSinCampos() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    /**
     * Ítem 16: el login devuelve un JWT y ese token sirve de verdad para entrar a una ruta
     * protegida. Es el test que ata el login con el resto de la API.
     */
    @Test
    void elLoginDevuelveUnTokenQueSirveParaEntrarAUnaRutaProtegida() throws Exception {
        String body = """
                {
                  "email": "pedro@uade.edu.ar",
                  "password": "%s"
                }
                """.formatted(TEST_PASS);

        String respuesta = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnMs").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = respuesta.split("\"token\":\"")[1].split("\"")[0];

        // un JWT son tres partes separadas por punto
        assertThat(token.split("[.]")).hasSize(3);

        Long id = usuarioRepository.findByEmailIgnoreCase("pedro@uade.edu.ar").orElseThrow().getId();

        // sin el token esta ruta da 401; con el token, responde
        mockMvc.perform(get("/api/usuarios/{id}/pedidos", id))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/usuarios/{id}/pedidos", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    /**
     * Ítem 21: un usuario dado de baja no entra, y recibe el mismo 401 que una contraseña
     * incorrecta. La verificación la hace el DaoAuthenticationProvider de Spring Security a
     * partir del isEnabled() del UserDetails.
     */
    @Test
    void unUsuarioInactivoNoPuedeIniciarSesion() throws Exception {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase("pedro@uade.edu.ar").orElseThrow();
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        String body = """
                {
                  "email": "pedro@uade.edu.ar",
                  "password": "%s"
                }
                """.formatted(TEST_PASS);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
