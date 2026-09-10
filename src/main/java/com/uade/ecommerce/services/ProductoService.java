package com.uade.ecommerce.services;

import com.uade.ecommerce.dto.CreateProductoRequest;
import com.uade.ecommerce.exception.ArgumentInvalidException;
import com.uade.ecommerce.exception.CategoriaNotFoundException;
import com.uade.ecommerce.exception.DuplicateResourceException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** Publicaciones de un usuario: el otro lado de Usuario 1:N Producto. */
    public List<Producto> getProductosByVendedor(Long vendedorId) {
        if (!usuarioRepository.existsById(vendedorId)) {
            throw new UsuarioNotFoundException(vendedorId);
        }

        return productoRepository.findByVendedorId(vendedorId);
    }

    public Producto createProducto(CreateProductoRequest request) {
        Set<Categoria> categorias = resolveCategorias(request.categoriasSolicitadas());
        Marca marca = resolveMarca(request.getMarcaId());
        Usuario vendedor = resolveVendedor(request.getVendedorId());

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
        producto.setCategorias(categorias);
        producto.setMarca(marca);
        producto.setVendedor(vendedor);

        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        agregarImagenes(producto, request.getImagenes());

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

        // Si mandan categorías, reemplazan por completo a las que tenía el producto
        List<Long> categoriaIds = request.categoriasSolicitadas();
        Set<Categoria> categorias = null;
        if (categoriaIds != null) {
            categorias = resolveCategorias(categoriaIds);
        }

        Marca marca = null;
        if (request.getMarcaId() != null) {
            marca = resolveMarca(request.getMarcaId());
        }

        Usuario vendedor = null;
        if (request.getVendedorId() != null) {
            vendedor = resolveVendedor(request.getVendedorId());
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

        if (categorias != null) {
            producto.setCategorias(categorias);
        }

        if (marca != null) {
            producto.setMarca(marca);
        }

        if (vendedor != null) {
            producto.setVendedor(vendedor);
        }

        // Igual que las categorías: si mandan imágenes, reemplazan la galería completa
        if (request.getImagenes() != null) {
            producto.getImagenes().clear();
            agregarImagenes(producto, request.getImagenes());
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

    /**
     * Busca todas las categorías pedidas. Un producto tiene que tener al menos una, y si
     * alguno de los ids no existe corta con 404 en vez de guardar el producto a medias.
     */
    private Set<Categoria> resolveCategorias(List<Long> categoriaIds) {
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            throw new ArgumentInvalidException(
                    "categoriaIds",
                    "El producto debe tener al menos una categoría"
            );
        }

        Set<Categoria> categorias = new LinkedHashSet<>();

        for (Long categoriaId : categoriaIds) {
            if (categoriaId == null) {
                throw new ArgumentInvalidException("categoriaIds", "Hay un id de categoría vacío");
            }

            categorias.add(categoriaRepository
                    .findById(categoriaId)
                    .orElseThrow(() -> new CategoriaNotFoundException(categoriaId)));
        }

        return categorias;
    }

    private Marca resolveMarca(Long marcaId) {
        if (marcaId == null) {
            throw new ArgumentInvalidException("marcaId", "La marca es obligatoria");
        }

        return marcaRepository
                .findById(marcaId)
                .orElseThrow(() -> new MarcaNotFoundException(marcaId));
    }

    private Usuario resolveVendedor(Long vendedorId) {
        if (vendedorId == null) {
            throw new ArgumentInvalidException(
                    "vendedorId",
                    "El vendedor es obligatorio: toda publicación tiene que tener un usuario que la creó"
            );
        }

        return usuarioRepository
                .findById(vendedorId)
                .orElseThrow(() -> new UsuarioNotFoundException(vendedorId));
    }

    /** Arma la galería del producto: la primera imagen queda marcada como principal. */
    private void agregarImagenes(Producto producto, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }

        int orden = 0;

        for (String url : urls) {
            if (url == null || url.isBlank()) {
                throw new ArgumentInvalidException("imagenes", "Hay una URL de imagen vacía");
            }

            ProductoImagen imagen = new ProductoImagen();
            imagen.setUrl(url.trim());
            imagen.setOrden(orden);
            imagen.setPrincipal(orden == 0);

            producto.agregarImagen(imagen);
            orden++;
        }
    }
}
