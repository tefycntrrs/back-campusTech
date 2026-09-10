package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CreateUsuarioRequest;
import com.uade.ecommerce.dto.LoginRequest;
import com.uade.ecommerce.dto.LoginResponse;
import com.uade.ecommerce.dto.UsuarioResponse;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.services.ProductoService;
import com.uade.ecommerce.services.UsuarioService;
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

    public UsuarioController(UsuarioService usuarioService, ProductoService productoService) {
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    // Registro de usuario -> 201 Created + header Location
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrarUsuario(
            @Valid @RequestBody CreateUsuarioRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Usuario usuario = usuarioService.registrarUsuario(request);

        URI location = uriBuilder
                .path("/api/usuarios/{id}")
                .buildAndExpand(usuario.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(UsuarioResponse.from(usuario));
    }

    // 200 con la lista, o 204 si todavía no hay usuarios registrados
    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> getAllUsuarios() {
        List<UsuarioResponse> usuarios = usuarioService.getAllUsuarios()
                .stream()
                .map(UsuarioResponse::from)
                .toList();

        if (usuarios.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getUsuarioById(@PathVariable Long id) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.getUsuarioById(id)));
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

        Usuario usuario = email != null
                ? usuarioService.getUsuarioByEmail(email)
                : usuarioService.getUsuarioByUsername(username);

        return ResponseEntity.ok(UsuarioResponse.from(usuario));
    }

    // Publicaciones creadas por el usuario -> el otro lado de Usuario 1:N Producto
    @GetMapping("/{id}/productos")
    public ResponseEntity<List<Producto>> getProductosByVendedor(@PathVariable Long id) {
        List<Producto> productos = productoService.getProductosByVendedor(id);

        if (productos.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(productos);
    }

    /**
     * Login por email y contraseña -> 200 con los datos del usuario, 401 si no coinciden.
     * Nunca devuelve la contraseña: la respuesta se arma con UsuarioResponse.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(LoginResponse.from(usuarioService.login(request)));
    }
}
