package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.dto.ProductoMasVendidoDTO;
import com.LaFerreteria.LaFerreteria.dto.ReporteCajaDTO;
import com.LaFerreteria.LaFerreteria.dto.ReporteVentasDTO;
import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.service.ReporteCajaService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsApiController {

    private final VentaService ventaService;
    private final ReporteCajaService reporteCajaService;

    public ReportsApiController(
            VentaService ventaService,
            ReporteCajaService reporteCajaService
    ) {
        this.ventaService = ventaService;
        this.reporteCajaService = reporteCajaService;
    }

    @GetMapping("/sales")
    @Transactional(readOnly = true)
    public ReporteVentasRespuesta ventas(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        Periodo periodo = periodo(from, to, false);
        ReporteVentasDTO reporte = ventaService.generarReporte(
                periodo.desde(),
                periodo.hasta()
        );

        return new ReporteVentasRespuesta(
                periodo,
                new MetricasVentas(
                        reporte.cantidadVentas(),
                        reporte.totalVendido(),
                        reporte.costoTotal(),
                        reporte.gananciaTotal()
                ),
                reporte.ventas().stream()
                        .map(this::resumirVenta)
                        .toList()
        );
    }

    @GetMapping("/products")
    public ReporteProductosRespuesta productos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        Periodo periodo = periodo(from, to, true);
        List<ProductoMasVendidoDTO> productos = ventaService
                .generarReporteProductosMasVendidos(
                        periodo.desde(),
                        periodo.hasta()
                );

        BigDecimal cantidad = productos.stream()
                .map(ProductoMasVendidoDTO::cantidadVendida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal facturado = productos.stream()
                .map(ProductoMasVendidoDTO::totalVendido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ganancia = productos.stream()
                .map(ProductoMasVendidoDTO::gananciaGenerada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteProductosRespuesta(
                periodo,
                new MetricasProductos(
                        productos.size(),
                        cantidad,
                        facturado,
                        ganancia
                ),
                productos
        );
    }

    @GetMapping("/cash")
    @Transactional(readOnly = true)
    public ReporteCajaRespuesta caja(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        Periodo periodo = periodo(from, to, true);
        ReporteCajaDTO reporte = reporteCajaService.generarReporte(
                periodo.desde(),
                periodo.hasta()
        );

        return new ReporteCajaRespuesta(
                periodo,
                new ResumenCaja(
                        reporte.totalIngresosVentas(),
                        reporte.totalPagosClientes(),
                        reporte.totalGeneral(),
                        reporte.totalVentasFacturadas()
                ),
                new DesgloseVentasCaja(
                        reporte.ventasEfectivo(),
                        reporte.ventasTransferencia(),
                        reporte.ventasTarjetaDebito(),
                        reporte.ventasTarjetaCredito(),
                        reporte.ventasCuentaCorriente(),
                        reporte.totalIngresosVentas()
                ),
                new DesglosePagosCaja(
                        reporte.pagosClientesEfectivo(),
                        reporte.pagosClientesTransferencia(),
                        reporte.pagosClientesTarjetaDebito(),
                        reporte.pagosClientesTarjetaCredito(),
                        reporte.pagosClientesOtro(),
                        reporte.totalPagosClientes()
                ),
                new ContadoresCaja(
                        reporte.cantidadVentas(),
                        reporte.cantidadPagosClientes(),
                        reporte.cantidadCajas(),
                        reporte.cajasAbiertas(),
                        reporte.cajasCerradas()
                ),
                reporte.cajas().stream().map(this::resumirCaja).toList(),
                reporte.ventas().stream().map(this::resumirVentaCaja).toList(),
                reporte.pagosClientes().stream().map(this::resumirPago).toList()
        );
    }

    private Periodo periodo(
            LocalDate desde,
            LocalDate hasta,
            boolean desdeInicioMes
    ) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaDesde = desde == null
                ? (desdeInicioMes ? hoy.withDayOfMonth(1) : hoy)
                : desde;
        LocalDate fechaHasta = hasta == null ? hoy : hasta;
        return new Periodo(fechaDesde, fechaHasta);
    }

    private VentaReporte resumirVenta(Venta venta) {
        return new VentaReporte(
                venta.getId(),
                venta.getFecha(),
                nombreCliente(venta),
                venta.getUsuario() == null
                        ? "Sin registrar"
                        : venta.getUsuario().getNombreCompleto(),
                venta.getFormaPago().name(),
                venta.getTotal(),
                venta.getCostoTotal(),
                venta.getGananciaTotal()
        );
    }

    private CajaReporte resumirCaja(Caja caja) {
        return new CajaReporte(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getEstado().name(),
                caja.getTotalVentas(),
                caja.getTotalPagosClientes(),
                caja.getTotalGeneral(),
                caja.getEfectivoEsperado(),
                caja.getEfectivoContado(),
                caja.getDiferenciaEfectivo()
        );
    }

    private VentaCajaReporte resumirVentaCaja(Venta venta) {
        BigDecimal ingresoCaja;

        if (venta.getFormaPago() == FormaPago.CUENTA_CORRIENTE) {
            ingresoCaja = BigDecimal.ZERO;
        } else if (venta.getFormaPago() == FormaPago.COMBINADO) {
            ingresoCaja = venta.getPagos().stream()
                    .filter(pago -> pago.getFormaPago()
                            != FormaPago.CUENTA_CORRIENTE)
                    .map(pago -> pago.getMonto() == null
                            ? BigDecimal.ZERO
                            : pago.getMonto())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            ingresoCaja = venta.getTotal();
        }

        return new VentaCajaReporte(
                venta.getId(),
                venta.getFecha(),
                nombreCliente(venta),
                venta.getFormaPago().name(),
                venta.getTotal(),
                ingresoCaja
        );
    }

    private PagoClienteReporte resumirPago(PagoCliente pago) {
        return new PagoClienteReporte(
                pago.getId(),
                pago.getFecha(),
                pago.getCliente().getId(),
                pago.getCliente().getNombre(),
                pago.getMedioPago().name(),
                pago.getMonto(),
                pago.getObservacion()
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

    public record Periodo(LocalDate desde, LocalDate hasta) {
    }

    public record ReporteVentasRespuesta(
            Periodo periodo,
            MetricasVentas metricas,
            List<VentaReporte> ventas
    ) {
    }

    public record MetricasVentas(
            int cantidad,
            BigDecimal total,
            BigDecimal costo,
            BigDecimal ganancia
    ) {
    }

    public record VentaReporte(
            Long id,
            LocalDateTime fecha,
            String cliente,
            String usuario,
            String formaPago,
            BigDecimal total,
            BigDecimal costo,
            BigDecimal ganancia
    ) {
    }

    public record ReporteProductosRespuesta(
            Periodo periodo,
            MetricasProductos metricas,
            List<ProductoMasVendidoDTO> productos
    ) {
    }

    public record MetricasProductos(
            int productos,
            BigDecimal unidades,
            BigDecimal facturado,
            BigDecimal ganancia
    ) {
    }

    public record ReporteCajaRespuesta(
            Periodo periodo,
            ResumenCaja resumen,
            DesgloseVentasCaja ventasPorMedio,
            DesglosePagosCaja pagosPorMedio,
            ContadoresCaja contadores,
            List<CajaReporte> cajas,
            List<VentaCajaReporte> ventas,
            List<PagoClienteReporte> pagos
    ) {
    }

    public record ResumenCaja(
            BigDecimal ingresosVentas,
            BigDecimal pagosClientes,
            BigDecimal totalIngresado,
            BigDecimal ventasFacturadas
    ) {
    }

    public record DesgloseVentasCaja(
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal debito,
            BigDecimal credito,
            BigDecimal cuentaCorriente,
            BigDecimal totalIngresado
    ) {
    }

    public record DesglosePagosCaja(
            BigDecimal efectivo,
            BigDecimal transferencia,
            BigDecimal debito,
            BigDecimal credito,
            BigDecimal otro,
            BigDecimal total
    ) {
    }

    public record ContadoresCaja(
            int ventas,
            int pagos,
            int cajas,
            int abiertas,
            int cerradas
    ) {
    }

    public record CajaReporte(
            Long id,
            LocalDateTime apertura,
            LocalDateTime cierre,
            String estado,
            BigDecimal ventas,
            BigDecimal pagosClientes,
            BigDecimal total,
            BigDecimal efectivoEsperado,
            BigDecimal efectivoContado,
            BigDecimal diferencia
    ) {
    }

    public record VentaCajaReporte(
            Long id,
            LocalDateTime fecha,
            String cliente,
            String formaPago,
            BigDecimal totalFacturado,
            BigDecimal ingresoCaja
    ) {
    }

    public record PagoClienteReporte(
            Long id,
            LocalDateTime fecha,
            Long clienteId,
            String cliente,
            String medioPago,
            BigDecimal monto,
            String observacion
    ) {
    }
}
