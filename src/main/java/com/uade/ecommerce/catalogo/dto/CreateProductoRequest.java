package com.uade.ecommerce.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada del producto: lo usan POST /api/productos y PUT /api/productos/{id}.
 *
 * <p>En vez de la entidad Producto recibe los ids de las relaciones (categoriaIds, marcaId);
 * el service los busca en la base y arma el Producto con las Categorias y la Marca de verdad.</p>
 *
 * <p>El vendedor NO se manda en el body: sale del usuario autenticado. Antes existía un campo
 * vendedorId y cualquiera podía publicar un producto a nombre de otro simplemente escribiendo
 * otro id (ítem 17).</p>
 *
 * <p>Las anotaciones de validación de este DTO toleran null a propósito: @Size, @Positive,
 * @PositiveOrZero, @Digits y @Pattern consideran válido un campo ausente. Es necesario porque el
 * mismo DTO sirve para crear y para actualizar, y el PUT es parcial (podés mandar solo precio y
 * stock, y el resto queda como estaba): un @NotNull o un @NotBlank acá rompería la actualización.
 * Entonces el DTO valida el FORMATO de lo que llega, y lo que es obligatorio al crear (nombre,
 * sku, precio, stock, categorías, marca, al menos una imagen) lo sigue exigiendo
 * ProductoService, que sí distingue "crear" de "actualizar".</p>
 *
 * <p>Las anotaciones las dispara el @Valid del controller: si alguna falla, Spring corta antes
 * de entrar al service y el GlobalExceptionHandler devuelve un 400 con el detalle campo por
 * campo. Las reglas que necesitan la base (SKU repetido, ids inexistentes) están en el service.</p>
 */
@Data
public class CreateProductoRequest {

    // El @Pattern rechaza un texto vacío o de solo espacios, pero deja pasar null (PUT parcial)
    @Pattern(regexp = "(?s).*\\S.*", message = "El nombre no puede estar vacío")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String descripcion;

    // La columna es DECIMAL(10,2): hasta 8 dígitos enteros y 2 decimales
    @Positive(message = "El precio debe ser mayor a 0")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal precio;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;

    // Código único del producto: no se puede repetir entre productos
    @Pattern(regexp = "(?s).*\\S.*", message = "El SKU no puede estar vacío")
    @Size(max = 80, message = "El SKU no puede superar los 80 caracteres")
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

    /**
     * Galería del producto (opcional en el PUT, obligatoria al crear). El @Valid hace que la
     * validación baje a cada ImagenProductoRequest de la lista; si la lista es null no pasa nada.
     */
    @Valid
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