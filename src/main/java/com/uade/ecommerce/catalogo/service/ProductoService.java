package com.uade.ecommerce.catalogo.service;

import com.uade.ecommerce.catalogo.dto.CreateProductoRequest;
import com.uade.ecommerce.catalogo.dto.ImagenProductoRequest;
import com.uade.ecommerce.catalogo.dto.ProductoResponse;
import com.uade.ecommerce.catalogo.model.Categoria;
import com.uade.ecommerce.catalogo.model.Marca;
import com.uade.ecommerce.catalogo.model.Producto;
import com.uade.ecommerce.catalogo.model.ProductoImagen;
import com.uade.ecommerce.catalogo.repository.CategoriaRepository;
import com.uade.ecommerce.catalogo.repository.MarcaRepository;
import com.uade.ecommerce.catalogo.repository.ProductoRepository;
import com.uade.ecommerce.identidad.model.Usuario;
import com.uade.ecommerce.identidad.repository.UsuarioRepository;
import com.uade.ecommerce.shared.exception.*;
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

    public List<ProductoResponse> getCatalogo() {
        return productoRepository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /** Cantidad de productos activos en el catálogo. */
    public long getTotalActivos() {
        return productoRepository.countByActivoTrue();
    }

    /** Busca en el catálogo por nombre (coincidencia parcial, sin distinguir mayúsculas). */
    public List<ProductoResponse> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArgumentInvalidException("q", "El texto de búsqueda es obligatorio");
        }

        return productoRepository
                .findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(texto.trim())
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /** Detalle público: un producto dado de baja (activo = false) se comporta como inexistente. */
    public ProductoResponse getProductoPublico(Long id) {
        Producto producto = buscarProducto(id);

        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ProductoNotFoundException(id);
        }

        return ProductoResponse.from(producto);
    }

    public List<ProductoResponse> getProductosByCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new CategoriaNotFoundException(categoriaId);
        }

        return productoRepository.findActivosByCategoriaId(categoriaId)
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /** Publicaciones de un usuario: el otro lado de Usuario 1:N Producto. */
    public List<ProductoResponse> getProductosByVendedor(Long vendedorId) {
        if (!usuarioRepository.existsById(vendedorId)) {
            throw new UsuarioNotFoundException(vendedorId);
        }

        return productoRepository.findByVendedorId(vendedorId)
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    /**
     * Publica un producto. El vendedor ya no llega en el body: lo pasa el controller a partir del
     * usuario autenticado (ítem 17), así nadie puede publicar a nombre de otro.
     *
     * @param vendedorId id del usuario autenticado que publica
     */
    public ProductoResponse createProducto(CreateProductoRequest request, Long vendedorId) {
        Set<Categoria> categorias = resolveCategorias(request.categoriasSolicitadas());
        Marca marca = resolveMarca(request.getMarcaId());
        Usuario vendedor = resolveVendedor(vendedorId);

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
        producto.setCategorias(categorias);
        producto.setMarca(marca);
        producto.setVendedor(vendedor);

        if (request.getActivo() != null) {
            producto.setActivo(request.getActivo());
        }

        reemplazarImagenes(producto, request.getImagenes());

        return ProductoResponse.from(productoRepository.save(producto));
    }

    /**
     * Actualización parcial. El {@code usuarioId} es el del usuario autenticado, no un query param
     * (ítem 17): antes cualquiera podía mandar ?usuarioId= con el id del dueño y editar un
     * producto ajeno.
     */
    public ProductoResponse updateProducto(Long id, Long usuarioId, CreateProductoRequest request) {
        Producto producto = buscarProducto(id);
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

        // El vendedor no se toca: un producto no cambia de dueño desde la API

        // Igual que las categorías: si mandan imágenes, reemplazan la galería completa
        if (request.getImagenes() != null) {
            if (request.getImagenes().isEmpty()) {
                throw new ArgumentInvalidException("imagenes", "El producto debe tener al menos una imagen");
            }
            reemplazarImagenes(producto, request.getImagenes());
        }

        return ProductoResponse.from(productoRepository.save(producto));
    }

    /** Baja lógica. Igual que el update: el usuarioId es el del autenticado, no un query param. */
    public void eliminarProducto(Long id, Long usuarioId) {
        Producto producto = buscarProducto(id);
        verificarPropietario(producto, usuarioId);

        if (Boolean.FALSE.equals(producto.getActivo())) {
            return;
        }

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    /**
     * Uso interno: devuelve la entidad para poder consultarla y modificarla (getProductoPublico,
     * updateProducto y eliminarProducto pasan por acá). Es privado a propósito: la entidad
     * Producto no sale de este service, hacia afuera solo viajan DTOs.
     */
    private Producto buscarProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNotFoundException(id));
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

    /**
     * El vendedor sale del usuario autenticado, así que en condiciones normales siempre existe.
     * Las dos verificaciones quedan como red de seguridad por si el service se llama desde otro lado.
     */
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

    /**
     * Solo el vendedor puede modificar o dar de baja su publicación. El id ya no lo elige el
     * cliente: viene del token, así que un 403 acá significa de verdad "este producto es de otro".
     */
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