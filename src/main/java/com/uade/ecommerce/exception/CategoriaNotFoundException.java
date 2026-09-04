package com.uade.ecommerce.exception;

/**
 * La categoría pedida no existe. Hereda de {"@link ResourceNotFoundException"}, así que sale como
 * HTTP 404 sin necesidad de un handler propio: lo único que agrega es el mensaje en español.
 *
 * La lanzan CategoriaService.getCategoriaById() y ProductoService cuando el producto apunta
 * a un categoriaId que no está en la base.
 */
public class CategoriaNotFoundException extends ResourceNotFoundException {

    public CategoriaNotFoundException(Long id) {
        super("Categoria", "La categoría con id " + id + " no existe");
    }
}
