package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.DetalleVenta;
import com.LaFerreteria.LaFerreteria.entity.PagoVenta;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.LaFerreteria.LaFerreteria.service.CategoriaService;
import com.LaFerreteria.LaFerreteria.service.ClienteService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import com.LaFerreteria.LaFerreteria.service.VentaFiltroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/sales")
public class SalesApiController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final CajaService cajaService;
    private final VentaFiltroService ventaFiltroService;

    public SalesApiController(
            VentaService ventaService,
            ProductoService productoService,
            ClienteService clienteService,
            CategoriaService categoriaService,
            CajaService cajaService,
            VentaFiltroService ventaFiltroService
    ) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.cajaService = cajaService;
        this.ventaFiltroService = ventaFiltroService;
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public HistorialRespuesta obtenerHistorial(
            @RequestParam(name = "number", required = false)
            String number,
            @RequestParam(name = "client", required = false)
            String client,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(name = "status", required = false)
            String status,
            @RequestParam(name = "payment", required = false)
            String payment,
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        Page<Venta> sales = ventaFiltroService.buscar(
                number,
                client,
                from,
                to,
                status,
                payment,
                PageRequest.of(
                        Math.max(page, 0),
                        20,
                        Sort.by(
                                Sort.Order.desc("fecha"),
                                Sort.Order.desc("id")
                        )
                )
        );

        return new HistorialRespuesta(
                sales.getContent()
                        .stream()
                        .map(this::resumirVentaHistorial)
                        .toList(),
                sales.getNumber(),
                sales.getTotalPages(),
                sales.getTotalElements(),
                sales.isFirst(),
                sales.isLast()
        );
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public VentaDetalleRespuesta obtenerVenta(
            @org.springframework.web.bind.annotation.PathVariable
            Long id
    ) {
        return resumirVentaDetalle(
                ventaService.buscarCompletaPorId(id)
        );
    }

    @PostMapping("/{id}/cancel")
    public VentaDetalleRespuesta cancelarVenta(
            @org.springframework.web.bind.annotation.PathVariable
            Long id
    ) {
        ventaService.cancelar(id);
        return resumirVentaDetalle(
                ventaService.buscarCompletaPorId(id)
        );
    }

    @GetMapping("/setup")
    @Transactional(readOnly = true)
    public SetupRespuesta obtenerSetup() {
        return new SetupRespuesta(
                buscarProductosInterno(""),
                buscarClientesInterno(""),
                categoriaService.listarActivas()
                        .stream()
                        .map(this::resumirCategoria)
                        .toList(),
                Arrays.asList(UnidadMedida.values()),
                Arrays.asList(FormaPago.values()),
                cajaService.buscarCajaAbierta()
                        .map(this::resumirCaja)
                        .orElse(null)
        );
    }

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<ProductoRespuesta> buscarProductos(
            @RequestParam(name = "query", defaultValue = "")
            String query
    ) {
        return buscarProductosInterno(query);
    }

    @GetMapping("/clients")
    @Transactional(readOnly = true)
    public List<ClienteRespuesta> buscarClientes(
            @RequestParam(name = "query", defaultValue = "")
            String query
    ) {
        return buscarClientesInterno(query);
    }

    @PostMapping("/cash/open")
    @ResponseStatus(HttpStatus.CREATED)
    public CajaRespuesta abrirCaja(
            @Valid @RequestBody AperturaCajaSolicitud solicitud
    ) {
        return resumirCaja(
                cajaService.abrirCaja(solicitud.montoInicial())
        );
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteRespuesta crearCliente(
            @Valid @RequestBody ClienteSolicitud solicitud
    ) {
        Cliente cliente = new Cliente();
        cliente.setNombre(solicitud.nombre());
        cliente.setDocumento(solicitud.documento());
        cliente.setTelefono(solicitud.telefono());
        cliente.setEmail(solicitud.email());
        cliente.setDireccion(solicitud.direccion());

        return resumirCliente(
                clienteService.guardar(cliente)
        );
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaRespuesta crearCategoria(
            @Valid @RequestBody CategoriaSolicitud solicitud
    ) {
        Categoria categoria = new Categoria();
        categoria.setNombre(solicitud.nombre().trim());
        categoria.setDescripcion(limpiar(solicitud.descripcion()));
        categoria.setActiva(true);

        return resumirCategoria(
                categoriaService.guardar(categoria)
        );
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoRespuesta crearProducto(
            @Valid @RequestBody ProductoSolicitud solicitud
    ) {
        Categoria categoria = new Categoria();
        categoria.setId(solicitud.categoriaId());

        Producto producto = new Producto();
        producto.setCodigo(solicitud.codigo());
        producto.setNombre(solicitud.nombre());
        producto.setDescripcion(limpiar(solicitud.descripcion()));
        producto.setMarca(limpiar(solicitud.marca()));
        producto.setPrecioCompra(solicitud.precioCompra());
        producto.setPrecioVenta(solicitud.precioVenta());
        producto.setStockActual(solicitud.stockActual());
        producto.setStockMinimo(solicitud.stockMinimo());
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setCategoria(categoria);

        return resumirProducto(
                productoService.guardar(producto)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaCreadaRespuesta crearVenta(
            @Valid @RequestBody VentaSolicitud solicitud
    ) {
        Venta venta = new Venta();
        venta.setFormaPago(solicitud.formaPago());
        venta.setNombreCliente(limpiar(solicitud.nombreCliente()));
        venta.setObservaciones(limpiar(solicitud.observaciones()));

        if (solicitud.clienteId() != null) {
            Cliente cliente = new Cliente();
            cliente.setId(solicitud.clienteId());
            venta.setCliente(cliente);
        }

        List<DetalleVenta> detalles = solicitud.detalles()
                .stream()
                .map(detalleSolicitud -> {
                    Producto producto = new Producto();
                    producto.setId(detalleSolicitud.productoId());

                    DetalleVenta detalle = new DetalleVenta();
                    detalle.setProducto(producto);
                    detalle.setCantidad(detalleSolicitud.cantidad());
                    return detalle;
                })
                .toList();

        venta.setDetalles(new ArrayList<>(detalles));

        List<PagoVenta> pagos = solicitud.pagos() == null
                ? List.of()
                : solicitud.pagos()
                .stream()
                .map(pagoSolicitud -> {
                    PagoVenta pago = new PagoVenta();
                    pago.setFormaPago(pagoSolicitud.formaPago());
                    pago.setMonto(pagoSolicitud.monto());
                    return pago;
                })
                .toList();

        venta.setPagos(new ArrayList<>(pagos));

        return resumirVenta(
                ventaService.crear(venta)
        );
    }

    private List<ProductoRespuesta> buscarProductosInterno(
            String query
    ) {
        return productoService
                .buscarPrimerosActivosPorNombre(query)
                .stream()
                .map(this::resumirProducto)
                .toList();
    }

    private List<ClienteRespuesta> buscarClientesInterno(
            String query
    ) {
        String filtro = normalizar(query);

        return clienteService.listarActivos()
                .stream()
                .filter(cliente -> filtro.isBlank()
                        || normalizar(
                        cliente.getNombre() + " "
                                + cliente.getDocumento()
                ).contains(filtro))
                .limit(16)
                .map(this::resumirCliente)
                .toList();
    }

    private ProductoRespuesta resumirProducto(Producto producto) {
        return new ProductoRespuesta(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getMarca(),
                producto.getPrecioVenta(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getUnidadMedida(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre()
        );
    }

    private ClienteRespuesta resumirCliente(Cliente cliente) {
        return new ClienteRespuesta(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getDocumento(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getSaldoCuentaCorriente()
        );
    }

    private CategoriaRespuesta resumirCategoria(
            Categoria categoria
    ) {
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre()
        );
    }

    private CajaRespuesta resumirCaja(Caja caja) {
        return new CajaRespuesta(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getMontoInicial(),
                caja.getEfectivoEsperado(),
                caja.getTotalGeneral()
        );
    }

    private VentaCreadaRespuesta resumirVenta(Venta venta) {
        String cliente = venta.getNombreCliente();
        if (cliente == null || cliente.isBlank()) {
            cliente = "Consumidor final";
        }

        return new VentaCreadaRespuesta(
                venta.getId(),
                venta.getFecha(),
                cliente,
                venta.getCliente() == null
                        ? null
                        : venta.getCliente().getDocumento(),
                venta.getCliente() == null
                        ? null
                        : venta.getCliente().getDireccion(),
                venta.getFormaPago(),
                venta.getEstado().name(),
                venta.getTotal(),
                venta.getObservaciones(),
                venta.getUsuario() == null
                        ? "Sin registrar"
                        : venta.getUsuario().getNombreCompleto(),
                venta.getCaja() == null
                        ? null
                        : venta.getCaja().getId(),
                venta.getDetalles()
                        .stream()
                        .map(detalle -> new DetalleVentaRespuesta(
                                detalle.getProducto().getId(),
                                detalle.getProducto().getCodigo(),
                                detalle.getProducto().getNombre(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario(),
                                detalle.getSubtotal()
                        ))
                        .toList(),
                venta.getPagos()
                        .stream()
                        .map(pago -> new PagoRespuesta(
                                pago.getFormaPago(),
                                pago.getMonto()
                        ))
                        .toList()
        );
    }

    private VentaHistorialItem resumirVentaHistorial(Venta venta) {
        return new VentaHistorialItem(
                venta.getId(),
                venta.getFecha(),
                nombreCliente(venta),
                venta.getUsuario() == null
                        ? "Sin registrar"
                        : venta.getUsuario().getNombreCompleto(),
                venta.getFormaPago(),
                venta.getEstado().name(),
                venta.getTotal()
        );
    }

    private VentaDetalleRespuesta resumirVentaDetalle(Venta venta) {
        return new VentaDetalleRespuesta(
                venta.getId(),
                venta.getFecha(),
                nombreCliente(venta),
                venta.getCliente() == null
                        ? null
                        : venta.getCliente().getId(),
                venta.getCliente() == null
                        ? null
                        : venta.getCliente().getDocumento(),
                venta.getCliente() == null
                        ? null
                        : venta.getCliente().getDireccion(),
                venta.getFormaPago(),
                venta.getEstado().name(),
                venta.getTotal(),
                venta.getCostoTotal(),
                venta.getGananciaTotal(),
                venta.getObservaciones(),
                venta.getUsuario() == null
                        ? "Sin registrar"
                        : venta.getUsuario().getNombreCompleto(),
                venta.getCaja() == null
                        ? null
                        : venta.getCaja().getId(),
                venta.getDetalles()
                        .stream()
                        .map(detalle -> new DetalleVentaCompletoRespuesta(
                                detalle.getProducto().getId(),
                                detalle.getProducto().getCodigo(),
                                detalle.getProducto().getNombre(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario(),
                                detalle.getSubtotal(),
                                detalle.getCostoUnitario(),
                                detalle.getCostoSubtotal(),
                                detalle.getGanancia()
                        ))
                        .toList(),
                venta.getPagos()
                        .stream()
                        .map(pago -> new PagoRespuesta(
                                pago.getFormaPago(),
                                pago.getMonto()
                        ))
                        .toList()
        );
    }

    private String nombreCliente(Venta venta) {
        if (venta.getNombreCliente() != null
                && !venta.getNombreCliente().isBlank()) {
            return venta.getNombreCliente();
        }

        if (venta.getCliente() != null) {
            return venta.getCliente().getNombre();
        }

        return "Consumidor final";
    }

    private String limpiar(String texto) {
        return texto == null || texto.isBlank()
                ? null
                : texto.trim();
    }

    private String normalizar(String texto) {
        String valor = texto == null ? "" : texto;
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public record SetupRespuesta(
            List<ProductoRespuesta> productos,
            List<ClienteRespuesta> clientes,
            List<CategoriaRespuesta> categorias,
            List<UnidadMedida> unidadesMedida,
            List<FormaPago> formasPago,
            CajaRespuesta caja
    ) {
    }

    public record HistorialRespuesta(
            List<VentaHistorialItem> ventas,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima
    ) {
    }

    public record VentaHistorialItem(
            Long id,
            LocalDateTime fecha,
            String cliente,
            String vendedor,
            FormaPago formaPago,
            String estado,
            BigDecimal total
    ) {
    }

    public record AperturaCajaSolicitud(
            @DecimalMin(
                    value = "0.00",
                    message = "El monto inicial no puede ser negativo"
            )
            BigDecimal montoInicial
    ) {
    }

    public record ClienteSolicitud(
            @NotBlank(message = "El nombre del cliente es obligatorio")
            @Size(max = 150)
            String nombre,
            @Size(max = 30)
            String documento,
            @Size(max = 50)
            String telefono,
            @Email(message = "El correo electronico no tiene un formato valido")
            @Size(max = 150)
            String email,
            @Size(max = 250)
            String direccion
    ) {
    }

    public record CategoriaSolicitud(
            @NotBlank(message = "El nombre de la categoria es obligatorio")
            @Size(max = 100)
            String nombre,
            @Size(max = 255)
            String descripcion
    ) {
    }

    public record ProductoSolicitud(
            @NotBlank(message = "El codigo es obligatorio")
            @Size(max = 50)
            String codigo,
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150)
            String nombre,
            @Size(max = 500)
            String descripcion,
            @Size(max = 100)
            String marca,
            @NotNull(message = "El precio de compra es obligatorio")
            @DecimalMin(value = "0.00", message = "El precio de compra no puede ser negativo")
            BigDecimal precioCompra,
            @NotNull(message = "El precio de venta es obligatorio")
            @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
            BigDecimal precioVenta,
            @NotNull(message = "El stock actual es obligatorio")
            @DecimalMin(value = "0.001", message = "El stock actual debe ser mayor que cero")
            BigDecimal stockActual,
            @NotNull(message = "El stock minimo es obligatorio")
            @DecimalMin(value = "0.000", message = "El stock minimo no puede ser negativo")
            BigDecimal stockMinimo,
            @NotNull(message = "La unidad de medida es obligatoria")
            UnidadMedida unidadMedida,
            @NotNull(message = "La categoria es obligatoria")
            Long categoriaId
    ) {
    }

    public record VentaSolicitud(
            Long clienteId,
            @Size(max = 150)
            String nombreCliente,
            @NotNull(message = "Debe seleccionar una forma de pago")
            FormaPago formaPago,
            @Size(max = 500)
            String observaciones,
            @NotEmpty(message = "Debe agregar al menos un producto")
            List<@Valid DetalleSolicitud> detalles,
            List<@Valid PagoSolicitud> pagos
    ) {
    }

    public record DetalleSolicitud(
            @NotNull(message = "Debe seleccionar un producto")
            Long productoId,
            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
            @Digits(integer = 12, fraction = 3, message = "La cantidad admite hasta 3 decimales")
            BigDecimal cantidad
    ) {
    }

    public record PagoSolicitud(
            @NotNull(message = "La forma de pago es obligatoria")
            FormaPago formaPago,
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
            BigDecimal monto
    ) {
    }

    public record ProductoRespuesta(
            Long id,
            String codigo,
            String nombre,
            String marca,
            BigDecimal precio,
            BigDecimal stock,
            BigDecimal stockMinimo,
            UnidadMedida unidadMedida,
            Long categoriaId,
            String categoria
    ) {
    }

    public record ClienteRespuesta(
            Long id,
            String nombre,
            String documento,
            String telefono,
            String email,
            BigDecimal saldo
    ) {
    }

    public record CategoriaRespuesta(
            Long id,
            String nombre
    ) {
    }

    public record CajaRespuesta(
            Long id,
            LocalDateTime fechaApertura,
            BigDecimal montoInicial,
            BigDecimal efectivoEsperado,
            BigDecimal totalGeneral
    ) {
    }

    public record VentaCreadaRespuesta(
            Long id,
            LocalDateTime fecha,
            String cliente,
            String documentoCliente,
            String direccionCliente,
            FormaPago formaPago,
            String estado,
            BigDecimal total,
            String observaciones,
            String vendedor,
            Long cajaId,
            List<DetalleVentaRespuesta> detalles,
            List<PagoRespuesta> pagos
    ) {
    }

    public record VentaDetalleRespuesta(
            Long id,
            LocalDateTime fecha,
            String cliente,
            Long clienteId,
            String documentoCliente,
            String direccionCliente,
            FormaPago formaPago,
            String estado,
            BigDecimal total,
            BigDecimal costoTotal,
            BigDecimal gananciaTotal,
            String observaciones,
            String vendedor,
            Long cajaId,
            List<DetalleVentaCompletoRespuesta> detalles,
            List<PagoRespuesta> pagos
    ) {
    }

    public record DetalleVentaRespuesta(
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
    }

    public record DetalleVentaCompletoRespuesta(
            Long productoId,
            String codigo,
            String nombre,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            BigDecimal costoUnitario,
            BigDecimal costoSubtotal,
            BigDecimal ganancia
    ) {
    }

    public record PagoRespuesta(
            FormaPago formaPago,
            BigDecimal monto
    ) {
    }
}
