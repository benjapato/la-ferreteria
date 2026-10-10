package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "proveedores",
                proveedorService.listarTodos()
        );

        return "proveedores/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "proveedor",
                new Proveedor()
        );

        return "proveedores/formulario";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model
    ) {

        Proveedor proveedor = proveedorService.buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el proveedor con ID " + id
                        )
                );

        model.addAttribute(
                "proveedor",
                proveedor
        );

        return "proveedores/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("proveedor") Proveedor proveedor,
            BindingResult resultado,
            Model model
    ) {

        if (resultado.hasErrors()) {
            return "proveedores/formulario";
        }

        try {

            if (proveedor.getId() == null) {
                proveedorService.guardar(proveedor);
            } else {
                proveedorService.actualizar(
                        proveedor.getId(),
                        proveedor
                );
            }

            return "redirect:/proveedores";

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "proveedores/formulario";
        }
    }

    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {

        proveedorService.cambiarEstado(id);

        return "redirect:/proveedores";
    }
}