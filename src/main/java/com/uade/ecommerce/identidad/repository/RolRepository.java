package com.uade.ecommerce.identidad.repository;

import com.uade.ecommerce.identidad.model.NombreRol;
import com.uade.ecommerce.identidad.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(NombreRol nombre);

    // Roles de un usuario puntual, consultados directo desde el repositorio de Rol
    List<Rol> findByUsuarios_Id(Long usuarioId);
}
