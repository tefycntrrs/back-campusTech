package com.uade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * Categorías del producto (N:N). Un producto puede estar en varias categorías y una
     * categoría tiene varios productos, así que la relación vive en la tabla intermedia
     * producto_categorias (producto_id, categoria_id) y no en una FK dentro de productos.
     *
     * <p>Es EAGER porque el JSON del producto siempre muestra sus categorías y la serialización
     * ocurre fuera de la transacción del service.</p>
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "producto_categorias",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Categoria> categorias = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    /**
     * Usuario que publicó el producto (Usuario 1:N Producto). Queda guardado en la columna
     * vendedor_id de la tabla productos. No se serializa entero para no arrastrar todo el
     * usuario dentro del producto: se exponen vendedorId y vendedorUsername.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vendedor_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario vendedor;

    @OneToMany(
            mappedBy = "producto",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("orden ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductoImagen> imagenes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Ids de las categorías, para que el JSON del producto sea fácil de leer desde el front. */
    public List<Long> getCategoriaIds() {
        return categorias == null
                ? List.of()
                : categorias.stream().map(Categoria::getId).toList();
    }

    public Long getVendedorId() {
        return vendedor != null ? vendedor.getId() : null;
    }

    public String getVendedorUsername() {
        return vendedor != null ? vendedor.getUsername() : null;
    }

    /** Alta de la imagen manteniendo los dos lados de la relación en sincronía. */
    public void agregarImagen(ProductoImagen imagen) {
        imagen.setProducto(this);
        this.imagenes.add(imagen);
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();

        this.createdAt = ahora;
        this.updatedAt = ahora;

        if (this.activo == null) {
            this.activo = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
