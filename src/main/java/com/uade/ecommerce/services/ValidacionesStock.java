package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.model.Producto;

/**
 * Reglas de stock compartidas entre agregar un item al carrito y el checkout:
 * el carrito las corre al agregar, y el checkout las vuelve a correr porque
 * el stock (o que el producto siga activo) pudo cambiar mientras estaba en el carrito.
 */
final class ValidacionesStock {

    private ValidacionesStock() {
    }

    static void validarCantidadPositiva(int cantidad) {

        if (cantidad <= 0) {

            throw new ArgumentInvalidException(
                    "cantidad",
                    "La cantidad solicitada debe ser mayor a cero"
            );
        }
    }

    static void validarProductoActivo(Producto producto) {

        if (!Boolean.TRUE.equals(producto.getActivo())) {

            throw new ArgumentInvalidException(
                    "productoId",
                    "Producto no disponible"
            );
        }
    }

    static void validarStockSuficiente(Producto producto, int cantidad) {

        if (producto.getStock() == null
                || producto.getStock() <= 0) {

            throw new ArgumentInvalidException(
                    "cantidad",
                    "El producto no tiene stock disponible"
            );
        }

        if (cantidad > producto.getStock()) {

            throw new ArgumentInvalidException(
                    "cantidad",
                    "No hay stock suficiente. Stock disponible: "
                            + producto.getStock()
            );
        }
    }
}
