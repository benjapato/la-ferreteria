package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.service.VentaFiltroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/ventas")
public class VentaFiltroController {

    private final VentaFiltroService ventaFiltroService;

    public VentaFiltroController(
            VentaFiltroService ventaFiltroService
    ) {
        this.ventaFiltroService =
                ventaFiltroService;
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam(
                    name = "numero",
                    required = false
            )
            String numero,

            @RequestParam(
                    name = "cliente",
                    required = false
            )
            String cliente,

            @RequestParam(
                    name = "fechaDesde",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaDesde,

            @RequestParam(
                    name = "fechaHasta",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaHasta,

            @RequestParam(
                    name = "estado",
                    required = false
            )
            String estado,

            @RequestParam(
                    name = "formaPago",
                    required = false
            )
            String formaPago,

            @RequestParam(
                    name = "pagina",
                    defaultValue = "0"
            )
            int numeroPagina,

            Model model
    ) {

        PageRequest pageable = PageRequest.of(
                Math.max(numeroPagina, 0),
                20,
                Sort.by(
                        Sort.Order.desc("fecha"),
                        Sort.Order.desc("id")
                )
        );

        /*
         * Evitamos una búsqueda de fechas
         * invertidas.
         */
        if (
                fechaDesde != null
                        && fechaHasta != null
                        && fechaDesde.isAfter(fechaHasta)
        ) {

            model.addAttribute(
                    "errorFiltro",
                    "La fecha desde no puede ser posterior a la fecha hasta."
            );

            model.addAttribute(
                    "ventas",
                    java.util.List.of()
            );

            model.addAttribute(
                    "pagina",
                    Page.empty(pageable)
            );

            cargarFiltrosEnModelo(
                    model,
                    numero,
                    cliente,
                    fechaDesde,
                    fechaHasta,
                    estado,
                    formaPago
            );

            model.addAttribute(
                    "cantidadResultados",
                    0
            );

            return "ventas/lista";
        }

        Page<Venta> pagina =
                ventaFiltroService.buscar(
                        numero,
                        cliente,
                        fechaDesde,
                        fechaHasta,
                        estado,
                        formaPago,
                        pageable
                );

        model.addAttribute(
                "ventas",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        model.addAttribute(
                "cantidadResultados",
                pagina.getTotalElements()
        );

        cargarFiltrosEnModelo(
                model,
                numero,
                cliente,
                fechaDesde,
                fechaHasta,
                estado,
                formaPago
        );

        return "ventas/lista";
    }

    private void cargarFiltrosEnModelo(
            Model model,
            String numero,
            String cliente,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            String formaPago
    ) {

        model.addAttribute(
                "numeroFiltro",
                numero == null
                        ? ""
                        : numero
        );

        model.addAttribute(
                "clienteFiltro",
                cliente == null
                        ? ""
                        : cliente
        );

        model.addAttribute(
                "fechaDesdeFiltro",
                fechaDesde
        );

        model.addAttribute(
                "fechaHastaFiltro",
                fechaHasta
        );

        model.addAttribute(
                "estadoFiltro",
                estado == null
                        ? ""
                        : estado
        );

        model.addAttribute(
                "formaPagoFiltro",
                formaPago == null
                        ? ""
                        : formaPago
        );
    }
}
