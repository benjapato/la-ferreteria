package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.dto.ReporteVentasDTO;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class InicioController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final CompraService compraService;

    public InicioController(
            VentaService ventaService,
            ProductoService productoService,
            CompraService compraService
    ) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.compraService = compraService;
    }

    @GetMapping({"/", "/inicio"})
    public String mostrarInicio(Model model) {

        LocalDate hoy = LocalDate.now();

        ReporteVentasDTO reporteHoy =
                ventaService.generarReporte(hoy, hoy);

        List<Producto> productosStockBajo =
                productoService.listarConStockBajo();

        model.addAttribute(
                "cantidadVentasHoy",
                reporteHoy.cantidadVentas()
        );

        model.addAttribute(
                "totalVendidoHoy",
                reporteHoy.totalVendido()
        );

        model.addAttribute(
                "gananciaHoy",
                reporteHoy.gananciaTotal()
        );

        model.addAttribute(
                "cantidadComprasPendientes",
                compraService.listarPendientes().size()
        );

        model.addAttribute(
                "cantidadStockBajo",
                productosStockBajo.size()
        );

        model.addAttribute(
                "productosStockBajo",
                productosStockBajo.stream()
                        .limit(5)
                        .toList()
        );

        model.addAttribute(
                "ventasRecientes",
                ventaService.listarTodas(
                        PageRequest.of(
                                0,
                                5,
                                Sort.by("fecha").descending()
                        )
                ).getContent()
        );

        return "inicio";
    }
}
