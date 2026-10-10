package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute(
                "categorias",
                categoriaService.listarTodas()
        );

        return "categorias/lista";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {
        model.addAttribute("categoria", new Categoria());

        return "categorias/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult resultado,
            Model model
    ) {

        if (resultado.hasErrors()) {
            return "categorias/formulario";
        }

        try {
            categoriaService.guardar(categoria);
            return "redirect:/categorias";

        } catch (IllegalArgumentException excepcion) {
            model.addAttribute("error", excepcion.getMessage());
            return "categorias/formulario";
        }
    }
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model
    ) {

        Categoria categoria = categoriaService.buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la categoría con ID " + id
                        )
                );

        model.addAttribute("categoria", categoria);

        return "categorias/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult resultado,
            Model model
    ) {

        if (resultado.hasErrors()) {
            return "categorias/formulario";
        }

        try {
            categoriaService.actualizar(id, categoria);
            return "redirect:/categorias";

        } catch (IllegalArgumentException excepcion) {
            model.addAttribute("error", excepcion.getMessage());
            return "categorias/formulario";
        }
    }
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {

        categoriaService.cambiarEstado(id);

        return "redirect:/categorias";
    }
}