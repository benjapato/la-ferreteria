package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCaja;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/caja")
public class CajaController {

    private final CajaService cajaService;

    public CajaController(
            CajaService cajaService
    ) {
        this.cajaService = cajaService;
    }

    @GetMapping
    public String mostrarCaja(Model model) {

        Caja cajaAbierta = cajaService.buscarCajaAbierta()
                .orElse(null);

        model.addAttribute(
                "cajaAbierta",
                cajaAbierta
        );

        model.addAttribute(
                "movimientosCaja",
                cajaAbierta == null
                        ? java.util.List.of()
                        : cajaService.listarMovimientos(
                                cajaAbierta.getId()
                        )
        );

        model.addAttribute(
                "cajas",
                cajaService.listarTodas()
        );

        return "caja/index";
    }

    @PostMapping("/abrir")
    public String abrirCaja(
            @RequestParam(
                    name = "montoInicial",
                    defaultValue = "0"
            ) BigDecimal montoInicial,
            RedirectAttributes redirectAttributes
    ) {

        try {

            cajaService.abrirCaja(montoInicial);

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/caja";
    }

    @PostMapping("/cerrar")
    public String cerrarCaja(
            @RequestParam BigDecimal efectivoContado,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Caja caja = cajaService.cerrarCaja(
                    efectivoContado
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeCierre(caja)
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/caja";
    }

    @PostMapping("/movimiento")
    public String registrarMovimiento(
            @RequestParam TipoMovimientoCaja tipo,
            @RequestParam BigDecimal monto,
            @RequestParam String motivo,
            RedirectAttributes redirectAttributes
    ) {

        try {

            cajaService.registrarMovimiento(
                    tipo,
                    monto,
                    motivo
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Movimiento de efectivo registrado"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/caja";
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Caja caja = cajaService.buscarPorId(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "No existe la caja con ID " + id
                            )
                    );

            model.addAttribute("caja", caja);
            model.addAttribute(
                    "movimientosCaja",
                    cajaService.listarMovimientos(caja.getId())
            );

            return "caja/detalle";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/caja";
        }
    }
    @PostMapping("/abrir/inicio")
    public String abrirCajaDesdeInicio(
            @RequestParam(
                    name = "montoInicial",
                    defaultValue = "0"
            ) BigDecimal montoInicial,
            RedirectAttributes redirectAttributes
    ) {

        try {

            cajaService.abrirCaja(montoInicial);

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/inicio";
    }

    @PostMapping("/cerrar/inicio")
    public String cerrarCajaDesdeInicio(
            @RequestParam BigDecimal efectivoContado,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Caja caja = cajaService.cerrarCaja(
                    efectivoContado
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensajeCierre(caja)
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/inicio";
    }

    private String mensajeCierre(
            Caja caja
    ) {

        return "Caja #" + caja.getId()
                + " cerrada. Efectivo esperado: $"
                + caja.getEfectivoEsperado()
                + ", contado: $"
                + caja.getEfectivoContado()
                + ", diferencia: $"
                + caja.getDiferenciaEfectivo();
    }

}
