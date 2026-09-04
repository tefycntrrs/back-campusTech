package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

//Repositorio de la clase Producto
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    //Obtener todos los productos de una categoria.
    //Se escribe la consulta a mano porque Producto expone getCategoriaId() y la
    //query derivada intentaba resolver un atributo 'categoriaId' que no existe en la entidad.
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId")
    List<Producto> findByCategoriaId(@Param("categoriaId") Long categoriaId);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySkuIgnoreCase(String sku);
}
