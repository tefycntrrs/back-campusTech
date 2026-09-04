package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.CreateUsuarioRequest;
import com.uade.ecommerce.dto.UsuarioResponse;
import com.uade.ecommerce.model.Usuario;
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

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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

    @GetMapping("/buscar")
    public ResponseEntity<UsuarioResponse> getUsuarioByEmail(@RequestParam String email) {
        return ResponseEntity.ok(UsuarioResponse.from(usuarioService.getUsuarioByEmail(email)));
    }
}
