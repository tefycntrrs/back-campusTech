package com.uade.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO de entrada del producto: lo usan POST /api/productos y PUT /api/productos/{id}.
 *
 * En vez de la entidad Producto recibe los ids de las relaciones (categoriaId, marcaId);
 * el service los busca en la base y arma el Producto con la Categoria y la Marca de verdad.
 *
 * No tiene anotaciones de validación a propósito, porque el mismo DTO sirve para crear y para
 * actualizar: el PUT es parcial (podés mandar solo precio y stock, y el resto queda como estaba),
 * así que un @NotNull acá rompería la actualización. Las validaciones están escritas a mano en
 * ProductoService, que sí sabe distinguir "crear" de "actualizar" y lanza ArgumentInvalidException
 * (400) o DuplicateResourceException (409) según el caso.
 */
@Data
public class CreateProductoRequest {

    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;

    // Código único del producto: no se puede repetir entre productos
    private String sku;

    // Opcional: si no viene, el producto se crea activo
    private Boolean activo;

    // Ids de las relaciones. Obligatorios al crear; opcionales al actualizar
    private Long categoriaId;
    private Long marcaId;
}
