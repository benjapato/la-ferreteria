package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.dto.ReporteVentasDTO;
import com.LaFerreteria.LaFerreteria.dto.VentasPorMedioDTO;
import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CompraService compraService;
    private final CajaService cajaService;

    public DashboardApiController(
            VentaService ventaService,
            ProductoService productoService,
            CompraService compraService,
            CajaService cajaService
    ) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.compraService = compraService;
        this.cajaService = cajaService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public DashboardRespuesta obtenerDashboard() {
        LocalDate hoy = LocalDate.now();

        ReporteVentasDTO reporte =
                ventaService.generarReporte(hoy, hoy);

        List<Producto> productosStockBajo =
                productoService.listarConStockBajo();

        List<VentaResumen> ventasRecientes =
                ventaService.listarTodas(
                                PageRequest.of(
                                        0,
                                        5,
                                        Sort.by(
                                                "fecha"
                                        ).descending()
                                )
                        )
                        .getContent()
                        .stream()
                        .map(this::resumirVenta)
                        .toList();

        return new DashboardRespuesta(
                new MetricasRespuesta(
                        reporte.cantidadVentas(),
                        reporte.totalVendido(),
                        reporte.gananciaTotal(),
                        compraService
                                .listarPendientes()
                                .size(),
                        productosStockBajo.size()
                ),
                cajaService.buscarCajaAbierta()
                        .map(this::resumirCaja)
                        .orElse(null),
                cajaService.buscarArqueoPendiente()
                        .map(this::resumirCajaPendiente)
                        .orElse(null),
                productosStockBajo.stream()
                        .limit(6)
                        .map(this::resumirProducto)
                        .toList(),
                ventasRecientes,
                ventaService.resumirMediosDePagoDelDia(hoy)
        );
    }

    private VentaResumen resumirVenta(Venta venta) {
        String cliente = "Consumidor final";

        if (venta.getCliente() != null) {
            cliente = venta.getCliente().getNombre();
        } else if (venta.getNombreCliente() != null
                && !venta.getNombreCliente().isBlank()) {
            cliente = venta.getNombreCliente();
        }

        return new VentaResumen(
                venta.getId(),
                venta.getFecha(),
                cliente,
                venta.getFormaPago().name(),
                venta.getEstado().name(),
                venta.getTotal(),
                venta.getGananciaTotal()
        );
    }

    private ProductoStockRespuesta resumirProducto(
            Producto producto
    ) {
        return new ProductoStockRespuesta(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getStockActual(),
                producto.getStockMinimo(),
                producto.getUnidadMedida().name()
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

    private CajaPendienteRespuesta resumirCajaPendiente(Caja caja) {
        return new CajaPendienteRespuesta(
                caja.getId(),
                caja.getFechaApertura(),
                caja.getFechaCierre(),
                caja.getEfectivoEsperado()
        );
    }

    public record DashboardRespuesta(
            MetricasRespuesta metricas,
            CajaRespuesta caja,
            CajaPendienteRespuesta cajaPendienteArqueo,
            List<ProductoStockRespuesta> productosStockBajo,
            List<VentaResumen> ventasRecientes,
            VentasPorMedioDTO ventasPorMedio
    ) {
    }

    public record MetricasRespuesta(
            long cantidadVentas,
            BigDecimal totalVendido,
            BigDecimal ganancia,
            int comprasPendientes,
            int stockBajo
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

    public record CajaPendienteRespuesta(
            Long id,
            LocalDateTime fechaApertura,
            LocalDateTime fechaCierre,
            BigDecimal efectivoEsperado
    ) {
    }

    public record ProductoStockRespuesta(
            Long id,
            String codigo,
            String nombre,
            BigDecimal stockActual,
            BigDecimal stockMinimo,
            String unidad
    ) {
    }

    public record VentaResumen(
            Long id,
            LocalDateTime fecha,
            String cliente,
            String formaPago,
            String estado,
            BigDecimal total,
            BigDecimal ganancia
    ) {
    }
}
