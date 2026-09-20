package com.uade.ecommerce.compras.model;

import com.uade.ecommerce.identidad.model.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "carritos")
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCarrito estado = EstadoCarrito.ACTIVO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToMany(
            mappedBy = "carrito",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ItemCarrito> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {

        LocalDateTime ahora = LocalDateTime.now();

        createdAt = ahora;
        updatedAt = ahora;

        if (estado == null) {
            estado = EstadoCarrito.ACTIVO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}