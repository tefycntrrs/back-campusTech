package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

//Repositorio de la clase Producto
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    //Obtener todos los productos de una categoria
    List<Producto> findByCategoriaId(Long categoriaId);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
