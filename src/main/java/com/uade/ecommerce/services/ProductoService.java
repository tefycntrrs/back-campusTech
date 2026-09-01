package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.MarcaNotFoundException;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.MarcaRepository;
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

    public List<Producto> getProductosByCategoria(Long categoriaId) {

        if (!categoriaRepository.existsById(categoriaId)) {
            throw new CategoriaNotFoundException(categoriaId);
        }

        return productoRepository.findByCategoriaId(categoriaId);
    }

    public Producto createProducto(CreateProductoRequest request) {

        if (request.getCategoriaId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La categoría es obligatoria"
            );
        }

        Categoria categoria = categoriaRepository
                .findById(request.getCategoriaId())
                .orElseThrow(() ->
                        new CategoriaNotFoundException(request.getCategoriaId())
                );

        if (request.getMarcaId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La marca es obligatoria"
            );
        }

        Marca marca = marcaRepository
                .findById(request.getMarcaId())
                .orElseThrow(() ->
                        new MarcaNotFoundException(request.getMarcaId())
                );

        Producto producto = new Producto();

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setSku(request.getSku());

        producto.setCategoria(categoria);
        producto.setMarca(marca);

        return productoRepository.save(producto);
    }
}