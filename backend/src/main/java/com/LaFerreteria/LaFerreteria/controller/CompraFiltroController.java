package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.service.CompraFiltroService;
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
@RequestMapping("/compras")
public class CompraFiltroController {

    private final CompraFiltroService compraFiltroService;

    public CompraFiltroController(
            CompraFiltroService compraFiltroService
    ) {
        this.compraFiltroService =
                compraFiltroService;
    }

    @GetMapping("/buscar")
    public String buscar(
            @RequestParam(
                    name = "numero",
                    required = false
            )
            String numero,

            @RequestParam(
                    name = "proveedor",
                    required = false
            )
            String proveedor,

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
                    "compras",
                    java.util.List.of()
            );

            model.addAttribute(
                    "pagina",
                    Page.empty(pageable)
            );

            model.addAttribute(
                    "cantidadResultados",
                    0
            );

            cargarFiltrosEnModelo(
                    model,
                    numero,
                    proveedor,
                    fechaDesde,
                    fechaHasta,
                    estado
            );

            return "compras/lista";
        }

        Page<Compra> pagina =
                compraFiltroService.buscar(
                        numero,
                        proveedor,
                        fechaDesde,
                        fechaHasta,
                        estado,
                        pageable
                );

        model.addAttribute(
                "compras",
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
                proveedor,
                fechaDesde,
                fechaHasta,
                estado
        );

        return "compras/lista";
    }

    private void cargarFiltrosEnModelo(
            Model model,
            String numero,
            String proveedor,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado
    ) {

        model.addAttribute(
                "numeroFiltro",
                numero == null
                        ? ""
                        : numero
        );

        model.addAttribute(
                "proveedorFiltro",
                proveedor == null
                        ? ""
                        : proveedor
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
    }
}
