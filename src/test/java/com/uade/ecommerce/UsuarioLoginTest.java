package com.uade.ecommerce;

import com.uade.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
class UsuarioLoginTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        usuarioRepository.deleteAll();

        String registro = """
                {
                  "nombre": "Pedro",
                  "apellido": "Marzano",
                  "username": "pedrom",
                  "email": "pedro@uade.edu.ar",
                  "password": "password123",
                  "fechaNacimiento": "1999-05-20",
                  "sexo": "MASCULINO"
                }
                """;

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
                  "password": "password123"
                }
                """;

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
                  "password": "password123"
                }
                """;

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
                  "password": "otraPassword"
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
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                // mismo mensaje a propósito: si dijera "el email no existe" se podrían
                // averiguar los emails registrados probando uno por uno
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
}
