package com.uade.ecommerce.shared.exception;

/**
 * Tiene dos constructores porque al pedido se lo busca de dos formas: por id
 * (GET /api/pedidos/{id}) y por número (GET /api/pedidos/numero/{numero}). Alcanzan dos
 * constructores porque los tipos son distintos (Long y String); en UsuarioNotFoundException
 * hizo falta un método estático porque email y username son los dos String.
 */
public class PedidoNotFoundException extends ResourceNotFoundException {

    /** Búsqueda por id: GET /api/pedidos/{id}. */
    public PedidoNotFoundException(Long id) {
        super("Pedido", "El pedido con id " + id + " no existe");
    }

    /** Búsqueda por número: GET /api/pedidos/numero/{numero}. */
    public PedidoNotFoundException(String numero) {
        super("Pedido", "El pedido con número " + numero + " no existe");
    }
}