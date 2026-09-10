package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio de la clase Usuario
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByUsernameIgnoreCase(String username);
}
