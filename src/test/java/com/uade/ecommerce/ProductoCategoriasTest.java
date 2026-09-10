package com.uade.ecommerce;

import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.model.Sexo;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.MarcaRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import com.uade.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
class ProductoCategoriasTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private MockMvc mockMvc;

    private Long notebooksId;
    private Long gamingId;
    private Long marcaId;
    private Long vendedorId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        limpiar();

        notebooksId = categoriaRepository.save(categoria("Notebooks")).getId();
        gamingId = categoriaRepository.save(categoria("Gaming")).getId();
        marcaId = marcaRepository.save(marca("Asus")).getId();
        vendedorId = usuarioRepository.save(vendedor()).getId();
    }

    @AfterEach
    void tearDown() {
        limpiar();
    }

    @Test
    void guardaUnProductoConDosCategoriasYAparaceEnLasDos() throws Exception {
        String body = """
                {
                  "nombre": "ROG Zephyrus G14",
                  "descripcion": "Ryzen 9, 32 GB RAM, RTX 4070",
                  "precio": 2999999,
                  "stock": 4,
                  "sku": "ROG-G14-4070",
                  "categoriaIds": [%d, %d],
                  "marcaId": %d,
                  "vendedorId": %d,
                  "imagenes": [{"url": "https://cdn.ejemplo.com/rog-g14.jpg"}]
                }
                """.formatted(notebooksId, gamingId, marcaId, vendedorId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaIds.length()").value(2));

        // el mismo producto tiene que salir listado en las dos categorías
        mockMvc.perform(get("/api/categorias/" + notebooksId + "/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("ROG-G14-4070"));

        mockMvc.perform(get("/api/categorias/" + gamingId + "/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("ROG-G14-4070"));

        // y en la base la relación quedó en la tabla intermedia producto_categorias
        Producto guardado = productoRepository.findAll().get(0);
        assertThat(guardado.getCategoriaIds()).containsExactlyInAnyOrder(notebooksId, gamingId);
    }

    @Test
    void guardaElVendedorQueCreoLaPublicacion() throws Exception {
        String body = """
                {
                  "nombre": "TUF Gaming A15",
                  "precio": 1899999,
                  "stock": 6,
                  "sku": "TUF-A15",
                  "categoriaIds": [%d],
                  "marcaId": %d,
                  "vendedorId": %d,
                  "imagenes": [{"url": "https://cdn.ejemplo.com/tuf-a15.jpg"}]
                }
                """.formatted(notebooksId, marcaId, vendedorId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        Long productoId = productoRepository.findAll().get(0).getId();

        // consultando el producto se sabe quién lo publicó
        mockMvc.perform(get("/api/productos/" + productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendedorId").value(vendedorId))
                .andExpect(jsonPath("$.vendedorUsername").value("vendedor.test"));

        // y desde el usuario se llega a sus publicaciones (Usuario 1:N Producto)
        mockMvc.perform(get("/api/usuarios/" + vendedorId + "/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sku").value("TUF-A15"));

        List<Producto> delVendedor = productoRepository.findByVendedorId(vendedorId);
        assertThat(delVendedor).hasSize(1);
        assertThat(delVendedor.get(0).getVendedor().getId()).isEqualTo(vendedorId);
    }

    @Test
    void rechazaUnProductoSinVendedor() throws Exception {
        String body = """
                {
                  "nombre": "Sin vendedor",
                  "precio": 1000,
                  "stock": 1,
                  "sku": "SIN-VENDEDOR",
                  "categoriaIds": [%d],
                  "marcaId": %d
                }
                """.formatted(notebooksId, marcaId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.vendedorId").exists());

        assertThat(productoRepository.count()).isZero();
    }

    @Test
    void aceptaElCategoriaIdViejoDeUnaSolaCategoria() throws Exception {
        String body = """
                {
                  "nombre": "Compatibilidad",
                  "precio": 1000,
                  "stock": 1,
                  "sku": "COMPAT-1",
                  "categoriaId": %d,
                  "marcaId": %d,
                  "vendedorId": %d,
                  "imagenes": [{"url": "https://cdn.ejemplo.com/compat.jpg"}]
                }
                """.formatted(notebooksId, marcaId, vendedorId);

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaIds.length()").value(1))
                .andExpect(jsonPath("$.categoriaIds[0]").value(notebooksId));
    }

    private void limpiar() {
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Categoria categoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categoria;
    }

    private Marca marca(String nombre) {
        Marca marca = new Marca();
        marca.setNombre(nombre);
        return marca;
    }

    private Usuario vendedor() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Vendedor");
        usuario.setApellido("De Prueba");
        usuario.setUsername("vendedor.test");
        usuario.setEmail("vendedor@uade.edu.ar");
        usuario.setPassword("$2a$10$hashDePrueba");
        usuario.setFechaNacimiento(LocalDate.of(1995, 3, 10));
        usuario.setSexo(Sexo.OTRO);
        return usuario;
    }
}
