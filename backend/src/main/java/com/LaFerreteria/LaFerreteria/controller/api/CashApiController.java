package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.MovimientoCaja;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCaja;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/cash")
public class CashApiController {

    private static final int PAGE_SIZE = 15;

    private final CajaService cajaService;

    @Value("${app.caja.cierre-automatico.hora:23:00}")
    private String horaCierreAutomatico;

    @Value("${app.caja.cierre-automatico.hora-aviso:22:45}")
    private String horaAvisoCierre;

    @Value("${app.caja.cierre-automatico.zona:America/Argentina/Buenos_Aires}")
    private String zonaCierreAutomatico;

    public CashApiController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public CajasRespuesta listar(
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<Caja> cajas = cajaService.listarTodas(
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.desc("fechaApertura"),
                                Sort.Order.desc("id")
                        )
                )
        );

        CajaDetalleRespuesta actual = cajaService.buscarCajaAbierta()
                .map(this::resumirDetalle)
                .orElse(null);

        CajaDetalleRespuesta pendienteArqueo = cajaService
                .buscarArqueoPendiente()
                .map(this::resumirDetalle)
                .orElse(null);

        return new CajasRespuesta(
                actual,
                pendienteArqueo,
                cajas.getContent().stream().map(this::resumir).toList(),
                cajas.getNumber(),
                cajas.getTotalPages(),
                cajas.getTotalElements(),
                cajas.isFirst(),
                cajas.isLast()
        );
    }

    @GetMapping("/status")
    @Transactional(readOnly = true)
    public EstadoOperativoRespuesta estadoOperativo() {
        Caja cajaAbierta = cajaService.buscarCajaAbierta().orElse(null);
        Caja pendiente = cajaService.buscarArqueoPendiente().orElse(null);
        LocalTime ahora = LocalTime.now(ZoneId.of(zonaCierreAutomatico));
        LocalTime horaAviso = LocalTime.parse(horaAvisoCierre);

        return new EstadoOperativoRespuesta(
                cajaAbierta != null,
                cajaAbierta == null ? null : cajaAbierta.getId(),
                cajaAbierta != null && !ahora.isBefore(horaAviso),
                horaCierreAutomatico,
                pendiente == null ? null : resumirPendiente(pendiente),
                cajaAbierta == null ? cajaService.buscarFaltanteUltimoCierre()
                        .map(caja -> new FaltanteCierreRespuesta(
                                caja.getId(), caja.getFechaCierre(),
                                caja.getEfectivoEsperado(), caja.getEfectivoContado(),
                                caja.getDiferenciaEfectivo().abs()
                        )).orElse(null) : null
        );
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CajaDetalleRespuesta detalle(@PathVariable Long id) {
        Caja caja = cajaService.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la caja con ID " + id
                ));
        return resumirDetalle(caja);
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CajaDetalleRespuesta abrir(
            @Valid @RequestBody AperturaSolicitud solicitud
    ) {
        return resumirDetalle(
                cajaService.abrirCaja(solicitud.montoInicial())
        );
    }

    @PostMapping("/movements")
    @Transactional
    public CajaDetalleRespuesta registrarMovimiento(
            @Valid @RequestBody MovimientoSolicitud solicitud
    ) {
        cajaService.registrarMovimiento(
                solicitud.tipo(),
                solicitud.monto(),
                solicitud.motivo()
        );
        Caja caja = cajaService.buscarCajaAbierta()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Debe abrir la caja antes de realizar esta operacion"
                ));
        return resumirDetalle(caja);
    }

    @PostMapping("/close")
    @Transactional
    public CajaDetalleRespuesta cerrar(
            @Valid @RequestBody CierreSolicitud solicitud
    ) {
        return resumirDetalle(
                cajaService.cerrarCaja(solicitud.efectivoContado())
        );
    }

    @PostMapping("/{id}/reconciliation")
    @Transactional
    public CajaDetalleRespuesta regularizarArqueo(
            @PathVariable Long id,
            @Valid @RequestBody CierreSolicitud solicitud
    ) {
        return resumirDetalle(
                cajaService.regularizarArqueoAutomatico(
                        id,
                        solicitud.efectivoContado()
                )
        );
    }

    private CajaResumenRespuesta resumir(Caja caja) {
        return new CajaResumenRespuesta(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getEstado().name(),
                nombreUsuario(caja.getUsuarioApertura()),
                caja.getUsuarioCierre() == null
                        ? null
                        : caja.getUsuarioCierre().getNombreCompleto(),
                caja.getTotalVentas(),
                caja.getTotalPagosClientes(),
                caja.getTotalGeneral(),
                caja.getEfectivoEsperado(),
                caja.getEfectivoContado(),
                caja.getDiferenciaEfectivo(),
                Boolean.TRUE.equals(caja.getCierreAutomatico()),
                requiereArqueo(caja)
        );
    }

    private CajaDetalleRespuesta resumirDetalle(Caja caja) {
        return new CajaDetalleRespuesta(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getEstado().name(),
                nombreUsuario(caja.getUsuarioApertura()),
                caja.getUsuarioCierre() == null
                        ? null
                        : caja.getUsuarioCierre().getNombreCompleto(),
                new VentasCajaRespuesta(
                        caja.getVentasEfectivo(),
                        caja.getVentasTransferencia(),
                        caja.getVentasTarjetaDebito(),
                        caja.getVentasTarjetaCredito(),
                        caja.getTotalVentas()
                ),
                new PagosCajaRespuesta(
                        caja.getPagosClientesEfectivo(),
                        caja.getPagosClientesTransferencia(),
                        caja.getPagosClientesTarjetaDebito(),
                        caja.getPagosClientesTarjetaCredito(),
                        caja.getPagosClientesOtro(),
                        caja.getTotalPagosClientes()
                ),
                caja.getTotalGeneral(),
                new ArqueoCajaRespuesta(
                        caja.getMontoInicial(),
                        caja.getIngresosEfectivo(),
                        caja.getRetirosEfectivo(),
                        caja.getEfectivoEsperado(),
                        caja.getEfectivoContado(),
                        caja.getDiferenciaEfectivo()
                ),
                Boolean.TRUE.equals(caja.getCierreAutomatico()),
                requiereArqueo(caja),
                cajaService.listarMovimientos(caja.getId()).stream()
                        .map(this::resumirMovimiento)
                        .toList()
        );
    }

    private ArqueoPendienteRespuesta resumirPendiente(Caja caja) {
        return new ArqueoPendienteRespuesta(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getEfectivoEsperado()
        );
    }

    private boolean requiereArqueo(Caja caja) {
        return Boolean.TRUE.equals(caja.getCierreAutomatico())
                && caja.getEfectivoContado() == null;
    }

    private MovimientoCajaRespuesta resumirMovimiento(
            MovimientoCaja movimiento
    ) {
        return new MovimientoCajaRespuesta(
                movimiento.getId(),
                movimiento.getFecha(),
                movimiento.getTipo().name(),
                movimiento.getMonto(),
                movimiento.getMotivo(),
                nombreUsuario(movimiento.getUsuario())
        );
    }

    private String nombreUsuario(
            com.LaFerreteria.LaFerreteria.entity.Usuario usuario
    ) {
        return usuario == null
                ? "Sin registrar"
                : usuario.getNombreCompleto();
    }

    public record AperturaSolicitud(
            @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El monto inicial admite hasta 2 decimales")
            BigDecimal montoInicial
    ) {
    }

    public record MovimientoSolicitud(
            @NotNull(message = "Debe seleccionar el tipo de movimiento")
            TipoMovimientoCaja tipo,
            @NotNull(message = "El monto es obligatorio")
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
            @Digits(integer = 13, fraction = 2, message = "El monto admite hasta 2 decimales")
            BigDecimal monto,
            @Size(max = 200, message = "El motivo no puede superar los 200 caracteres")
            String motivo
    ) {
    }

    public record CierreSolicitud(
            @NotNull(message = "El efectivo contado es obligatorio")
            @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo")
            @Digits(integer = 13, fraction = 2, message = "El efectivo contado admite hasta 2 decimales")
            BigDecimal efectivoContado
    ) {
    }

    public record CajasRespuesta(
            CajaDetalleRespuesta actual,
            CajaDetalleRespuesta pendienteArqueo,
            List<CajaResumenRespuesta> cajas,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima
    ) {
    }

    public record CajaResumenRespuesta(
            Long id,
            LocalDateTime apertura,
            LocalDateTime cierre,
            String estado,
            String abiertaPor,
            String cerradaPor,
            BigDecimal totalVentas,
            BigDecimal pagosClientes,
            BigDecimal totalGeneral,
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            BigDecimal diferencia,
            boolean cierreAutomatico,
            boolean arqueoPendiente
    ) {
    }

    public record CajaDetalleRespuesta(
            Long id,
            LocalDateTime apertura,
            LocalDateTime cierre,
            String estado,
            String abiertaPor,
            String cerradaPor,
            VentasCajaRespuesta ventas,
            PagosCajaRespuesta pagosClientes,
            BigDecimal totalGeneral,
            ArqueoCajaRespuesta arqueo,
            boolean cierreAutomatico,
            boolean arqueoPendiente,
            List<MovimientoCajaRespuesta> movimientos
    ) {
    }

    public record VentasCajaRespuesta(
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal debito,
            BigDecimal credito,
            BigDecimal total
    ) {
    }

    public record PagosCajaRespuesta(
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal debito,
            BigDecimal credito,
            BigDecimal otro,
            BigDecimal total
    ) {
    }

    public record ArqueoCajaRespuesta(
            BigDecimal montoInicial,
            BigDecimal ingresos,
            BigDecimal retiros,
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            BigDecimal diferencia
    ) {
    }

    public record MovimientoCajaRespuesta(
            Long id,
            LocalDateTime fecha,
            String tipo,
            BigDecimal monto,
            String motivo,
            String usuario
    ) {
    }

    public record EstadoOperativoRespuesta(
            boolean cajaAbierta,
            Long cajaId,
            boolean avisoCierre,
            String horaCierre,
            ArqueoPendienteRespuesta arqueoPendiente,
            FaltanteCierreRespuesta faltanteUltimoCierre
    ) {
    }

    public record FaltanteCierreRespuesta(
            Long id,
            LocalDateTime fechaCierre,
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            BigDecimal faltante
    ) {
    }

    public record ArqueoPendienteRespuesta(
            Long id,
            LocalDateTime apertura,
            LocalDateTime cierre,
            BigDecimal efectivoEsperado
    ) {
    }
}
