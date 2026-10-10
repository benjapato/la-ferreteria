package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.enums.OperacionActualizacionPrecio;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ProductoService {

    private static final BigDecimal PRECIO_MAXIMO =
            new BigDecimal("9999999999999.99");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAllByOrderByNombreAsc();
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Producto> buscarPrimerosActivosPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return productoRepository
                    .findTop6ByActivoTrueOrderByNombreAsc();
        }

        return productoRepository
                .findTop6ByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(
                        nombre.trim()
                );
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto guardar(Producto producto) {

        normalizarDatos(producto);

        if (productoRepository.existsByCodigoIgnoreCase(producto.getCodigo())) {
            throw new IllegalArgumentException(
                    "Ya existe un producto con el código " + producto.getCodigo()
            );
        }

        Categoria categoria = buscarCategoriaValida(
                producto.getCategoria()
        );

        producto.setCategoria(categoria);
        producto.setActivo(true);

        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datosProducto) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el producto con ID " + id
                        )
                );

        normalizarDatos(datosProducto);

        Optional<Producto> productoConMismoCodigo =
                productoRepository.findByCodigoIgnoreCase(
                        datosProducto.getCodigo()
                );

        if (productoConMismoCodigo.isPresent()
                && !productoConMismoCodigo.get().getId().equals(id)) {

            throw new IllegalArgumentException(
                    "Ya existe otro producto con el código "
                            + datosProducto.getCodigo()
            );
        }

        Categoria categoria = buscarCategoriaValida(
                datosProducto.getCategoria()
        );

        producto.setCodigo(datosProducto.getCodigo());
        producto.setNombre(datosProducto.getNombre());
        producto.setDescripcion(datosProducto.getDescripcion());
        producto.setMarca(datosProducto.getMarca());
        producto.setPrecioCompra(datosProducto.getPrecioCompra());
        producto.setPrecioVenta(datosProducto.getPrecioVenta());
        producto.setStockActual(datosProducto.getStockActual());
        producto.setStockMinimo(datosProducto.getStockMinimo());
        producto.setUnidadMedida(datosProducto.getUnidadMedida());
        producto.setCategoria(categoria);

        return productoRepository.save(producto);
    }

    public void cambiarEstado(Long id) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el producto con ID " + id
                        )
                );

        producto.setActivo(
                !Boolean.TRUE.equals(producto.getActivo())
        );

        productoRepository.save(producto);
    }

    @Transactional
    public int actualizarPreciosVentaPorPorcentaje(
            BigDecimal porcentaje,
            OperacionActualizacionPrecio operacion
    ) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El porcentaje debe ser mayor que cero"
            );
        }

        if (operacion == OperacionActualizacionPrecio.DISMINUIR
                && porcentaje.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException(
                    "El porcentaje de disminucion debe ser menor que 100"
            );
        }

        BigDecimal variacion = porcentaje.divide(
                BigDecimal.valueOf(100),
                8,
                RoundingMode.HALF_UP
        );
        BigDecimal factor = operacion == OperacionActualizacionPrecio.AUMENTAR
                ? BigDecimal.ONE.add(variacion)
                : BigDecimal.ONE.subtract(variacion);

        List<Producto> productos = productoRepository.findAllByOrderByNombreAsc();
        List<PrecioActualizado> preciosActualizados = productos.stream()
                .map(producto -> {
                    BigDecimal nuevoPrecio = producto.getPrecioVenta()
                            .multiply(factor)
                            .setScale(2, RoundingMode.HALF_UP);

                    if (operacion == OperacionActualizacionPrecio.AUMENTAR
                            && nuevoPrecio.compareTo(
                                    producto.getPrecioCompra()
                            ) < 0) {
                        nuevoPrecio = producto.getPrecioCompra()
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    return new PrecioActualizado(producto, nuevoPrecio);
                })
                .toList();

        for (PrecioActualizado precioActualizado : preciosActualizados) {
            Producto producto = precioActualizado.producto();
            BigDecimal nuevoPrecio = precioActualizado.precioVenta();

            if (nuevoPrecio.compareTo(PRECIO_MAXIMO) > 0) {
                throw new IllegalArgumentException(
                        "El nuevo precio de " + producto.getNombre()
                                + " supera el maximo permitido"
                );
            }

            if (nuevoPrecio.compareTo(producto.getPrecioCompra()) < 0) {
                throw new IllegalArgumentException(
                        "No se pueden actualizar los precios porque "
                                + producto.getNombre()
                                + " quedaria con un precio de venta menor al de compra"
                );
            }
        }

        preciosActualizados.forEach(precioActualizado ->
                precioActualizado.producto().setPrecioVenta(
                        precioActualizado.precioVenta()
                )
        );
        productoRepository.saveAll(productos);
        return productos.size();
    }

    private record PrecioActualizado(
            Producto producto,
            BigDecimal precioVenta
    ) {
    }

    private Categoria buscarCategoriaValida(Categoria categoriaRecibida) {

        if (categoriaRecibida == null
                || categoriaRecibida.getId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar una categoría"
            );
        }

        Categoria categoria = categoriaRepository
                .findById(categoriaRecibida.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La categoría seleccionada no existe"
                        )
                );

        if (!Boolean.TRUE.equals(categoria.getActiva())) {
            throw new IllegalArgumentException(
                    "La categoría seleccionada está inactiva"
            );
        }

        return categoria;
    }

    private void normalizarDatos(Producto producto) {

        BigDecimal precioCompra = producto.getPrecioCompra();
        BigDecimal precioVenta = producto.getPrecioVenta();

        if (precioCompra != null
                && precioVenta != null
                && precioVenta.compareTo(precioCompra) < 0) {
            throw new IllegalArgumentException(
                    "El precio de venta no puede ser menor al precio de compra"
            );
        }

        if (producto.getCodigo() != null) {
            producto.setCodigo(
                    producto.getCodigo().trim().toUpperCase()
            );
        }

        if (producto.getNombre() != null) {
            producto.setNombre(producto.getNombre().trim());
        }

        if (producto.getDescripcion() != null) {
            producto.setDescripcion(
                    producto.getDescripcion().trim()
            );
        }

        if (producto.getMarca() != null) {
            producto.setMarca(producto.getMarca().trim());
        }
    }

    @Transactional(readOnly = true)
    public List<Producto> listarConStockBajo() {
        return productoRepository.buscarProductosConStockBajo();
    }

    @Transactional(readOnly = true)
    public List<Producto> buscar(String texto) {

        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }

        return productoRepository
                .buscarPorCodigoONombre(
                        texto.trim()
                );
    }

    @Transactional(readOnly = true)
    public Page<Producto> buscar(
            String texto,
            Pageable pageable
    ) {

        if (texto == null || texto.isBlank()) {
            return productoRepository.findAll(pageable);
        }

        return productoRepository.buscarPaginaPorCodigoONombre(
                texto.trim(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<Producto> buscarCatalogo(
            String texto,
            Long categoriaId,
            Boolean activo,
            boolean soloStockBajo,
            boolean soloSinStock,
            Pageable pageable
    ) {
        String textoNormalizado = texto == null || texto.isBlank()
                ? ""
                : texto.trim();

        return productoRepository.buscarCatalogo(
                textoNormalizado,
                categoriaId,
                activo,
                soloStockBajo,
                soloSinStock,
                pageable
        );
    }
}
