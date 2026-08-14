package com.uade.ecommerce.services;

import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> getAllProductos() {
        return productoRepository.findAll();
    }

    public Producto createProducto(Producto producto) {
        return productoRepository.save(producto);
    }
}
