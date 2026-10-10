package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.service.CategoriaService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(
            ProductoService productoService,
            CategoriaService categoriaService
    ) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
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

        Page<Producto> pagina = productoService.buscar(
                buscar,
                PageRequest.of(
                        Math.max(numeroPagina, 0),
                        20,
                        Sort.by("nombre").ascending()
                )
        );

        model.addAttribute(
                "productos",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        model.addAttribute(
                "buscar",
                buscar == null ? "" : buscar
        );

        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute("producto", new Producto());

        cargarOpcionesFormulario(model);

        return "productos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @Valid @ModelAttribute("producto") Producto producto,
            BindingResult resultado,
            Model model
    ) {

        if (resultado.hasErrors()) {
            cargarOpcionesFormulario(model);
            return "productos/formulario";
        }

        try {

            if (producto.getId() == null) {
                productoService.guardar(producto);
            } else {
                productoService.actualizar(
                        producto.getId(),
                        producto
                );
            }

            return "redirect:/productos";

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            cargarOpcionesFormulario(model);

            return "productos/formulario";
        }
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Model model
    ) {

        Producto producto = productoService.buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el producto con ID " + id
                        )
                );

        model.addAttribute("producto", producto);

        cargarOpcionesFormulario(model);

        return "productos/formulario";
    }


    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstado(@PathVariable Long id) {

        productoService.cambiarEstado(id);

        return "redirect:/productos";
    }

    private void cargarOpcionesFormulario(Model model) {

        model.addAttribute(
                "categorias",
                categoriaService.listarActivas()
        );

        model.addAttribute(
                "unidadesMedida",
                UnidadMedida.values()
        );
    }
    @GetMapping("/stock-bajo")
    public String mostrarStockBajo(Model model) {

        model.addAttribute(
                "productos",
                productoService.listarConStockBajo()
        );

        return "productos/stock-bajo";
    }
}
