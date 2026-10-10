package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.entity.DetalleCompra;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.enums.EstadoCompra;
import com.LaFerreteria.LaFerreteria.service.CompraFiltroService;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.ProveedorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/purchases")
public class PurchasesApiController {

    private static final int PAGE_SIZE = 20;

    private final CompraService compraService;
    private final CompraFiltroService compraFiltroService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;

    public PurchasesApiController(
            CompraService compraService,
            CompraFiltroService compraFiltroService,
            ProveedorService proveedorService,
            ProductoService productoService
    ) {
        this.compraService = compraService;
        this.compraFiltroService = compraFiltroService;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ComprasPaginaRespuesta listar(
            @RequestParam(defaultValue = "") String number,
            @RequestParam(defaultValue = "") String supplier,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page
    ) {
        String estado = normalizarEstado(status);
        Page<Compra> pagina = compraFiltroService.buscar(
                number,
                supplier,
                from,
                to,
                estado,
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.desc("fecha"),
                                Sort.Order.desc("id")
                        )
                )
        );

        List<Compra> todas = compraService.listarTodas();
        long pendientes = contarEstado(todas, EstadoCompra.PENDIENTE);
        long confirmadas = contarEstado(todas, EstadoCompra.CONFIRMADA);
        BigDecimal invertido = todas.stream()
                .filter(compra -> compra.getEstado() == EstadoCompra.CONFIRMADA)
                .map(Compra::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ComprasPaginaRespuesta(
                pagina.getContent().stream().map(this::resumir).toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isFirst(),
                pagina.isLast(),
                new CompraMetricasRespuesta(
                        todas.size(),
                        pendientes,
                        confirmadas,
                        invertido
                )
        );
    }

