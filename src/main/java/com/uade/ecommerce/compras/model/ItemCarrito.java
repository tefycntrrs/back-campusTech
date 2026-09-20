package com.uade.ecommerce.compras.model;

import com.uade.ecommerce.catalogo.model.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "items_carrito",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"carrito_id", "producto_id"}
                )
        }
)
public class ItemCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(
            name = "precio_referencia",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal precioReferencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
}