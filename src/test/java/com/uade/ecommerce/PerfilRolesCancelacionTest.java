package com.uade.ecommerce;

import com.uade.ecommerce.identidad.model.NombreRol;
import com.uade.ecommerce.identidad.model.Rol;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.RolRepository;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.identidad.security.JwtService;
import com.uade.ecommerce.support.SeguridadDeTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4.2 asignar roles, 4.3 /me y 4.4 cancelar pedido (el 403 de cancelar).
 */
@SpringBootTest
@SuppressWarnings("java:S2068")
class PerfilRolesCancelacionTest {

    private static final String TEST_PASS = "SecretPass!2026";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = SeguridadDeTest.mockMvcConSeguridad(context);
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
    }

    @Test
    void elRegistroSinRolQuedaComoUser() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro("comun", "comun@uade.edu.ar", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void elRegistroPuedePedirRolVendedor() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro("vende", "vende@uade.edu.ar", "VENDEDOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("VENDEDOR"));
    }

    @Test
    void elRegistroRechazaAutoasignarseAdmin() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro("hacker", "hacker@uade.edu.ar", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.rol").exists());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void unAdminPuedeAsignarRoles() throws Exception {
        Usuario user = registrar("comun", "comun@uade.edu.ar", null);
        Usuario admin = registrar("jefe", "jefe@uade.edu.ar", null);
        darRolAdmin(admin);

        mockMvc.perform(put("/api/usuarios/{id}/roles", user.getId())
                        .header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"VENDEDOR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("VENDEDOR"));
    }

    @Test
    void unUsuarioComunNoPuedeAsignarRoles() throws Exception {
        Usuario user = registrar("comun", "comun@uade.edu.ar", null);
        Usuario otro = registrar("otro", "otro@uade.edu.ar", null);

        mockMvc.perform(put("/api/usuarios/{id}/roles", otro.getId())
                        .header("Authorization", token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\": [\"ADMIN\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meDevuelveAlUsuarioDelToken() throws Exception {
        Usuario user = registrar("yo", "yo@uade.edu.ar", null);

        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", token(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("yo@uade.edu.ar"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void meSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meProductosYMePedidosVaciosDan204() throws Exception {
        Usuario user = registrar("vacio", "vacio@uade.edu.ar", null);

        mockMvc.perform(get("/api/usuarios/me/productos")
                        .header("Authorization", token(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios/me/pedidos")
                        .header("Authorization", token(user)))
                .andExpect(status().isNoContent());
    }

    private String bodyRegistro(String username, String email, String rol) {
        String rolJson = rol == null ? "" : ",\n                  \"rol\": \"" + rol + "\"";
        return """
                {
                  "nombre": "Test",
                  "apellido": "Perfil",
                  "username": "%s",
                  "email": "%s",
                  "password": "%s",
                  "fechaNacimiento": "1999-05-20",
                  "sexo": "OTRO"%s
                }
                """.formatted(username, email, TEST_PASS, rolJson);
    }

    private Usuario registrar(String username, String email, String rol) throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro(username, email, rol)))
                .andExpect(status().isCreated());

        return usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private void darRolAdmin(Usuario usuario) {
        Rol admin = rolRepository.findByNombre(NombreRol.ADMIN)
                .orElseGet(() -> rolRepository.save(new Rol(NombreRol.ADMIN)));

        usuario.getRoles().add(admin);
        usuarioRepository.save(usuario);
    }

    private String token(Usuario usuario) {
        return SeguridadDeTest.bearer(jwtService, userDetailsService, usuario.getEmail());
    }
}
