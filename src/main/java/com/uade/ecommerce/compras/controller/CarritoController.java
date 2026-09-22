package com.uade.ecommerce.compras.controller;

import com.uade.ecommerce.compras.dto.AgregarItemCarritoRequest;
import com.uade.ecommerce.compras.dto.CarritoResponse;
import com.uade.ecommerce.compras.dto.PedidoResponse;
import com.uade.ecommerce.compras.service.CarritoService;
import com.uade.ecommerce.compras.service.PedidoService;
import com.uade.ecommerce.identidad.security.UsuarioAutenticado;
import com.uade.ecommerce.shared.exception.ForbiddenException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Endpoints del carrito y del checkout. Todos piden estar autenticado.
 *
 * <p>Ítem 18: el {@code usuarioId} de la URL se sigue aceptando para no cambiarle las rutas al
 * front, pero ya no es quien dice ser el cliente: cada operación verifica que coincida con el
 * usuario del token y, si no, responde 403. Antes cualquiera podía ver, llenar o vaciar el
 * carrito de otro simplemente cambiando el número de la URL.</p>
 */
@RestController
@RequestMapping("/api/carritos")
public class CarritoController {

    private final CarritoService carritoService;
    private final PedidoService pedidoService;
    private final UsuarioAutenticado usuarioAutenticado;

    public CarritoController(
            CarritoService carritoService,
            PedidoService pedidoService,
            UsuarioAutenticado usuarioAutenticado
    ) {
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @PostMapping("/usuarios/{usuarioId}/items")
    public ResponseEntity<CarritoResponse> agregarProducto(
            @PathVariable Long usuarioId,
            @Valid
            @RequestBody
            AgregarItemCarritoRequest request
    ) {
        verificarCarritoPropio(usuarioId);

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
        verificarCarritoPropio(usuarioId);

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
        verificarCarritoPropio(usuarioId);

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
        verificarCarritoPropio(usuarioId);

        return ResponseEntity.ok(
                carritoService.vaciarCarrito(
                        usuarioId
                )
        );
    }

    /**
     * Checkout: crea un Pedido nuevo -> 201 Created + Location.
     *
     * <p>Acá la URL trae el id del carrito, no el del usuario, así que la verificación de dueño
     * no se puede hacer en el controller: se hace dentro del service, que es el que sabe de quién
     * es el carrito.</p>
     */
    @PostMapping("/{id}/checkout")
    public ResponseEntity<PedidoResponse> checkout(
            @PathVariable Long id,
            UriComponentsBuilder uriBuilder
    ) {

        PedidoResponse pedido = pedidoService.checkout(
                id,
                usuarioAutenticado.idRequerido()
        );

        URI location = uriBuilder
                .path("/api/pedidos/{id}")
                .buildAndExpand(pedido.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(pedido);
    }

    /** El carrito de la URL tiene que ser el del usuario del token; si no, 403. */
    private void verificarCarritoPropio(Long usuarioId) {
        if (!usuarioAutenticado.idRequerido().equals(usuarioId)) {
            throw new ForbiddenException("Solo podés operar sobre tu propio carrito");
        }
    }
}
