package com.uade.ecommerce.compras.controller;

import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.service.PedidoService;
import com.uade.ecommerce.identidad.security.UsuarioAutenticado;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de pedidos. Requiere estar autenticado y además ser el dueño del pedido: un pedido
 * tiene qué compró alguien y por cuánto, así que no alcanza con estar logueado (ítem 18).
 * Un ADMIN sí puede consultar cualquiera, para soporte.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioAutenticado usuarioAutenticado;

    public PedidoController(
            PedidoService pedidoService,
            UsuarioAutenticado usuarioAutenticado
    ) {
        this.pedidoService = pedidoService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    // Detalle de un pedido -> 200, 403 si es de otro, o 404 desde el GlobalExceptionHandler.
    // Es la URL que devuelve el header Location del checkout (POST /api/carritos/{id}/checkout)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.getPedidoById(
                id,
                usuarioAutenticado.idRequerido(),
                usuarioAutenticado.esAdmin()
        ));
    }

    // Búsqueda por número de pedido (por ejemplo, PED-1758300000000) -> 200, 403 o 404
    @GetMapping("/numero/{numero}")
    public ResponseEntity<PedidoResponse> getPedidoByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(pedidoService.getPedidoByNumero(
                numero,
                usuarioAutenticado.idRequerido(),
                usuarioAutenticado.esAdmin()
        ));
    }
}
