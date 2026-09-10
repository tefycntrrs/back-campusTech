package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.dto.ImagenProductoRequest;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.DuplicateResourceException;
import com.uade.ecommerce.exception.ForbiddenException;
import com.uade.ecommerce.exception.MarcaNotFoundException;
import com.uade.ecommerce.exception.ProductoNotFoundException;
import com.uade.ecommerce.exception.UsuarioNotFoundException;
import com.uade.ecommerce.model.Categoria;
import com.uade.ecommerce.model.Marca;
import com.uade.ecommerce.model.Producto;
import com.uade.ecommerce.model.ProductoImagen;
import com.uade.ecommerce.model.Usuario;
import com.uade.ecommerce.repository.CategoriaRepository;
import com.uade.ecommerce.repository.MarcaRepository;
import com.uade.ecommerce.repository.ProductoRepository;
import com.uade.ecommerce.repository.UsuarioRepository;
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
    private final UsuarioRepository usuarioRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            MarcaRepository marcaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Producto> getCatalogo() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    public Producto getProductoById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
    }

    public Producto getProductoPublico(Long id) {
        Producto producto = getProductoById(id);

        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ProductoNotFoundException(id);
        }

        return producto;
    }

    public List<Producto> getProductosByCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new CategoriaNotFoundException(categoriaId);
        }

        return productoRepository.findActivosByCategoriaId(categoriaId);
    }

    public Producto createProducto(CreateProductoRequest request) {
        Categoria categoria = resolveCategoria(request.getCategoriaId());
        Marca marca = resolveMarca(request.getMarcaId());
        Usuario vendedor = resolveVendedor(request.getUsuarioId());

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

        if (request.getImagenes() == null || request.getImagenes().isEmpty()) {
            throw new ArgumentInvalidException("imagenes", "El producto debe tener al menos una imagen");
        }

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setSku(sku);
        producto.setCategoria(categoria);
        producto.setMarca(marca);
        producto.setVendedor(vendedor);

        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        reemplazarImagenes(producto, request.getImagenes());

        return productoRepository.save(producto);
    }

    public Producto updateProducto(Long id, Long usuarioId, CreateProductoRequest request) {
        Producto producto = getProductoById(id);
        verificarPropietario(producto, usuarioId);

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

        if (request.getImagenes() != null) {
            if (request.getImagenes().isEmpty()) {
                throw new ArgumentInvalidException("imagenes", "El producto debe tener al menos una imagen");
            }
            reemplazarImagenes(producto, request.getImagenes());
        }

        return productoRepository.save(producto);
    }

    public void eliminarProducto(Long id, Long usuarioId) {
        Producto producto = getProductoById(id);
        verificarPropietario(producto, usuarioId);

        if (Boolean.FALSE.equals(producto.getActivo())) {
            return;
        }

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private void reemplazarImagenes(Producto producto, List<ImagenProductoRequest> imagenes) {

        producto.limpiarImagenes();

        boolean yaHayPrincipal = false;

        for (int posicion = 0; posicion < imagenes.size(); posicion++) {

            ImagenProductoRequest datos = imagenes.get(posicion);

            if (datos == null || datos.getUrl() == null || datos.getUrl().isBlank()) {
                throw new ArgumentInvalidException("imagenes", "Cada imagen necesita una url");
            }

            boolean principal = Boolean.TRUE.equals(datos.getPrincipal()) && !yaHayPrincipal;
            yaHayPrincipal = yaHayPrincipal || principal;

            Integer orden = datos.getOrden();
            if (orden == null) {
                orden = posicion;
            }

            ProductoImagen imagen = new ProductoImagen();
            imagen.setUrl(datos.getUrl().trim());
            imagen.setOrden(orden);
            imagen.setPrincipal(principal);

            producto.agregarImagen(imagen);
        }

        if (!yaHayPrincipal) {
            producto.getImagenes().get(0).setPrincipal(true);
        }
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

    private Usuario resolveVendedor(Long usuarioId) {
        if (usuarioId == null) {
            throw new ArgumentInvalidException("usuarioId", "El usuario que publica el producto es obligatorio");
        }

        return usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new UsuarioNotFoundException(usuarioId));
    }

    private void verificarPropietario(Producto producto, Long usuarioId) {
        if (usuarioId == null) {
            throw new ArgumentInvalidException("usuarioId", "Falta indicar el usuario que realiza la operación");
        }

        if (!usuarioRepository.existsById(usuarioId)) {
            throw new UsuarioNotFoundException(usuarioId);
        }

        if (!producto.perteneceA(usuarioId)) {
            throw new ForbiddenException("El producto pertenece a otro usuario");
        }
    }
}
