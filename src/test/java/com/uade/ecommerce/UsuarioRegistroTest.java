package com.uade.ecommerce;

import com.uade.ecommerce.model.Sexo;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registro de usuario de punta a punta: request HTTP -> service -> base de datos.
 */
@SpringBootTest
class UsuarioRegistroTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        usuarioRepository.deleteAll();
    }

    @Test
    void registraUsuarioYLoGuardaEnLaBaseConFechaNacimientoYSexo() throws Exception {
        String body = """
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
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("pedro@uade.edu.ar"))
                .andExpect(jsonPath("$.username").value("pedrom"))
                .andExpect(jsonPath("$.fechaNacimiento").value("1999-05-20"))
                .andExpect(jsonPath("$.sexo").value("MASCULINO"))
                .andExpect(jsonPath("$.password").doesNotExist());

        Optional<Usuario> guardado = usuarioRepository.findByEmailIgnoreCase("pedro@uade.edu.ar");

        assertThat(guardado).isPresent();
        assertThat(guardado.get().getFechaNacimiento()).isEqualTo(LocalDate.of(1999, 5, 20));
        assertThat(guardado.get().getSexo()).isEqualTo(Sexo.MASCULINO);
        assertThat(guardado.get().getUsername()).isEqualTo("pedrom");
        assertThat(guardado.get().getActivo()).isTrue();
        // la contraseña se guarda codificada con encode(), nunca en texto plano
        assertThat(guardado.get().getPassword()).isNotEqualTo("password123").startsWith("$2");
    }

    @Test
    void rechazaElRegistroConCamposInvalidos() throws Exception {
        String body = """
                {
                  "nombre": "",
                  "apellido": "Marzano",
                  "username": "pm",
                  "email": "no-es-un-email",
                  "password": "123",
                  "fechaNacimiento": "2999-01-01",
                  "sexo": "MASCULINO"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/usuarios/registro"))
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists())
                .andExpect(jsonPath("$.errores.fechaNacimiento").exists())
                .andExpect(jsonPath("$.errores.username").exists());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void rechazaUnSexoInexistente() throws Exception {
        String body = """
                {
                  "nombre": "Ana",
                  "apellido": "Gomez",
                  "username": "anag",
                  "email": "ana@uade.edu.ar",
                  "password": "password123",
                  "fechaNacimiento": "2000-01-15",
                  "sexo": "INEXISTENTE"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR")));
    }

    @Test
    void rechazaUnEmailRepetido() throws Exception {
        String body = """
                {
                  "nombre": "Ana",
                  "apellido": "Gomez",
                  "username": "anag",
                  "email": "ana@uade.edu.ar",
                  "password": "password123",
                  "fechaNacimiento": "2000-01-15",
                  "sexo": "FEMENINO"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void rechazaUnUsernameRepetido() throws Exception {
        String primero = """
                {
                  "nombre": "Ana",
                  "apellido": "Gomez",
                  "username": "anag",
                  "email": "ana@uade.edu.ar",
                  "password": "password123",
                  "fechaNacimiento": "2000-01-15",
                  "sexo": "FEMENINO"
                }
                """;

        // mismo username, otro email: igual tiene que rebotar porque el username es unico
        String segundo = """
                {
                  "nombre": "Analia",
                  "apellido": "Perez",
                  "username": "ANAG",
                  "email": "analia@uade.edu.ar",
                  "password": "password123",
                  "fechaNacimiento": "2000-01-15",
                  "sexo": "FEMENINO"
                }
                """;

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primero))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void devuelve404ConElFormatoDeErrorGlobalCuandoElUsuarioNoExiste() throws Exception {
        mockMvc.perform(get("/api/usuarios/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("El usuario con id 9999 no existe"));
    }
}
