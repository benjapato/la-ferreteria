package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.dto.UsuarioFormularioDTO;
import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import com.LaFerreteria.LaFerreteria.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(
            @RequestParam(
                    name = "buscar",
                    required = false
            )
            String buscar,
            Model model
    ) {

        model.addAttribute(
                "usuarios",
                usuarioService.listar(buscar)
        );

        model.addAttribute(
                "buscar",
                buscar == null ? "" : buscar
        );

        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(
            Model model
    ) {

        UsuarioFormularioDTO formulario =
                new UsuarioFormularioDTO();

        formulario.setRol(
                RolUsuario.VENDEDOR
        );
        formulario.setActivo(true);

        cargarFormulario(
                model,
                formulario,
                false
        );

        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            cargarFormulario(
                    model,
                    usuarioService
                            .crearFormulario(id),
                    true
            );

            return "usuarios/formulario";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/usuarios";
        }
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid
            @ModelAttribute("usuarioFormulario")
            UsuarioFormularioDTO formulario,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        boolean edicion =
                formulario.getId() != null;

        if (bindingResult.hasErrors()) {
            cargarFormulario(
                    model,
                    formulario,
                    edicion
            );

            return "usuarios/formulario";
        }

        try {
            usuarioService.guardar(
                    formulario,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    edicion
                            ? "Usuario actualizado correctamente"
                            : "Usuario creado correctamente"
            );

            return "redirect:/usuarios";

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            cargarFormulario(
                    model,
                    formulario,
                    edicion
            );

            return "usuarios/formulario";
        }
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {
            usuarioService.cambiarEstado(
                    id,
                    authentication.getName()
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Estado del usuario actualizado"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/usuarios";
    }

    private void cargarFormulario(
            Model model,
            UsuarioFormularioDTO formulario,
            boolean edicion
    ) {

        model.addAttribute(
                "usuarioFormulario",
                formulario
        );

        model.addAttribute(
                "roles",
                RolUsuario.values()
        );

        model.addAttribute(
                "edicion",
                edicion
        );
    }
}
