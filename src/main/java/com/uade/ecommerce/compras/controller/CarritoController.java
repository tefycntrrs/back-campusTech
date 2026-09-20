package com.uade.ecommerce.compras.controller;

import com.uade.ecommerce.compras.dto.AgregarItemCarritoRequest;
import com.uade.ecommerce.compras.dto.CarritoResponse;
import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.service.CarritoService;
import com.uade.ecommerce.compras.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;
    private final PedidoService pedidoService;

    public CarritoController(
            CarritoService carritoService,
            PedidoService pedidoService
    ) {
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
    }

    @PostMapping("/usuarios/{usuarioId}/items")
    public ResponseEntity<CarritoResponse> agregarProducto(
            @PathVariable Long usuarioId,
            @Valid
            @RequestBody
            AgregarItemCarritoRequest request
    ) {

        return ResponseEntity.ok(
                carritoService.agregarProducto(
                        usuarioId,
                        request
                )
        );
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<CarritoResponse> obtenerCarrito(
            @PathVariable Long usuarioId
    ) {

        return ResponseEntity.ok(
                carritoService.obtenerCarritoActivo(
                        usuarioId
                )
        );
    }

    @DeleteMapping(
            "/usuarios/{usuarioId}/items/{itemId}"
    )
    public ResponseEntity<CarritoResponse> eliminarItem(
            @PathVariable Long usuarioId,
            @PathVariable Long itemId
    ) {

        return ResponseEntity.ok(
                carritoService.eliminarItem(
                        usuarioId,
                        itemId
                )
        );
    }

    @DeleteMapping(
            "/usuarios/{usuarioId}/items"
    )
    public ResponseEntity<CarritoResponse> vaciarCarrito(
            @PathVariable Long usuarioId
    ) {

        return ResponseEntity.ok(
                carritoService.vaciarCarrito(
                        usuarioId
                )
        );
    }

    // Checkout crea un Pedido nuevo -> 201 Created + Location
    @PostMapping("/{id}/checkout")
    public ResponseEntity<PedidoResponse> checkout(
            @PathVariable Long id,
            UriComponentsBuilder uriBuilder
    ) {

        PedidoResponse pedido = pedidoService.checkout(id);

        URI location = uriBuilder
                .path("/api/pedidos/{id}")
                .buildAndExpand(pedido.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(pedido);
    }
}