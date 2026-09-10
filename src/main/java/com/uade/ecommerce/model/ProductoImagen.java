package com.uade.ecommerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "producto_imagenes")
public class ProductoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String url;

    /** Texto alternativo de la imagen (accesibilidad). Opcional. */
    @Column(length = 255)
    private String descripcion;

    /** Posición dentro de la galería: 0 es la primera. */
    @Column(nullable = false)
    private Integer orden = 0;

    /** Marca la foto de portada del producto. */
    @Column(nullable = false)
    private Boolean principal = false;

    // Se ignora en el JSON: la imagen ya viaja dentro del producto, incluirlo sería un ciclo
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Producto producto;

    @PrePersist
    public void prePersist() {
        if (orden == null) {
            orden = 0;
        }

        if (principal == null) {
            principal = false;
        }
    }
}