    @GetMapping("/setup")
    @Transactional(readOnly = true)
    public CompraConfiguracionRespuesta configuracion() {
        return new CompraConfiguracionRespuesta(
                proveedorService.listarActivos().stream()
                        .map(this::resumirProveedor)
                        .toList(),
                productoService.listarActivos().stream()
                        .map(this::resumirProducto)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public CompraDetalleRespuesta detalle(@PathVariable Long id) {
        return resumirDetalle(compraService.buscarCompletaPorId(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraDetalleRespuesta crear(
            @Valid @RequestBody CompraSolicitud solicitud
    ) {
        Compra compra = new Compra();
        Proveedor proveedor = new Proveedor();
        proveedor.setId(solicitud.proveedorId());
        compra.setProveedor(proveedor);
        compra.setObservaciones(solicitud.observaciones());
        compra.setDetalles(solicitud.detalles().stream()
                .map(this::construirDetalle)
                .toList());

        Compra creada = compraService.crear(compra);
        return resumirDetalle(
                compraService.buscarCompletaPorId(creada.getId())
        );
    }

    @PostMapping("/{id}/confirm")
    public CompraDetalleRespuesta confirmar(@PathVariable Long id) {
        compraService.confirmar(id);
        return resumirDetalle(compraService.buscarCompletaPorId(id));
    }

    @PostMapping("/{id}/cancel")
    public CompraDetalleRespuesta cancelar(@PathVariable Long id) {
        compraService.cancelar(id);
        return resumirDetalle(compraService.buscarCompletaPorId(id));
    }

    private DetalleCompra construirDetalle(DetalleCompraSolicitud solicitud) {
        Producto producto = new Producto();
        producto.setId(solicitud.productoId());

        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(solicitud.cantidad());
        detalle.setCostoUnitario(solicitud.costoUnitario());
        return detalle;
    }

    private CompraRespuesta resumir(Compra compra) {
        return new CompraRespuesta(
                compra.getId(),
                compra.getFecha(),
                compra.getProveedor().getId(),
                compra.getProveedor().getNombre(),
                compra.getEstado().name(),
                compra.getTotal(),
                compra.getUsuario() == null
                        ? "Sin registrar"
                        : compra.getUsuario().getNombreCompleto()
        );
    }

    private CompraDetalleRespuesta resumirDetalle(Compra compra) {
        return new CompraDetalleRespuesta(
                compra.getId(),
                compra.getFecha(),
                resumirProveedor(compra.getProveedor()),
                compra.getEstado().name(),
                compra.getTotal(),
                compra.getObservaciones(),
                compra.getUsuario() == null
                        ? "Sin registrar"
                        : compra.getUsuario().getNombreCompleto(),
                compra.getDetalles().stream()
                        .map(detalle -> new DetalleCompraRespuesta(
                                detalle.getId(),
                                detalle.getProducto().getId(),
                                detalle.getProducto().getCodigo(),
                                detalle.getProducto().getNombre(),
                                detalle.getProducto().getUnidadMedida().name(),
                                detalle.getCantidad(),
                                detalle.getCostoUnitario(),
                                detalle.getSubtotal()
                        ))
                        .toList()
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
                proveedor.getDireccion()
        );
    }

    private ProductoCompraRespuesta resumirProducto(Producto producto) {
        return new ProductoCompraRespuesta(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getMarca(),
                producto.getCategoria().getNombre(),
                producto.getUnidadMedida().name(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getPrecioCompra()
        );
    }

    private long contarEstado(List<Compra> compras, EstadoCompra estado) {
        return compras.stream()
                .filter(compra -> compra.getEstado() == estado)
                .count();
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }

        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        try {
            EstadoCompra.valueOf(normalizado);
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalArgumentException(
                    "El estado de compra seleccionado no es valido"
            );
        }
        return normalizado;
    }

    public record ComprasPaginaRespuesta(
            List<CompraRespuesta> compras,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima,
            CompraMetricasRespuesta metricas
    ) {
    }

    public record CompraMetricasRespuesta(
            long total,
            long pendientes,
            long confirmadas,
            BigDecimal invertido
    ) {
    }

    public record CompraRespuesta(
            Long id,
            LocalDateTime fecha,
            Long proveedorId,
            String proveedor,
            String estado,
            BigDecimal total,
            String usuario
    ) {
    }

    public record CompraConfiguracionRespuesta(
            List<ProveedorRespuesta> proveedores,
            List<ProductoCompraRespuesta> productos
    ) {
    }

    public record ProveedorRespuesta(
            Long id,
            String nombre,
            String razonSocial,
            String cuit,
            String telefono,
            String email,
            String direccion
    ) {
    }

    public record ProductoCompraRespuesta(
            Long id,
            String codigo,
            String nombre,
            String marca,
            String categoria,
            String unidadMedida,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            BigDecimal costoActual
    ) {
    }

    public record CompraDetalleRespuesta(
            Long id,
            LocalDateTime fecha,
            ProveedorRespuesta proveedor,
            String estado,
            BigDecimal total,
            String observaciones,
            String usuario,
            List<DetalleCompraRespuesta> detalles
    ) {
    }

    public record DetalleCompraRespuesta(
            Long id,
            Long productoId,
            String codigo,
            String producto,
            String unidadMedida,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            BigDecimal subtotal
    ) {
    }

    public record CompraSolicitud(
            @NotNull(message = "Debe seleccionar un proveedor")
            Long proveedorId,
            @Size(max = 500, message = "Las observaciones no pueden superar los 500 caracteres")
            String observaciones,
            @NotEmpty(message = "Debe agregar al menos un producto a la compra")
            List<@Valid DetalleCompraSolicitud> detalles
    ) {
    }

    public record DetalleCompraSolicitud(
            @NotNull(message = "Debe seleccionar un producto")
            Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
            @Digits(integer = 12, fraction = 3, message = "La cantidad admite hasta 3 decimales")
            BigDecimal cantidad,
            @NotNull(message = "El costo unitario es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor que cero")
            @Digits(integer = 13, fraction = 2, message = "El costo admite hasta 2 decimales")
            BigDecimal costoUnitario
    ) {
    }
}
