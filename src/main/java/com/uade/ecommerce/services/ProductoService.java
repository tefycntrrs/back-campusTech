package com.uade.ecommerce.services;

import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.MarcaNotFoundException;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.MarcaRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            MarcaRepository marcaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

    public List<Producto> getAllProductos() {
        return productoRepository.findAll();
    }

    public Producto createProducto(Producto producto) {

        if (producto.getCategoria() == null ||
                producto.getCategoria().getId() == null) {
            throw new RuntimeException("La categoría es obligatoria");
        }

        Long categoriaId = producto.getCategoria().getId();

        Categoria categoria = categoriaRepository
                .findById(categoriaId)
                .orElseThrow(() ->
                        new CategoriaNotFoundException(categoriaId));

        if (producto.getMarca() == null ||
                producto.getMarca().getId() == null) {
            throw new RuntimeException("La marca es obligatoria");
        }

        Long marcaId = producto.getMarca().getId();

        Marca marca = marcaRepository
                .findById(marcaId)
                .orElseThrow(() ->
                        new MarcaNotFoundException(marcaId));

        producto.setCategoria(categoria);
        producto.setMarca(marca);

        return productoRepository.save(producto);
    }
}