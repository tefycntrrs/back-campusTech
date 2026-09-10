package com.uade.ecommerce.repository;

import com.uade.ecommerce.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

//Repositorio de la clase Producto
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    //Catálogo público: solo productos activos, orden alfabético
    List<Producto> findByActivoTrueOrderByNombreAsc();

    //Obtener todos los productos de una categoria.
    //La relación es N:N, así que se recorre la tabla intermedia producto_categorias
    //con un JOIN sobre la colección p.categorias en vez de comparar una FK.
    @Query("SELECT DISTINCT p FROM Producto p JOIN p.categorias c WHERE c.id = :categoriaId")
    List<Producto> findByCategoriaId(@Param("categoriaId") Long categoriaId);

    //Productos activos de una categoria, para el catálogo por categoría
    @Query("SELECT DISTINCT p FROM Producto p JOIN p.categorias c "
            + "WHERE c.id = :categoriaId AND p.activo = true "
            + "ORDER BY p.nombre ASC")
    List<Producto> findActivosByCategoriaId(@Param("categoriaId") Long categoriaId);

    //Publicaciones creadas por un usuario vendedor
    @Query("SELECT p FROM Producto p WHERE p.vendedor.id = :vendedorId")
    List<Producto> findByVendedorId(@Param("vendedorId") Long vendedorId);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySkuIgnoreCase(String sku);
}
