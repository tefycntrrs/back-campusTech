package com.uade.ecommerce.compras.controller;

import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.service.PedidoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Detalle de un pedido -> 200, o 404 desde el GlobalExceptionHandler.
    // Es la URL que devuelve el header Location del checkout (POST /api/carritos/{id}/checkout)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.getPedidoById(id));
    }

    // Búsqueda por número de pedido (por ejemplo, PED-1758300000000) -> 200, o 404
    @GetMapping("/numero/{numero}")
    public ResponseEntity<PedidoResponse> getPedidoByNumero(@PathVariable String numero) {
        return ResponseEntity.ok(pedidoService.getPedidoByNumero(numero));
    }
}