package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.MovimientoCuentaCorriente;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.LaFerreteria.LaFerreteria.service.ClienteService;
import com.LaFerreteria.LaFerreteria.service.MovimientoCuentaCorrienteService;
import com.LaFerreteria.LaFerreteria.service.PagoClienteService;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
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
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/customers")
public class CustomersApiController {

    private static final int PAGE_SIZE = 20;

    private final ClienteService clienteService;
    private final MovimientoCuentaCorrienteService movimientoService;
    private final PagoClienteService pagoClienteService;
    private final VentaRepository ventaRepository;
    private final CajaService cajaService;

    public CustomersApiController(
            ClienteService clienteService,
            MovimientoCuentaCorrienteService movimientoService,
            PagoClienteService pagoClienteService,
            VentaRepository ventaRepository,
            CajaService cajaService
    ) {
        this.clienteService = clienteService;
        this.movimientoService = movimientoService;
        this.pagoClienteService = pagoClienteService;
        this.ventaRepository = ventaRepository;
        this.cajaService = cajaService;
    }

    @GetMapping
    public ClientesPaginaRespuesta listarClientes(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "TODOS") String status,
            @RequestParam(defaultValue = "TODOS") String balance,
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

        String saldo = normalizarFiltro(balance);
        if (!List.of("TODOS", "DEUDA", "AL_DIA", "A_FAVOR").contains(saldo)) {
            throw new IllegalArgumentException(
                    "El filtro de saldo seleccionado no es valido"
            );
        }

