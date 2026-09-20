package com.uade.ecommerce.identidad.repository;

import com.uade.ecommerce.identidad.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// Repositorio de la clase Usuario
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByUsernameIgnoreCase(String username);
}
