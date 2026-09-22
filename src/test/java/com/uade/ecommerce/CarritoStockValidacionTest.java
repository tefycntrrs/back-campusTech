package com.uade.ecommerce;

import com.uade.ecommerce.catalogo.model.Categoria;
import com.uade.ecommerce.catalogo.model.Marca;
import com.uade.ecommerce.catalogo.model.Producto;
import com.uade.ecommerce.identidad.model.Sexo;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.compras.repository.CarritoRepository;
import com.uade.ecommerce.catalogo.repository.CategoriaRepository;
import com.uade.ecommerce.compras.repository.ItemCarritoRepository;
import com.uade.ecommerce.catalogo.repository.MarcaRepository;
import com.uade.ecommerce.catalogo.repository.ProductoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarea 09 - Reglas de stock del carrito: producto activo, stock > 0,
 * cantidad positiva y cantidad solicitada dentro del stock disponible.
 */
@SpringBootTest
class CarritoStockValidacionTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemCarritoRepository itemCarritoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    private MockMvc mockMvc;

    private Usuario usuario;
    private Long usuarioId;
    private Long productoConStockId;
    private Long productoSinStockId;
    private Long productoInactivoId;

    @BeforeEach
    void setUp() {
        mockMvc = SeguridadDeTest.mockMvcConSeguridad(context);

        limpiarBaseDeDatos();

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre("Test");
        nuevoUsuario.setApellido("QA");
        nuevoUsuario.setUsername("qa_carrito");
        nuevoUsuario.setEmail("qa.carrito@test.com");
        nuevoUsuario.setPassword("password123");
        nuevoUsuario.setFechaNacimiento(LocalDate.of(1995, 1, 1));
        nuevoUsuario.setSexo(Sexo.OTRO);
        usuario = usuarioRepository.save(nuevoUsuario);
        usuarioId = usuario.getId();

        Categoria categoria = new Categoria();
        categoria.setNombre("CategoriaQA");
        categoria.setDescripcion("test");
        Categoria categoriaGuardada = categoriaRepository.save(categoria);

        Marca marca = new Marca();
        marca.setNombre("MarcaQA");
        Marca marcaGuardada = marcaRepository.save(marca);

        productoConStockId = crearProducto(
                "ProductoQA-Normal", "QA-NORMAL-001", 5, true,
                categoriaGuardada, marcaGuardada
        );

        productoSinStockId = crearProducto(
                "ProductoQA-SinStock", "QA-SINSTOCK-001", 0, true,
                categoriaGuardada, marcaGuardada
        );

        productoInactivoId = crearProducto(
                "ProductoQA-Inactivo", "QA-INACTIVO-001", 5, false,
                categoriaGuardada, marcaGuardada
        );
    }

    @AfterEach
    void tearDown() {
        limpiarBaseDeDatos();
    }

    // Se corre antes y despues de cada test: evita que un carrito/usuario
    // de esta clase quede colgado y rompa el setUp (mas simple) de otra
    // clase de test que corra despues en la misma base H2.
    private void limpiarBaseDeDatos() {
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Long crearProducto(
            String nombre,
            String sku,
            int stock,
            boolean activo,
            Categoria categoria,
            Marca marca
    ) {

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion("test");
        producto.setPrecio(BigDecimal.valueOf(100));
        producto.setStock(stock);
        producto.setSku(sku);
        producto.setActivo(activo);
        producto.getCategorias().add(categoria);
        producto.setMarca(marca);
        producto.setVendedor(usuario);

        return productoRepository.save(producto).getId();
    }

    /** El carrito ahora exige estar autenticado y ser su dueno (item 18). */
    private String token() {
        return SeguridadDeTest.bearer(jwtService, userDetailsService, usuario.getEmail());
    }

    private String bodyAgregarItem(Long productoId, int cantidad) {
        return """
                {
                  "productoId": %d,
                  "cantidad": %d
                }
                """.formatted(productoId, cantidad);
    }

    @Test
    void agregaElItemCuandoElProductoEstaActivoYHayStockSuficiente() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoConStockId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productoId").value(productoConStockId))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));
    }

    @Test
    void rechazaElProductoInactivo() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoInactivoId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.productoId").exists());

        assertThat(itemCarritoRepository.count()).isZero();
    }

    @Test
    void rechazaElProductoSinStock() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoSinStockId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());

        assertThat(itemCarritoRepository.count()).isZero();
    }

    @Test
    void rechazaUnaCantidadNegativa() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoConStockId, -3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").exists());

        assertThat(itemCarritoRepository.count()).isZero();
    }

    @Test
    void rechazaUnaCantidadQueSuperaElStockDisponible() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoConStockId, 999)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").value(
                        org.hamcrest.Matchers.containsString("No hay stock suficiente")));

        assertThat(itemCarritoRepository.count()).isZero();
    }

    @Test
    void rechazaLaSumaAcumuladaQueSuperaElStockAlAgregarElMismoProductoDosVeces() throws Exception {
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoConStockId, 3)))
                .andExpect(status().isOk());

        // ya hay 3 en el carrito, quedan 2 de stock: pedir 3 mas debe rechazarse
        mockMvc.perform(post("/api/carritos/usuarios/{usuarioId}/items", usuarioId)
                        .header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyAgregarItem(productoConStockId, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.cantidad").value(
                        org.hamcrest.Matchers.containsString("No hay stock suficiente")));

        assertThat(itemCarritoRepository.count()).isEqualTo(1);
    }
}