        Page<Cliente> pagina = clienteService.buscarCatalogo(
                query,
                activo,
                "DEUDA".equals(saldo),
                "AL_DIA".equals(saldo),
                "A_FAVOR".equals(saldo),
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.asc("nombre"),
                                Sort.Order.asc("id")
                        )
                )
        );

        List<Cliente> todos = clienteService.listarTodos();
        long activos = todos.stream()
                .filter(cliente -> Boolean.TRUE.equals(cliente.getActivo()))
                .count();
        long conDeuda = todos.stream()
                .filter(cliente -> saldo(cliente).compareTo(BigDecimal.ZERO) > 0)
                .count();
        BigDecimal deudaTotal = todos.stream()
                .map(this::saldo)
                .filter(valor -> valor.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientesPaginaRespuesta(
                pagina.getContent().stream()
                        .map(this::resumirCliente)
                        .toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isFirst(),
                pagina.isLast(),
                new ClienteMetricasRespuesta(
                        todos.size(),
                        activos,
                        conDeuda,
                        deudaTotal
                ),
                cajaService.buscarCajaAbierta().isPresent()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteRespuesta crearCliente(
            @Valid @RequestBody ClienteSolicitud solicitud
    ) {
        return resumirCliente(
                clienteService.guardar(construirCliente(solicitud))
        );
    }

    @PutMapping("/{id}")
    public ClienteRespuesta actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteSolicitud solicitud
    ) {
        Cliente cliente = construirCliente(solicitud);
        cliente.setId(id);
        return resumirCliente(clienteService.guardar(cliente));
    }

    @PostMapping("/{id}/status")
    public ClienteRespuesta cambiarEstado(@PathVariable Long id) {
        return resumirCliente(clienteService.cambiarEstado(id));
    }

    @GetMapping("/{id}")
    public ClienteDetalleRespuesta obtenerCliente(@PathVariable Long id) {
        Cliente cliente = buscarCliente(id);
        return new ClienteDetalleRespuesta(
                resumirCliente(cliente),
                cajaService.buscarCajaAbierta().isPresent(),
                Arrays.stream(MedioPago.values())
                        .map(Enum::name)
                        .toList()
        );
    }

    @GetMapping("/{id}/movements")
    @Transactional(readOnly = true)
    public MovimientosPaginaRespuesta listarMovimientos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        buscarCliente(id);
        Page<MovimientoCuentaCorriente> pagina = movimientoService
                .listarPorCliente(
                        id,
                        PageRequest.of(
                                Math.max(page, 0),
                                PAGE_SIZE,
                                Sort.by(
                                        Sort.Order.desc("fecha"),
                                        Sort.Order.desc("id")
                                )
                        )
                );

        return new MovimientosPaginaRespuesta(
                pagina.getContent().stream()
                        .map(this::resumirMovimiento)
                        .toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }

    @GetMapping("/{id}/sales")
    @Transactional(readOnly = true)
    public VentasClientePaginaRespuesta listarVentas(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page
    ) {
        buscarCliente(id);
        Page<Venta> pagina = ventaRepository.findByClienteId(
                id,
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.desc("fecha"),
                                Sort.Order.desc("id")
                        )
                )
        );

        return new VentasClientePaginaRespuesta(
                pagina.getContent().stream()
                        .map(this::resumirVenta)
                        .toList(),
                pagina.getNumber(),
                pagina.getTotalPages(),
                pagina.getTotalElements(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }

    @PostMapping("/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoClienteRespuesta registrarPago(
            @PathVariable Long id,
            @Valid @RequestBody PagoClienteSolicitud solicitud
    ) {
        PagoCliente pago = new PagoCliente();
        pago.setMonto(solicitud.monto());
        pago.setMedioPago(solicitud.medioPago());
        pago.setObservacion(solicitud.observacion());

        PagoCliente guardado = pagoClienteService.registrarPago(id, pago);
        return resumirPago(guardado);
    }

    @PostMapping("/{clienteId}/payments/{pagoId}/cancel")
    public PagoClienteRespuesta anularPago(
            @PathVariable Long clienteId,
            @PathVariable Long pagoId,
            @Valid @RequestBody AnulacionPagoSolicitud solicitud
    ) {
        return resumirPago(
                pagoClienteService.anularPago(
                        clienteId,
                        pagoId,
                        solicitud.motivo()
                )
        );
    }

    private Cliente buscarCliente(Long id) {
        return clienteService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente seleccionado no existe"
                ));
    }

    private Cliente construirCliente(ClienteSolicitud solicitud) {
        Cliente cliente = new Cliente();
        cliente.setNombre(solicitud.nombre());
        cliente.setDocumento(solicitud.documento());
        cliente.setTelefono(solicitud.telefono());
        cliente.setEmail(solicitud.email());
        cliente.setDireccion(solicitud.direccion());
        return cliente;
    }

    private ClienteRespuesta resumirCliente(Cliente cliente) {
        BigDecimal saldo = saldo(cliente);
        return new ClienteRespuesta(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getDocumento(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                saldo,
                estadoSaldo(saldo),
                cliente.getActivo()
        );
    }

    private MovimientoRespuesta resumirMovimiento(
            MovimientoCuentaCorriente movimiento
    ) {
        PagoCliente pago = movimiento.getPagoCliente();
        return new MovimientoRespuesta(
                movimiento.getId(),
                movimiento.getFecha(),
                movimiento.getTipo().name(),
                movimiento.getImporte(),
                movimiento.getSaldoAnterior(),
                movimiento.getSaldoPosterior(),
                movimiento.getDescripcion(),
                movimiento.getVenta() == null
                        ? null
                        : movimiento.getVenta().getId(),
                pago == null ? null : resumirPago(pago)
        );
    }

    private VentaClienteRespuesta resumirVenta(Venta venta) {
        return new VentaClienteRespuesta(
                venta.getId(),
                venta.getFecha(),
                venta.getFormaPago().name(),
                venta.getEstado().name(),
                venta.getTotal(),
                venta.getUsuario() == null
                        ? "Sin registrar"
                        : venta.getUsuario().getNombreCompleto()
        );
    }

    private PagoClienteRespuesta resumirPago(PagoCliente pago) {
        return new PagoClienteRespuesta(
                pago.getId(),
                pago.getFecha(),
                pago.getMonto(),
                pago.getMedioPago().name(),
                pago.getSaldoAnterior(),
                pago.getSaldoPosterior(),
                pago.getObservacion(),
                Boolean.TRUE.equals(pago.getAnulado()),
                pago.getFechaAnulacion(),
                pago.getMotivoAnulacion(),
                pago.getCaja() == null ? null : pago.getCaja().getId(),
                pago.getUsuario() == null
                        ? "Sin registrar"
                        : pago.getUsuario().getNombreCompleto()
        );
    }

    private BigDecimal saldo(Cliente cliente) {
        return cliente.getSaldoCuentaCorriente() == null
                ? BigDecimal.ZERO
                : cliente.getSaldoCuentaCorriente();
    }

    private String estadoSaldo(BigDecimal saldo) {
        int comparacion = saldo.compareTo(BigDecimal.ZERO);
        if (comparacion > 0) {
            return "DEUDA";
        }
        if (comparacion < 0) {
            return "A_FAVOR";
        }
        return "AL_DIA";
    }

    private String normalizarFiltro(String filtro) {
        return filtro == null
                ? "TODOS"
                : filtro.trim().toUpperCase(Locale.ROOT);
    }

    public record ClientesPaginaRespuesta(
            List<ClienteRespuesta> clientes,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima,
            ClienteMetricasRespuesta metricas,
            boolean cajaAbierta
    ) {
    }

    public record ClienteMetricasRespuesta(
            long total,
            long activos,
            long conDeuda,
            BigDecimal deudaTotal
    ) {
    }

    public record ClienteDetalleRespuesta(
            ClienteRespuesta cliente,
            boolean cajaAbierta,
            List<String> mediosPago
    ) {
    }

    public record ClienteRespuesta(
            Long id,
            String nombre,
            String documento,
            String telefono,
            String email,
            String direccion,
            BigDecimal saldo,
            String estadoSaldo,
            Boolean activo
    ) {
    }

    public record ClienteSolicitud(
            @NotBlank(message = "El nombre del cliente es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
            String nombre,
            @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
            String documento,
            @Size(max = 50, message = "El telefono no puede superar los 50 caracteres")
            String telefono,
            @Email(message = "El correo electronico no tiene un formato valido")
            @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
            String email,
            @Size(max = 250, message = "La direccion no puede superar los 250 caracteres")
            String direccion
    ) {
    }

    public record MovimientosPaginaRespuesta(
            List<MovimientoRespuesta> movimientos,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima
    ) {
    }

    public record MovimientoRespuesta(
            Long id,
            java.time.LocalDateTime fecha,
            String tipo,
            BigDecimal importe,
            BigDecimal saldoAnterior,
            BigDecimal saldoPosterior,
            String descripcion,
            Long ventaId,
            PagoClienteRespuesta pago
    ) {
    }

    public record VentasClientePaginaRespuesta(
            List<VentaClienteRespuesta> ventas,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima
    ) {
    }

    public record VentaClienteRespuesta(
            Long id,
            java.time.LocalDateTime fecha,
            String formaPago,
            String estado,
            BigDecimal total,
            String vendedor
    ) {
    }

    public record PagoClienteSolicitud(
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
            @Digits(integer = 13, fraction = 2, message = "El monto admite hasta 2 decimales")
            BigDecimal monto,
            @NotNull(message = "El medio de pago es obligatorio")
            MedioPago medioPago,
            @Size(max = 300, message = "La observacion no puede superar los 300 caracteres")
            String observacion
    ) {
    }

    public record AnulacionPagoSolicitud(
            @Size(max = 300, message = "El motivo no puede superar los 300 caracteres")
            String motivo
    ) {
    }

    public record PagoClienteRespuesta(
            Long id,
            java.time.LocalDateTime fecha,
            BigDecimal monto,
            String medioPago,
            BigDecimal saldoAnterior,
            BigDecimal saldoPosterior,
            String observacion,
            boolean anulado,
            java.time.LocalDateTime fechaAnulacion,
            String motivoAnulacion,
            Long cajaId,
            String usuario
    ) {
    }
}
