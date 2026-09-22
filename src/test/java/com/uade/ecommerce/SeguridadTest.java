package com.uade.ecommerce;

import com.uade.ecommerce.catalogo.repository.CategoriaRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Punto 3 de la consigna: registro con rol, contraseña hasheada y matriz de acceso.
 *
 * <p>Cubre los ítems 12 (rutas públicas vs protegidas), 13 (autorización por rol),
 * 18 (solo el dueño opera su carrito) y 20 (registro con rol, password hasheada y 403).</p>
 */
@SpringBootTest
@SuppressWarnings("java:S2068")
class SeguridadTest {

    private static final String TEST_PASS = "SecretPass!2026";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = SeguridadDeTest.mockMvcConSeguridad(context);
        limpiar();
    }

    @AfterEach
    void tearDown() {
        limpiar();
    }

    private void limpiar() {
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        rolRepository.deleteAll();
    }

    // Ítem 20: el registro asigna rol y guarda la contraseña hasheada

    @Test
    void elRegistroAsignaElRolUserPorDefecto() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro("nuevo", "nuevo@uade.edu.ar")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        Usuario guardado = usuarioRepository.findByEmailIgnoreCase("nuevo@uade.edu.ar").orElseThrow();

        assertThat(guardado.getRoles())
                .extracting(Rol::getNombre)
                .containsExactly(NombreRol.USER);
    }

    @Test
    void elRegistroNuncaGuardaLaContrasenaEnTextoPlano() throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro("hasheado", "hasheado@uade.edu.ar")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        String guardada = usuarioRepository
                .findByEmailIgnoreCase("hasheado@uade.edu.ar")
                .orElseThrow()
                .getPassword();

        assertThat(guardada).isNotEqualTo(TEST_PASS);
        // "$2a" / "$2b" es el prefijo que BCrypt le pone a todos sus hashes
        assertThat(guardada).startsWith("$2");
        assertThat(passwordEncoder.matches(TEST_PASS, guardada)).isTrue();
    }

    // Ítem 12: qué es público y qué pide token

    @Test
    void elCatalogoSeMiraSinEstarLogueado() throws Exception {
        // 204 porque no hay nada cargado; lo importante es que no responde 401
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/marcas"))
                .andExpect(status().isNoContent());
    }

    @Test
    void sinTokenLasRutasProtegidasDevuelven401ConElFormatoDeErrorGlobal() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/productos"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // Ítem 13 y 20: autorización por rol

    @Test
    void unUsuarioSinRolAdminNoPuedeCrearCategorias() throws Exception {
        Usuario user = registrar("comun", "comun@uade.edu.ar");

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCategoria()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(categoriaRepository.count()).isZero();
    }

    @Test
    void unAdminSiPuedeCrearCategorias() throws Exception {
        Usuario admin = registrar("jefe", "jefe@uade.edu.ar");
        darRolAdmin(admin);

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCategoria()))
                .andExpect(status().isCreated());

        assertThat(categoriaRepository.count()).isEqualTo(1);
    }

    // Ítem 18: el carrito y los pedidos son de su dueño

    @Test
    void unUsuarioNoPuedeMirarElCarritoDeOtro() throws Exception {
        Usuario propio = registrar("propio", "propio@uade.edu.ar");
        Usuario ajeno = registrar("ajeno", "ajeno@uade.edu.ar");

        mockMvc.perform(get("/api/carritos/usuarios/{id}", ajeno.getId())
                        .header("Authorization", token(propio)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unUsuarioNoPuedeVaciarElCarritoDeOtro() throws Exception {
        Usuario propio = registrar("propio2", "propio2@uade.edu.ar");
        Usuario ajeno = registrar("ajeno2", "ajeno2@uade.edu.ar");

        mockMvc.perform(delete("/api/carritos/usuarios/{id}/items", ajeno.getId())
                        .header("Authorization", token(propio)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unUsuarioNoPuedeVerElHistorialDePedidosDeOtro() throws Exception {
        Usuario propio = registrar("propio3", "propio3@uade.edu.ar");
        Usuario ajeno = registrar("ajeno3", "ajeno3@uade.edu.ar");

        mockMvc.perform(get("/api/usuarios/{id}/pedidos", ajeno.getId())
                        .header("Authorization", token(propio)))
                .andExpect(status().isForbidden());
    }

    private String bodyRegistro(String username, String email) {
        return """
                {
                  "nombre": "Test",
                  "apellido": "Seguridad",
                  "username": "%s",
                  "email": "%s",
                  "password": "%s",
                  "fechaNacimiento": "1999-05-20",
                  "sexo": "OTRO"
                }
                """.formatted(username, email, TEST_PASS);
    }

    private String bodyCategoria() {
        return """
                {
                  "nombre": "Perifericos"
                }
                """;
    }

    /** Registra por el endpoint real, así el usuario queda con su rol y su hash de verdad. */
    private Usuario registrar(String username, String email) throws Exception {
        mockMvc.perform(post("/api/usuarios/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyRegistro(username, email)))
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
