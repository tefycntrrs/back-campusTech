package com.uade.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada del producto: lo usan POST /api/productos y PUT /api/productos/{id}.
 *
 * <p>En vez de la entidad Producto recibe los ids de las relaciones (categoriaIds, marcaId,
 * vendedorId); el service los busca en la base y arma el Producto con las Categorias, la Marca
 * y el Usuario vendedor de verdad.</p>
 *
 * <p>No tiene anotaciones de validación a propósito, porque el mismo DTO sirve para crear y para
 * actualizar: el PUT es parcial (podés mandar solo precio y stock, y el resto queda como estaba),
 * así que un @NotNull acá rompería la actualización. Las validaciones están escritas a mano en
 * ProductoService, que sí sabe distinguir "crear" de "actualizar" y lanza ArgumentInvalidException
 * (400) o DuplicateResourceException (409) según el caso.</p>
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

    /**
     * Categorías del producto: ahora la relación es N:N, así que se manda una lista.
     * Obligatoria al crear (al menos una); en el PUT es opcional y, si viene, reemplaza
     * todas las categorías del producto.
     */
    private List<Long> categoriaIds;

    /**
     * Compatibilidad con la versión anterior, cuando el producto tenía una sola categoría.
     * Si llega categoriaId y no llega categoriaIds, se usa como lista de un solo elemento.
     */
    private Long categoriaId;

    // Ids de las relaciones. Obligatorios al crear; opcionales al actualizar
    private Long marcaId;

    /** Usuario que publica el producto. Obligatorio al crear: queda guardado como vendedor. */
    private Long vendedorId;

    /** Galería del producto (opcional en el PUT, obligatoria al crear). */
    private List<ImagenProductoRequest> imagenes;

    /**
     * Devuelve las categorías pedidas unificando categoriaIds y el viejo categoriaId,
     * sin repetidos y respetando el orden en que llegaron.
     */
    public List<Long> categoriasSolicitadas() {
        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            return categoriaIds.stream().distinct().toList();
        }

        if (categoriaId != null) {
            return List.of(categoriaId);
        }

        // null (y no lista vacía) significa "no mandaron categorías": el PUT las deja como están
        return categoriaIds;
    }
}
