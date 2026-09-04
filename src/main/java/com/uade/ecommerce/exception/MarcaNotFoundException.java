package com.uade.ecommerce.exception;

/**
 * La marca pedida no existe. Hereda de {¨¨@link ResourceNotFoundException}, así que sale como
 * HTTP 404 sin necesidad de un handler propio: lo único que agrega es el mensaje en español.
 *
 * La lanzan MarcaService.getMarcaById() y ProductoService cuando el producto apunta a un
 * marcaId que no está en la base.
 */
public class MarcaNotFoundException extends ResourceNotFoundException {

    public MarcaNotFoundException(Long id) {
        super("Marca", "La marca con id " + id + " no existe");
    }
}
