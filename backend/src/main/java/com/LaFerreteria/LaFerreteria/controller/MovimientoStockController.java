package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.service.MovimientoStockService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.LaFerreteria.LaFerreteria.entity.MovimientoStock;

@Controller
@RequestMapping("/movimientos-stock")
public class MovimientoStockController {

    private final MovimientoStockService movimientoStockService;
    private final ProductoService productoService;

    public MovimientoStockController(
            MovimientoStockService movimientoStockService,
            ProductoService productoService
    ) {
        this.movimientoStockService = movimientoStockService;
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "pagina",
                    defaultValue = "0"
            )
            int numeroPagina,
            Model model
    ) {

        Page<MovimientoStock> pagina =
                movimientoStockService.listarTodos(
                        PageRequest.of(
                                Math.max(numeroPagina, 0),
                                20,
                                Sort.by(
                                        Sort.Order.desc("fecha"),
                                        Sort.Order.desc("id")
                                )
                        )
                );

        model.addAttribute(
                "movimientos",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        return "movimientos-stock/lista";
    }

    @GetMapping("/ajuste")
    public String mostrarFormularioAjuste(Model model) {

        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );

        return "movimientos-stock/ajuste";
    }

    @PostMapping("/ajuste")
    public String guardarAjuste(
            @RequestParam Long productoId,
            @RequestParam TipoMovimientoStock tipo,
            @RequestParam BigDecimal cantidad,
            @RequestParam(required = false) String observacion,
            RedirectAttributes redirectAttributes
    ) {

        try {

            movimientoStockService.ajustarStock(
                    productoId,
                    tipo,
                    cantidad,
                    observacion
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El ajuste de stock fue registrado correctamente"
            );

            return "redirect:/movimientos-stock";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/movimientos-stock/ajuste";
        }
    }
}
