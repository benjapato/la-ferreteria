package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.dto.ProductoMasVendidoDTO;
import com.LaFerreteria.LaFerreteria.dto.ReporteCajaDTO;
import com.LaFerreteria.LaFerreteria.dto.ReporteVentasDTO;
import com.LaFerreteria.LaFerreteria.service.ReporteCajaService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final VentaService ventaService;
    private final ReporteCajaService
            reporteCajaService;

    public ReporteController(
            VentaService ventaService,
            ReporteCajaService reporteCajaService
    ) {
        this.ventaService = ventaService;
        this.reporteCajaService =
                reporteCajaService;
    }

    /*
     * Reporte general de ventas.
     */
    @GetMapping("/ventas")
    public String reporteVentas(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta,

            Model model
    ) {

        if (desde == null) {
            desde = LocalDate.now();
        }

        if (hasta == null) {
            hasta = LocalDate.now();
        }

        model.addAttribute(
                "desde",
                desde
        );

        model.addAttribute(
                "hasta",
                hasta
        );

        try {

            ReporteVentasDTO reporte =
                    ventaService.generarReporte(
                            desde,
                            hasta
                    );

            model.addAttribute(
                    "reporte",
                    reporte
            );

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "reportes/ventas";
    }

    /*
     * Reporte de productos más vendidos.
     */
    @GetMapping("/productos")
    public String reporteProductos(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta,

            Model model
    ) {

        if (desde == null) {

            desde = LocalDate
                    .now()
                    .withDayOfMonth(1);
        }

        if (hasta == null) {

            hasta = LocalDate.now();
        }

        model.addAttribute(
                "desde",
                desde
        );

        model.addAttribute(
                "hasta",
                hasta
        );

        try {

            List<ProductoMasVendidoDTO> productos =
                    ventaService
                            .generarReporteProductosMasVendidos(
                                    desde,
                                    hasta
                            );

            model.addAttribute(
                    "productos",
                    productos
            );

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "reportes/productos";
    }

    /*
     * Reporte de ingresos de caja.
     */
    @GetMapping("/caja")
    public String reporteCaja(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate desde,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate hasta,

            Model model
    ) {

        /*
         * Por defecto se muestra desde
         * el primer día del mes hasta hoy.
         */
        if (desde == null) {

            desde = LocalDate
                    .now()
                    .withDayOfMonth(1);
        }

        if (hasta == null) {

            hasta = LocalDate.now();
        }

        model.addAttribute(
                "desde",
                desde
        );

        model.addAttribute(
                "hasta",
                hasta
        );

        try {

            ReporteCajaDTO reporte =
                    reporteCajaService
                            .generarReporte(
                                    desde,
                                    hasta
                            );

            model.addAttribute(
                    "reporte",
                    reporte
            );

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "reportes/caja";
    }
}