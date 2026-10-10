package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.service.ClienteService;
import com.LaFerreteria.LaFerreteria.service.MovimientoCuentaCorrienteService;
import com.LaFerreteria.LaFerreteria.service.PagoClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class CuentaCorrienteController {

    private final ClienteService clienteService;
    private final PagoClienteService pagoClienteService;
    private final MovimientoCuentaCorrienteService
            movimientoCuentaCorrienteService;

    public CuentaCorrienteController(
            ClienteService clienteService,
            PagoClienteService pagoClienteService,
            MovimientoCuentaCorrienteService
                    movimientoCuentaCorrienteService
    ) {
        this.clienteService = clienteService;
        this.pagoClienteService = pagoClienteService;
        this.movimientoCuentaCorrienteService =
                movimientoCuentaCorrienteService;
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cliente cliente = buscarCliente(id);

            model.addAttribute(
                    "cliente",
                    cliente
            );

            model.addAttribute(
                    "movimientos",
                    movimientoCuentaCorrienteService
                            .listarPorCliente(id)
            );

            return "clientes/detalle";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/clientes";
        }
    }

    @GetMapping("/{id}/pago")
    public String mostrarFormularioPago(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cliente cliente = buscarCliente(id);

            model.addAttribute(
                    "cliente",
                    cliente
            );

            model.addAttribute(
                    "pago",
                    new PagoCliente()
            );

            model.addAttribute(
                    "mediosPago",
                    MedioPago.values()
            );

            return "clientes/pago";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/clientes";
        }
    }

    @PostMapping("/{id}/pago")
    public String registrarPago(
            @PathVariable Long id,
            @ModelAttribute("pago") PagoCliente pago,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (resultado.hasErrors()) {

            model.addAttribute(
                    "error",
                    "Revisá el monto y el medio de pago"
            );

            cargarFormularioPago(id, model);

            return "clientes/pago";
        }

        try {

            PagoCliente pagoGuardado =
                    pagoClienteService.registrarPago(
                            id,
                            pago
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Pago registrado por $"
                            + pagoGuardado.getMonto()
            );

            return "redirect:/clientes/detalle/" + id;

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            cargarFormularioPago(id, model);

            return "clientes/pago";
        }
    }

    /*
     * Anula un pago cargado por error.
     */
    @PostMapping("/{clienteId}/pagos/{pagoId}/anular")
    public String anularPago(
            @PathVariable Long clienteId,
            @PathVariable Long pagoId,
            @RequestParam(name = "motivo") String motivo,
            RedirectAttributes redirectAttributes
    ) {

        try {

            PagoCliente pagoAnulado =
                    pagoClienteService.anularPago(
                            clienteId,
                            pagoId,
                            motivo
                    );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "El pago #"
                            + pagoAnulado.getId()
                            + " fue anulado correctamente"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/clientes/detalle/" + clienteId;
    }

    private Cliente buscarCliente(Long id) {

        return clienteService.buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el cliente con ID " + id
                        )
                );
    }

    private void cargarFormularioPago(
            Long clienteId,
            Model model
    ) {

        model.addAttribute(
                "cliente",
                buscarCliente(clienteId)
        );

        model.addAttribute(
                "mediosPago",
                MedioPago.values()
        );
    }
}