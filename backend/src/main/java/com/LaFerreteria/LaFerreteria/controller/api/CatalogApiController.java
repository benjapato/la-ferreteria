package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.enums.OperacionActualizacionPrecio;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.service.CategoriaService;
import com.LaFerreteria.LaFerreteria.service.MovimientoStockService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.ProveedorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/catalog")
public class CatalogApiController {

    private static final int PAGE_SIZE = 20;

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final ProveedorService proveedorService;
    private final MovimientoStockService movimientoStockService;

    public CatalogApiController(
            ProductoService productoService,
            CategoriaService categoriaService,
            ProveedorService proveedorService,
            MovimientoStockService movimientoStockService
    ) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.proveedorService = proveedorService;
        this.movimientoStockService = movimientoStockService;
    }

    @GetMapping("/setup")
    public CatalogoOpcionesRespuesta obtenerOpciones() {
        return new CatalogoOpcionesRespuesta(
                listarCategoriasInterno(),
                Arrays.stream(UnidadMedida.values())
                        .map(Enum::name)
                        .toList()
        );
    }

    @GetMapping("/products")
    public ProductosPaginaRespuesta listarProductos(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "TODOS") String status,
            @RequestParam(defaultValue = "TODOS") String stock,
            @RequestParam(defaultValue = "0") int page
    ) {
        Boolean activo = switch (normalizarFiltro(status)) {
            case "ACTIVOS" -> true;
            case "INACTIVOS" -> false;
            case "TODOS" -> null;
            default -> throw new IllegalArgumentException(
                    "El estado seleccionado no es valido"
            );
        };

        String filtroStock = normalizarFiltro(stock);
        if (!List.of("TODOS", "BAJO", "SIN_STOCK").contains(filtroStock)) {
            throw new IllegalArgumentException(
                    "El filtro de stock seleccionado no es valido"
            );
        }

        Page<Producto> pagina = productoService.buscarCatalogo(
                query,
                category,
                activo,
                "BAJO".equals(filtroStock),
                "SIN_STOCK".equals(filtroStock),
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.asc("nombre"),
                                Sort.Order.asc("id")
                        )
                )
        );

        List<Producto> todos = productoService.listarTodos();
        long activos = todos.stream()
                .filter(producto -> Boolean.TRUE.equals(producto.getActivo()))
                .count();
        long stockBajo = todos.stream()
                .filter(this::tieneStockBajo)
                .count();
        long sinStock = todos.stream()
                .filter(producto -> producto.getStockActual()
                        .compareTo(BigDecimal.ZERO) <= 0)
                .count();

        return new ProductosPaginaRespuesta(
                pagina.getContent().stream()
                        .map(this::resumirProducto)
                        .toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isFirst(),
                pagina.isLast(),
                new ProductoMetricasRespuesta(
                        todos.size(),
                        activos,
                        stockBajo,
                        sinStock
                )
        );
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoRespuesta crearProducto(
            @Valid @RequestBody ProductoSolicitud solicitud
    ) {
        Producto producto = construirProducto(solicitud);
        return resumirProducto(productoService.guardar(producto));
    }

    @PutMapping("/products/{id}")
    public ProductoRespuesta actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoSolicitud solicitud
    ) {
        Producto existente = buscarProducto(id);
        Producto producto = construirProducto(solicitud);
        producto.setStockActual(existente.getStockActual());
        return resumirProducto(productoService.actualizar(id, producto));
    }

    @PostMapping("/products/{id}/status")
    public ProductoRespuesta cambiarEstadoProducto(@PathVariable Long id) {
        productoService.cambiarEstado(id);
        return resumirProducto(buscarProducto(id));
    }

    @PostMapping("/products/prices/percentage")
    public ActualizacionPreciosRespuesta actualizarPreciosPorPorcentaje(
            @Valid @RequestBody ActualizacionPreciosSolicitud solicitud
    ) {
        int productosActualizados =
                productoService.actualizarPreciosVentaPorPorcentaje(
                        solicitud.porcentaje(),
                        solicitud.operacion()
                );

        return new ActualizacionPreciosRespuesta(
                productosActualizados,
                solicitud.porcentaje(),
                solicitud.operacion()
        );
    }

    @PostMapping("/products/{id}/stock-adjustments")
    public ProductoRespuesta ajustarStock(
            @PathVariable Long id,
            @Valid @RequestBody AjusteStockSolicitud solicitud
    ) {
        movimientoStockService.ajustarStock(
                id,
                solicitud.tipo(),
                solicitud.cantidad(),
                solicitud.observacion()
        );
        return resumirProducto(buscarProducto(id));
    }

    @GetMapping("/categories")
    public List<CategoriaRespuesta> listarCategorias() {
        return listarCategoriasInterno();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaRespuesta crearCategoria(
            @Valid @RequestBody CategoriaSolicitud solicitud
    ) {
        Categoria categoria = construirCategoria(solicitud);
        return resumirCategoria(categoriaService.guardar(categoria));
    }

    @PutMapping("/categories/{id}")
    public CategoriaRespuesta actualizarCategoria(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaSolicitud solicitud
    ) {
        return resumirCategoria(
                categoriaService.actualizar(
                        id,
                        construirCategoria(solicitud)
                )
        );
    }

    @PostMapping("/categories/{id}/status")
    public CategoriaRespuesta cambiarEstadoCategoria(@PathVariable Long id) {
        categoriaService.cambiarEstado(id);
        return resumirCategoria(
                categoriaService.buscarPorId(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "La categoria seleccionada no existe"
                        ))
        );
    }

    @GetMapping("/providers")
    public List<ProveedorRespuesta> listarProveedores() {
        return proveedorService.listarTodos().stream()
                .map(this::resumirProveedor)
                .toList();
    }

    @PostMapping("/providers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProveedorRespuesta crearProveedor(
            @Valid @RequestBody ProveedorSolicitud solicitud
    ) {
        return resumirProveedor(
                proveedorService.guardar(
                        construirProveedor(solicitud)
                )
        );
    }

    @PutMapping("/providers/{id}")
    public ProveedorRespuesta actualizarProveedor(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorSolicitud solicitud
    ) {
        return resumirProveedor(
                proveedorService.actualizar(
                        id,
                        construirProveedor(solicitud)
                )
        );
    }

    @PostMapping("/providers/{id}/status")
    public ProveedorRespuesta cambiarEstadoProveedor(@PathVariable Long id) {
        proveedorService.cambiarEstado(id);
        return resumirProveedor(
                proveedorService.buscarPorId(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El proveedor seleccionado no existe"
                        ))
        );
    }

    private Producto buscarProducto(Long id) {
        return productoService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El producto seleccionado no existe"
                ));
    }

    private Producto construirProducto(ProductoSolicitud solicitud) {
        Categoria categoria = new Categoria();
        categoria.setId(solicitud.categoriaId());

        Producto producto = new Producto();
        producto.setCodigo(solicitud.codigo());
        producto.setNombre(solicitud.nombre());
        producto.setDescripcion(normalizarOpcional(solicitud.descripcion()));
        producto.setMarca(normalizarOpcional(solicitud.marca()));
        producto.setPrecioCompra(solicitud.precioCompra());
        producto.setPrecioVenta(solicitud.precioVenta());
        producto.setStockActual(solicitud.stockActual());
        producto.setStockMinimo(solicitud.stockMinimo());
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setCategoria(categoria);
        return producto;
    }

    private Categoria construirCategoria(CategoriaSolicitud solicitud) {
        Categoria categoria = new Categoria();
        categoria.setNombre(solicitud.nombre().trim());
        categoria.setDescripcion(normalizarOpcional(solicitud.descripcion()));
        return categoria;
    }

    private Proveedor construirProveedor(ProveedorSolicitud solicitud) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(solicitud.nombre());
        proveedor.setRazonSocial(solicitud.razonSocial());
        proveedor.setCuit(solicitud.cuit());
        proveedor.setTelefono(solicitud.telefono());
        proveedor.setEmail(solicitud.email());
        proveedor.setDireccion(solicitud.direccion());
        return proveedor;
    }

    private List<CategoriaRespuesta> listarCategoriasInterno() {
        return categoriaService.listarTodas().stream()
                .sorted(Comparator.comparing(
                        Categoria::getNombre,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(this::resumirCategoria)
                .toList();
    }

    private ProductoRespuesta resumirProducto(Producto producto) {
        BigDecimal margen = producto.getPrecioVenta()
                .subtract(producto.getPrecioCompra());
        BigDecimal margenPorcentaje = producto.getPrecioCompra()
                .compareTo(BigDecimal.ZERO) > 0
                ? margen.multiply(BigDecimal.valueOf(100))
                .divide(producto.getPrecioCompra(), 2, RoundingMode.HALF_UP)
                : null;

        return new ProductoRespuesta(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getMarca(),
                producto.getPrecioCompra(),
                producto.getPrecioVenta(),
                margen,
                margenPorcentaje,
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getUnidadMedida(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getActivo(),
                estadoStock(producto)
        );
    }

    private CategoriaRespuesta resumirCategoria(Categoria categoria) {
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getActiva()
        );
    }

    private ProveedorRespuesta resumirProveedor(Proveedor proveedor) {
        return new ProveedorRespuesta(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getRazonSocial(),
                proveedor.getCuit(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getDireccion(),
                proveedor.getActivo()
        );
    }

    private boolean tieneStockBajo(Producto producto) {
        return Boolean.TRUE.equals(producto.getActivo())
                && producto.getStockActual().compareTo(
                producto.getStockMinimo()
        ) <= 0;
    }

    private String estadoStock(Producto producto) {
        if (producto.getStockActual().compareTo(BigDecimal.ZERO) <= 0) {
            return "SIN_STOCK";
        }
        if (tieneStockBajo(producto)) {
            return "BAJO";
        }
        return "NORMAL";
    }

    private String normalizarFiltro(String filtro) {
        return filtro == null
                ? "TODOS"
                : filtro.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarOpcional(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }

    public record CatalogoOpcionesRespuesta(
            List<CategoriaRespuesta> categorias,
            List<String> unidadesMedida
    ) {
    }

    public record ProductosPaginaRespuesta(
            List<ProductoRespuesta> productos,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima,
            ProductoMetricasRespuesta metricas
    ) {
    }

    public record ProductoMetricasRespuesta(
            long total,
            long activos,
            long stockBajo,
            long sinStock
    ) {
    }

    public record ProductoRespuesta(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            String marca,
            BigDecimal precioCompra,
            BigDecimal precioVenta,
            BigDecimal margen,
            BigDecimal margenPorcentaje,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            UnidadMedida unidadMedida,
            Long categoriaId,
            String categoria,
            Boolean activo,
            String estadoStock
    ) {
    }

    public record ProductoSolicitud(
            @NotBlank(message = "El codigo es obligatorio")
            @Size(max = 50, message = "El codigo no puede superar los 50 caracteres")
            String codigo,
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
            String nombre,
            @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
            String descripcion,
            @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
            String marca,
            @NotNull(message = "El precio de compra es obligatorio")
            @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El precio de compra admite hasta 2 decimales")
            BigDecimal precioCompra,
            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
            @Digits(integer = 13, fraction = 2, message = "El precio de venta admite hasta 2 decimales")
            BigDecimal precioVenta,
            @NotNull(message = "El stock actual es obligatorio")
            @DecimalMin(value = "0.000", message = "El stock actual no puede ser negativo")
            @Digits(integer = 12, fraction = 3, message = "El stock actual admite hasta 3 decimales")
            BigDecimal stockActual,
            @NotNull(message = "El stock minimo es obligatorio")
            @DecimalMin(value = "0.000", message = "El stock minimo no puede ser negativo")
            @Digits(integer = 12, fraction = 3, message = "El stock minimo admite hasta 3 decimales")
            BigDecimal stockMinimo,
            @NotNull(message = "La unidad de medida es obligatoria")
            UnidadMedida unidadMedida,
            @NotNull(message = "La categoria es obligatoria")
            Long categoriaId
    ) {
    }

    public record ActualizacionPreciosSolicitud(
            @NotNull(message = "El porcentaje es obligatorio")
            @DecimalMin(value = "0.01", message = "El porcentaje debe ser mayor que cero")
            @Digits(integer = 4, fraction = 2, message = "El porcentaje admite hasta 2 decimales")
            BigDecimal porcentaje,
            @NotNull(message = "La operacion es obligatoria")
            OperacionActualizacionPrecio operacion
    ) {
    }

    public record ActualizacionPreciosRespuesta(
            int productosActualizados,
            BigDecimal porcentaje,
            OperacionActualizacionPrecio operacion
    ) {
    }

    public record AjusteStockSolicitud(
            @NotNull(message = "El tipo de ajuste es obligatorio")
            TipoMovimientoStock tipo,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
            @Digits(integer = 12, fraction = 3, message = "La cantidad admite hasta 3 decimales")
            BigDecimal cantidad,
            @Size(max = 500, message = "La observacion no puede superar los 500 caracteres")
            String observacion
    ) {
    }

    public record CategoriaSolicitud(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
            String nombre,
            @Size(max = 255, message = "La descripcion no puede superar los 255 caracteres")
            String descripcion
    ) {
    }

    public record CategoriaRespuesta(
            Long id,
            String nombre,
            String descripcion,
            Boolean activa
    ) {
    }

    public record ProveedorSolicitud(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
            String nombre,
            @Size(max = 150, message = "La razon social no puede superar los 150 caracteres")
            String razonSocial,
            @Size(max = 13, message = "El CUIT no puede superar los 13 caracteres")
            String cuit,
            @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
            String telefono,
            @Email(message = "El correo electronico no es valido")
            @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
            String email,
            @Size(max = 255, message = "La direccion no puede superar los 255 caracteres")
            String direccion
    ) {
    }

    public record ProveedorRespuesta(
            Long id,
            String nombre,
            String razonSocial,
            String cuit,
            String telefono,
            String email,
            String direccion,
            Boolean activo
    ) {
    }
}
