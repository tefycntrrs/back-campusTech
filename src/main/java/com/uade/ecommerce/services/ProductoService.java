package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.DuplicateResourceException;
import com.uade.ecommerce.exception.MarcaNotFoundException;
import com.uade.ecommerce.exception.ProductoNotFoundException;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.MarcaRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public Producto getProductoById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    public List<Producto> getProductosByCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new CategoriaNotFoundException(categoriaId);
        }

        return productoRepository.findByCategoriaId(categoriaId);
    }

    public Producto createProducto(CreateProductoRequest request) {
        Categoria categoria = resolveCategoria(request.getCategoriaId());
        Marca marca = resolveMarca(request.getMarcaId());

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new ArgumentInvalidException("nombre", "El nombre del producto es obligatorio");
        }

        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new ArgumentInvalidException("sku", "El SKU es obligatorio");
        }

        String sku = request.getSku().trim();

        if (productoRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Producto", "Ya existe un producto con ese SKU");
        }

        validarPrecio(request.getPrecio());
        validarStock(request.getStock());

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setSku(sku);
        producto.setCategoria(categoria);
        producto.setMarca(marca);

        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        return productoRepository.save(producto);
    }

    public Producto updateProducto(Long id, CreateProductoRequest request) {
        Producto producto = getProductoById(id);

        String sku = null;
        if (request.getSku() != null) {
            if (request.getSku().isBlank()) {
                throw new ArgumentInvalidException("sku", "El SKU no puede estar vacío");
            }

            sku = request.getSku().trim();
            if (productoRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
                throw new DuplicateResourceException("Producto", "Ya existe un producto con ese SKU");
            }
        }

        Categoria categoria = null;
        if (request.getCategoriaId() != null) {
            categoria = resolveCategoria(request.getCategoriaId());
        }

        Marca marca = null;
        if (request.getMarcaId() != null) {
            marca = resolveMarca(request.getMarcaId());
        }

        if (request.getNombre() != null) {
            producto.setNombre(request.getNombre());
        }

        if (request.getDescripcion() != null) {
            producto.setDescripcion(request.getDescripcion());
        }

        if (request.getPrecio() != null) {
            validarPrecio(request.getPrecio());
            producto.setPrecio(request.getPrecio());
        }

        if (request.getStock() != null) {
            validarStock(request.getStock());
            producto.setStock(request.getStock());
        }

        if (sku != null) {
            producto.setSku(sku);
        }

        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        if (categoria != null) {
            producto.setCategoria(categoria);
        }

        if (marca != null) {
            producto.setMarca(marca);
        }

        return productoRepository.save(producto);
    }

    private void validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new ArgumentInvalidException("precio", "El precio es obligatorio");
        }

        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ArgumentInvalidException("precio", "El precio debe ser mayor a 0");
        }
    }

    private void validarStock(Integer stock) {
        if (stock == null) {
            throw new ArgumentInvalidException("stock", "El stock es obligatorio");
        }

        if (stock < 0) {
            throw new ArgumentInvalidException("stock", "El stock no puede ser negativo");
        }
    }

    private Categoria resolveCategoria(Long categoriaId) {
        if (categoriaId == null) {
            throw new ArgumentInvalidException("categoriaId", "La categoría es obligatoria");
        }

        return categoriaRepository
                .findById(categoriaId)
                .orElseThrow(() -> new CategoriaNotFoundException(categoriaId));
    }

    private Marca resolveMarca(Long marcaId) {
        if (marcaId == null) {
            throw new ArgumentInvalidException("marcaId", "La marca es obligatoria");
        }

        return marcaRepository
                .findById(marcaId)
                .orElseThrow(() -> new MarcaNotFoundException(marcaId));
    }
}
