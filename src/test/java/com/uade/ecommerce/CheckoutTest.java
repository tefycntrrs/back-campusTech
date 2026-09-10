package com.uade.ecommerce;

import com.uade.ecommerce.model.Carrito;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.EstadoCarrito;
import com.uade.ecommerce.model.ItemCarrito;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.model.Sexo;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.repository.CarritoRepository;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.DetallePedidoRepository;
import com.uade.ecommerce.repository.ItemCarritoRepository;
import com.uade.ecommerce.repository.MarcaRepository;
import com.uade.ecommerce.repository.PedidoRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import com.uade.ecommerce.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarea 10 - Checkout: valida stock de todos los items, calcula el total,
 * crea el Pedido + DetallePedido, descuenta stock y cierra el carrito,
 * todo dentro de una unica transaccion.
 */
@SpringBootTest
class CheckoutTest {

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
    private PedidoRepository pedidoRepository;

    @Autowired
    private DetallePedidoRepository detallePedidoRepository;

    private MockMvc mockMvc;

    private Usuario usuario;
    private Producto productoA;
    private Producto productoB;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        limpiarBaseDeDatos();

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre("Test");
        nuevoUsuario.setApellido("QA");
        nuevoUsuario.setUsername("qa_checkout");
        nuevoUsuario.setEmail("qa.checkout@test.com");
        nuevoUsuario.setPassword("password123");
        nuevoUsuario.setFechaNacimiento(LocalDate.of(1995, 1, 1));
        nuevoUsuario.setSexo(Sexo.OTRO);
        usuario = usuarioRepository.save(nuevoUsuario);

        Categoria categoria = new Categoria();
        categoria.setNombre("CategoriaQA-Checkout");
        Categoria categoriaGuardada = categoriaRepository.save(categoria);

        Marca marca = new Marca();
        marca.setNombre("MarcaQA-Checkout");
        Marca marcaGuardada = marcaRepository.save(marca);

        productoA = crearProducto(
                "ProductoA", "QA-CHECKOUT-A", 10,
                BigDecimal.valueOf(100), categoriaGuardada, marcaGuardada
        );

        productoB = crearProducto(
                "ProductoB", "QA-CHECKOUT-B", 10,
                BigDecimal.valueOf(50), categoriaGuardada, marcaGuardada
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
        detallePedidoRepository.deleteAll();
        pedidoRepository.deleteAll();
        itemCarritoRepository.deleteAll();
        carritoRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        marcaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    private Producto crearProducto(
            String nombre,
            String sku,
            int stock,
            BigDecimal precio,
            Categoria categoria,
            Marca marca
    ) {

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion("test");
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setSku(sku);
        producto.setActivo(true);
        producto.getCategorias().add(categoria);
        producto.setMarca(marca);
        producto.setVendedor(usuario);

        return productoRepository.save(producto);
    }

    private Carrito crearCarritoConItems(int cantidadA, int cantidadB) {

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setEstado(EstadoCarrito.ACTIVO);
        carrito = carritoRepository.save(carrito);

        ItemCarrito itemA = new ItemCarrito();
        itemA.setCarrito(carrito);
        itemA.setProducto(productoA);
        itemA.setCantidad(cantidadA);
        itemA.setPrecioReferencia(productoA.getPrecio());
        carrito.getItems().add(itemA);

        ItemCarrito itemB = new ItemCarrito();
        itemB.setCarrito(carrito);
        itemB.setProducto(productoB);
        itemB.setCantidad(cantidadB);
        itemB.setPrecioReferencia(productoB.getPrecio());
        carrito.getItems().add(itemB);

        return carritoRepository.save(carrito);
    }

    @Test
    void confirmaElPedidoCalculaElTotalYDescuentaStock() throws Exception {
        Carrito carrito = crearCarritoConItems(2, 3);

        // total esperado: 2*100 + 3*50 = 350
        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").exists())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.subtotal").value(350.0))
                .andExpect(jsonPath("$.total").value(350.0))
                .andExpect(jsonPath("$.detalles.length()").value(2));

        Producto productoAActualizado = productoRepository.findById(productoA.getId()).orElseThrow();
        Producto productoBActualizado = productoRepository.findById(productoB.getId()).orElseThrow();

        assertThat(productoAActualizado.getStock()).isEqualTo(8);
        assertThat(productoBActualizado.getStock()).isEqualTo(7);

        Carrito carritoActualizado = carritoRepository.findById(carrito.getId()).orElseThrow();
        assertThat(carritoActualizado.getEstado()).isEqualTo(EstadoCarrito.FINALIZADO);

        assertThat(pedidoRepository.count()).isEqualTo(1);
        assertThat(detallePedidoRepository.count()).isEqualTo(2);
    }

    @Test
    void rechazaElCheckoutDeUnCarritoInexistente() throws Exception {
        mockMvc.perform(post("/api/carritos/{id}/checkout", 9999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaElCheckoutDeUnCarritoVacio() throws Exception {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setEstado(EstadoCarrito.ACTIVO);
        carrito = carritoRepository.save(carrito);

        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaElCheckoutDeUnCarritoYaFinalizado() throws Exception {
        Carrito carrito = crearCarritoConItems(1, 1);

        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isCreated());

        // segundo checkout sobre el mismo carrito, ya FINALIZADO
        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void siUnProductoNoTieneStockSuficienteNoDescuentaNingunoYNoCreaElPedido() throws Exception {
        Carrito carrito = crearCarritoConItems(2, 3);

        // entre agregar al carrito y el checkout, productoB se queda sin stock suficiente
        productoB.setStock(1);
        productoRepository.save(productoB);

        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isBadRequest());

        // productoA tenia stock de sobra, pero como productoB fallo, no se descuenta nada
        Producto productoAActualizado = productoRepository.findById(productoA.getId()).orElseThrow();
        Producto productoBActualizado = productoRepository.findById(productoB.getId()).orElseThrow();

        assertThat(productoAActualizado.getStock()).isEqualTo(10);
        assertThat(productoBActualizado.getStock()).isEqualTo(1);

        Carrito carritoActualizado = carritoRepository.findById(carrito.getId()).orElseThrow();
        assertThat(carritoActualizado.getEstado()).isEqualTo(EstadoCarrito.ACTIVO);

        assertThat(pedidoRepository.count()).isZero();
        assertThat(detallePedidoRepository.count()).isZero();
    }

    @Test
    void siUnProductoQuedoInactivoRechazaElCheckoutYNoDescuentaNada() throws Exception {
        Carrito carrito = crearCarritoConItems(2, 3);

        productoA.setActivo(false);
        productoRepository.save(productoA);

        mockMvc.perform(post("/api/carritos/{id}/checkout", carrito.getId()))
                .andExpect(status().isBadRequest());

        Producto productoBActualizado = productoRepository.findById(productoB.getId()).orElseThrow();
        assertThat(productoBActualizado.getStock()).isEqualTo(10);

        Carrito carritoActualizado = carritoRepository.findById(carrito.getId()).orElseThrow();
        assertThat(carritoActualizado.getEstado()).isEqualTo(EstadoCarrito.ACTIVO);

        assertThat(pedidoRepository.count()).isZero();
    }
}
