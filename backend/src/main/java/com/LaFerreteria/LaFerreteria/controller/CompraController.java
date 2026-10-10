package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.entity.DetalleCompra;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/compras")
public class CompraController {

    private final CompraService compraService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;

    public CompraController(
            CompraService compraService,
            ProveedorService proveedorService,
            ProductoService productoService
    ) {
        this.compraService = compraService;
        this.proveedorService = proveedorService;
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

        Page<Compra> pagina = compraService.listarTodas(
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
                "compras",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        return "compras/lista";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {

        Compra compra = new Compra();

        // El formulario comienza con una fila de producto.
        compra.getDetalles().add(new DetalleCompra());

        model.addAttribute("compra", compra);

        cargarOpcionesFormulario(model);

        return "compras/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("compra") Compra compra,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {
            Compra compraGuardada = compraService.crear(compra);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "La compra #" + compraGuardada.getId()
                            + " fue registrada como pendiente"
            );

            return "redirect:/compras";

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            cargarOpcionesFormulario(model);

            return "compras/formulario";
        }
    }

    @PostMapping("/confirmar/{id}")
    public String confirmar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {
            compraService.confirmar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "La compra fue confirmada y el stock fue actualizado"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/compras";
    }

    @PostMapping("/cancelar/{id}")
    public String cancelar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {
            compraService.cancelar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "La compra fue cancelada"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/compras";
    }

    private void cargarOpcionesFormulario(Model model) {

        model.addAttribute(
                "proveedores",
                proveedorService.listarActivos()
        );

        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );
    }
    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            model.addAttribute(
                    "compra",
                    compraService.buscarCompletaPorId(id)
            );

            return "compras/detalle";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/compras";
        }
    }
}
