package com.uade.ecommerce;

import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.model.ProductoImagen;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión y catálogo de productos: listado alfabético solo con activos, detalle con
 * descripción e imágenes, y gestión de stock / baja restringida al usuario propietario.
 */
@SpringBootTest
class ProductoCatalogoTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private MarcaRepository marcaRepository;

    private MockMvc mockMvc;

    private Usuario duenio;
    private Usuario otro;
    private Categoria categoria;
    private Marca marca;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();

        duenio = usuarioRepository.save(nuevoUsuario("duenio@uade.edu.ar"));
        otro = usuarioRepository.save(nuevoUsuario("otro@uade.edu.ar"));
        categoria = categoriaRepository.save(nuevaCategoria());
        marca = marcaRepository.save(nuevaMarca());
    }

    @AfterEach
    void tearDown() {
        productoRepository.deleteAll();
        usuarioRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
    }

    @Test
    void elCatalogoDevuelveSoloActivosOrdenadosPorNombre() throws Exception {
        guardarProducto("Zapatilla", "ZAP-1", true, duenio);
        guardarProducto("Auricular", "AUR-1", true, duenio);
        guardarProducto("Mouse", "MOU-1", true, duenio);
        guardarProducto("Bicicleta dada de baja", "BIC-1", false, duenio);

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Auricular"))
                .andExpect(jsonPath("$[1].nombre").value("Mouse"))
                .andExpect(jsonPath("$[2].nombre").value("Zapatilla"));
    }

    @Test
    void elDetalleTraeDescripcionEImagenes() throws Exception {
        Producto producto = guardarProducto("Notebook", "NB-1", true, duenio);

        mockMvc.perform(get("/api/productos/" + producto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Descripcion de Notebook"))
                .andExpect(jsonPath("$.vendedorId").value(duenio.getId()))
                .andExpect(jsonPath("$.imagenes.length()").value(2))
                .andExpect(jsonPath("$.imagenes[0].principal").value(true));
    }

    @Test
    void elDetalleDeUnProductoDadoDeBajaDevuelve404() throws Exception {
        Producto producto = guardarProducto("Fantasma", "FAN-1", false, duenio);

        mockMvc.perform(get("/api/productos/" + producto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void elPropietarioPuedeModificarElStock() throws Exception {
        Producto producto = guardarProducto("Teclado", "TEC-1", true, duenio);

        mockMvc.perform(put("/api/productos/" + producto.getId())
                        .param("usuarioId", String.valueOf(duenio.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(42));

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(42);
    }

    @Test
    void otroUsuarioNoPuedeModificarElStock() throws Exception {
        Producto producto = guardarProducto("Monitor", "MON-1", true, duenio);

        mockMvc.perform(put("/api/productos/" + producto.getId())
                        .param("usuarioId", String.valueOf(otro.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStock()).isEqualTo(10);
    }

    @Test
    void elPutSinUsuarioIdEsRechazado() throws Exception {
        Producto producto = guardarProducto("Parlante", "PAR-1", true, duenio);

        mockMvc.perform(put("/api/productos/" + producto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\": 5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void elPropietarioPuedeEliminarSuPublicacionYSaleDelCatalogo() throws Exception {
        Producto producto = guardarProducto("Camara", "CAM-1", true, duenio);

        mockMvc.perform(delete("/api/productos/" + producto.getId())
                        .param("usuarioId", String.valueOf(duenio.getId())))
                .andExpect(status().isNoContent());

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getActivo()).isFalse();

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isNoContent());
    }

    @Test
    void otroUsuarioNoPuedeEliminarLaPublicacion() throws Exception {
        Producto producto = guardarProducto("Tablet", "TAB-1", true, duenio);

        mockMvc.perform(delete("/api/productos/" + producto.getId())
                        .param("usuarioId", String.valueOf(otro.getId())))
                .andExpect(status().isForbidden());

        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getActivo()).isTrue();
    }

    private Producto guardarProducto(String nombre, String sku, boolean activo, Usuario vendedor) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion("Descripcion de " + nombre);
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setStock(10);
        producto.setSku(sku);
        producto.setActivo(activo);
        producto.setCategoria(categoria);
        producto.setMarca(marca);
        producto.setVendedor(vendedor);

        ProductoImagen portada = new ProductoImagen();
        portada.setUrl("https://cdn.ejemplo.com/" + sku + "/1.jpg");
        portada.setOrden(0);
        portada.setPrincipal(true);
        producto.agregarImagen(portada);

        ProductoImagen secundaria = new ProductoImagen();
        secundaria.setUrl("https://cdn.ejemplo.com/" + sku + "/2.jpg");
        secundaria.setOrden(1);
        secundaria.setPrincipal(false);
        producto.agregarImagen(secundaria);

        return productoRepository.save(producto);
    }

    private Usuario nuevoUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Test");
        usuario.setApellido("User");
        usuario.setEmail(email);
        usuario.setPassword("$2a$10$abcdefghijklmnopqrstuv");
        usuario.setFechaNacimiento(LocalDate.of(1995, 1, 1));
        usuario.setSexo(Sexo.OTRO);
        return usuario;
    }

    private Categoria nuevaCategoria() {
        Categoria nueva = new Categoria();
        nueva.setNombre("Tecnologia");
        nueva.setActivo(true);
        return nueva;
    }

    private Marca nuevaMarca() {
        Marca nueva = new Marca();
        nueva.setNombre("MarcaTest");
        nueva.setActivo(true);
        return nueva;
    }
}
