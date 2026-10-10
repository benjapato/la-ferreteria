package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(
            ClienteService clienteService
    ) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "buscar",
                    required = false
            )
            String buscar,

            @RequestParam(
                    name = "pagina",
                    defaultValue = "0"
            )
            int numeroPagina,

            Model model
    ) {

        Page<Cliente> pagina = clienteService.buscar(
                buscar,
                PageRequest.of(
                        Math.max(numeroPagina, 0),
                        20,
                        Sort.by("nombre").ascending()
                )
        );

        model.addAttribute(
                "clientes",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        model.addAttribute(
                "buscar",
                buscar == null ? "" : buscar
        );

        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Model model) {

        model.addAttribute(
                "cliente",
                new Cliente()
        );

        return "clientes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        return clienteService.buscarPorId(id)
                .map(cliente -> {

                    model.addAttribute(
                            "cliente",
                            cliente
                    );

                    return "clientes/formulario";
                })
                .orElseGet(() -> {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "El cliente seleccionado no existe"
                    );

                    return "redirect:/clientes";
                });
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid
            @ModelAttribute("cliente") Cliente cliente,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (resultado.hasErrors()) {

            model.addAttribute(
                    "error",
                    "Revisá los datos ingresados"
            );

            return "clientes/formulario";
        }

        try {

            boolean nuevo = cliente.getId() == null;

            clienteService.guardar(cliente);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    nuevo
                            ? "El cliente fue registrado correctamente"
                            : "El cliente fue actualizado correctamente"
            );

            return "redirect:/clientes";

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "clientes/formulario";
        }
    }

    @PostMapping("/estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Cliente cliente =
                    clienteService.cambiarEstado(id);

            String mensaje =
                    Boolean.TRUE.equals(cliente.getActivo())
                            ? "El cliente fue activado"
                            : "El cliente fue desactivado";

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    mensaje
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/clientes";
    }
}
