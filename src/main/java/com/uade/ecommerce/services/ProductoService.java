package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Producto> getAllProductos() {
        return productoRepository.findAll();
    }

    public List<Producto> getProductosByCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe");
        }
        return productoRepository.findByCategoriaId(categoriaId);
    }

    public Producto createProducto(CreateProductoRequest request) {
        if (request.getCategoriaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría es obligatoria");
        }

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setCategoria(categoria);

        return productoRepository.save(producto);
    }
}
