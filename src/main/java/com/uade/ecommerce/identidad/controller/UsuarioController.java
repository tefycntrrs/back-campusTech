package com.uade.ecommerce.identidad.controller;

import com.uade.ecommerce.catalogo.dto.ProductoResponse;
import com.uade.ecommerce.catalogo.service.ProductoService;
import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.service.PedidoService;
import com.uade.ecommerce.identidad.dto.CreateUsuarioRequest;
import com.uade.ecommerce.identidad.dto.LoginRequest;
import com.uade.ecommerce.identidad.dto.LoginResponse;
import com.uade.ecommerce.identidad.dto.UsuarioResponse;
import com.uade.ecommerce.identidad.service.UsuarioService;
import com.uade.ecommerce.shared.exception.ArgumentInvalidException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ProductoService productoService;
    private final PedidoService pedidoService;

    public UsuarioController(
            UsuarioService usuarioService,
            ProductoService productoService,
            PedidoService pedidoService
    ) {
        this.usuarioService = usuarioService;
        this.productoService = productoService;
        this.pedidoService = pedidoService;
    }

    // Registro de usuario -> 201 Created + header Location
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrarUsuario(
            @Valid @RequestBody CreateUsuarioRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        UsuarioResponse usuario = usuarioService.registrarUsuario(request);

        // UsuarioResponse es un record: el id se lee con id(), no con getId()
        URI location = uriBuilder
                .path("/api/usuarios/{id}")
                .buildAndExpand(usuario.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(usuario);
    }

    // 200 con la lista, o 204 si todavía no hay usuarios registrados
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAllUsuarios() {
        List<UsuarioResponse> usuarios = usuarioService.getAllUsuarios();

        if (usuarios.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.getUsuarioById(id));
    }

    // Se puede buscar por email o por username; hay que mandar uno de los dos
    @GetMapping("/buscar")
    public ResponseEntity<UsuarioResponse> buscarUsuario(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String username
    ) {
        if (email == null && username == null) {
            throw new ArgumentInvalidException("email", "Indicá email o username para buscar");
        }

        UsuarioResponse usuario = email != null
                ? usuarioService.getUsuarioByEmail(email)
                : usuarioService.getUsuarioByUsername(username);

        return ResponseEntity.ok(usuario);
    }

    // Publicaciones creadas por el usuario -> el otro lado de Usuario 1:N Producto
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProductoResponse>> getProductosByVendedor(@PathVariable Long id) {
        List<ProductoResponse> productos = productoService.getProductosByVendedor(id);

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }

    // Historial de compras del usuario, del más nuevo al más viejo
    // -> 200 con la lista, 204 si todavía no compró nada, 404 si el usuario no existe
    @GetMapping("/{id}/pedidos")
    public ResponseEntity<List<PedidoResponse>> getPedidosByUsuario(@PathVariable Long id) {
        List<PedidoResponse> pedidos = pedidoService.getPedidosByUsuario(id);

        if (pedidos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(pedidos);
    }

    /**
     * Login por email y contraseña -> 200 con los datos del usuario, 401 si no coinciden.
     * Nunca devuelve la contraseña: el service arma la respuesta con UsuarioResponse.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }
}