package com.uade.ecommerce.controller;

import com.uade.ecommerce.dto.AgregarItemCarritoRequest;
import com.uade.ecommerce.dto.CarritoResponse;
import com.uade.ecommerce.model.Carrito;
import com.uade.ecommerce.services.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(
            CarritoService carritoService
    ) {
        this.carritoService = carritoService;
    }

    @PostMapping("/usuarios/{usuarioId}/items")
    public ResponseEntity<CarritoResponse> agregarProducto(
            @PathVariable Long usuarioId,
            @Valid
            @RequestBody
            AgregarItemCarritoRequest request
    ) {

        Carrito carrito =
                carritoService.agregarProducto(
                        usuarioId,
                        request
                );

        return ResponseEntity.ok(
                CarritoResponse.from(carrito)
        );
    }

    @GetMapping("/usuarios/{usuarioId}")
    public ResponseEntity<CarritoResponse> obtenerCarrito(
            @PathVariable Long usuarioId
    ) {

        Carrito carrito =
                carritoService.obtenerCarritoActivo(
                        usuarioId
                );

        return ResponseEntity.ok(
                CarritoResponse.from(carrito)
        );
    }

    @DeleteMapping(
            "/usuarios/{usuarioId}/items/{itemId}"
    )
    public ResponseEntity<CarritoResponse> eliminarItem(
            @PathVariable Long usuarioId,
            @PathVariable Long itemId
    ) {

        Carrito carrito =
                carritoService.eliminarItem(
                        usuarioId,
                        itemId
                );

        return ResponseEntity.ok(
                CarritoResponse.from(carrito)
        );
    }

    @DeleteMapping(
            "/usuarios/{usuarioId}/items"
    )
    public ResponseEntity<CarritoResponse> vaciarCarrito(
            @PathVariable Long usuarioId
    ) {

        Carrito carrito =
                carritoService.vaciarCarrito(
                        usuarioId
                );

        return ResponseEntity.ok(
                CarritoResponse.from(carrito)
        );
    }
}