package com.uade.ecommerce.exception;

/**
 * El producto pedido no existe. Hereda de {"@link ResourceNotFoundException"}, así que sale como
 * HTTP 404 sin necesidad de un handler propio: lo único que agrega es el mensaje en español.
 *
 * La lanza ProductoService.getProductoById(), que es por donde pasan el GET por id y el PUT.
 */
public class ProductoNotFoundException extends ResourceNotFoundException {

    public ProductoNotFoundException(Long id) {
        super("Producto", "El producto con id " + id + " no existe");
    }
}
