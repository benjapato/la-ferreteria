package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.entity.DetalleVenta;
import com.LaFerreteria.LaFerreteria.entity.PagoVenta;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.service.ClienteService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.LaFerreteria.LaFerreteria.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final ClienteService clienteService;

    public VentaController(
            VentaService ventaService,
            ProductoService productoService,
            ClienteService clienteService
    ) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.clienteService = clienteService;
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

        Page<Venta> pagina = ventaService.listarTodas(
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
                "ventas",
                pagina.getContent()
        );

        model.addAttribute("pagina", pagina);

        return "ventas/lista";
    }

    @GetMapping("/nueva")
    public String mostrarFormulario(Model model) {

        Venta venta = new Venta();
        venta.setCliente(new Cliente());

        // La venta comienza con una fila de producto.
        venta.getDetalles().add(new DetalleVenta());
        asegurarPagosCombinados(venta);

        model.addAttribute("venta", venta);

        cargarOpcionesFormulario(model);

        return "ventas/formulario";
    }

    @GetMapping("/catalogos")
    @ResponseBody
    public CatalogosVentaRespuesta listarCatalogos() {

        List<ProductoCatalogoRespuesta> productos =
                productoService.listarActivos()
                        .stream()
                        .map(producto ->
                                new ProductoCatalogoRespuesta(
                                        producto.getId(),
                                        producto.getCodigo(),
                                        producto.getNombre(),
                                        producto.getStockActual(),
                                        producto.getPrecioVenta()
                                )
                        )
                        .toList();

        List<ClienteCatalogoRespuesta> clientes =
                clienteService.listarActivos()
                        .stream()
                        .map(cliente ->
                                new ClienteCatalogoRespuesta(
                                        cliente.getId(),
                                        cliente.getNombre(),
                                        cliente.getDocumento(),
                                        cliente.getSaldoCuentaCorriente()
                                )
                        )
                        .toList();

        return new CatalogosVentaRespuesta(
                productos,
                clientes
        );
    }

    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute("venta") Venta venta,
            BindingResult resultado,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if (resultado.hasErrors()) {

            asegurarFilaDetalle(venta);
            cargarOpcionesFormulario(model);

            model.addAttribute(
                    "error",
                    "Revisá los datos ingresados."
            );

            return "ventas/formulario";
        }

        try {

            Venta ventaGuardada =
                    ventaService.crear(venta);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Venta registrada correctamente"
            );

            return "redirect:/ventas/comprobante/"
                    + ventaGuardada.getId();

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );

            asegurarFilaDetalle(venta);
            cargarOpcionesFormulario(model);

            return "ventas/formulario";
        }
    }

    @PostMapping("/cancelar/{id}")
    public String cancelar(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {

        try {

            ventaService.cancelar(id);

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "La venta fue cancelada y el stock fue devuelto"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/ventas";
    }

    private void cargarOpcionesFormulario(Model model) {

        model.addAttribute(
                "productos",
                productoService.listarActivos()
        );

        model.addAttribute(
                "formasPago",
                FormaPago.values()
        );

        model.addAttribute(
                "clientes",
                clienteService.listarActivos()
        );

    }

    private void asegurarFilaDetalle(Venta venta) {

        if (venta.getCliente() == null) {
            venta.setCliente(new Cliente());
        }

        if (venta.getDetalles() == null
                || venta.getDetalles().isEmpty()) {

            venta.getDetalles().add(
                    new DetalleVenta()
            );
        }

        asegurarPagosCombinados(venta);
    }

    private void asegurarPagosCombinados(Venta venta) {

        Map<FormaPago, PagoVenta> pagosPorForma =
                new EnumMap<>(FormaPago.class);

        if (venta.getPagos() != null) {
            for (PagoVenta pago : venta.getPagos()) {
                if (pago != null && pago.getFormaPago() != null) {
                    pagosPorForma.putIfAbsent(
                            pago.getFormaPago(),
                            pago
                    );
                }
            }
        }

        List<PagoVenta> pagos = new ArrayList<>();

        for (FormaPago forma : List.of(
                FormaPago.EFECTIVO,
                FormaPago.TRANSFERENCIA,
                FormaPago.TARJETA_DEBITO,
                FormaPago.TARJETA_CREDITO
        )) {

            PagoVenta pago = pagosPorForma.getOrDefault(
                    forma,
                    new PagoVenta()
            );

            pago.setVenta(venta);
            pago.setFormaPago(forma);
            pagos.add(pago);
        }

        venta.setPagos(pagos);
    }

    public record CatalogosVentaRespuesta(
            List<ProductoCatalogoRespuesta> productos,
            List<ClienteCatalogoRespuesta> clientes
    ) {
    }

    public record ProductoCatalogoRespuesta(
            Long id,
            String codigo,
            String nombre,
            BigDecimal stock,
            BigDecimal precio
    ) {
    }

    public record ClienteCatalogoRespuesta(
            Long id,
            String nombre,
            String documento,
            BigDecimal saldo
    ) {
    }

    @GetMapping("/detalle/{id}")
    public String verDetalle(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            model.addAttribute(
                    "venta",
                    ventaService.buscarCompletaPorId(id)
            );

            return "ventas/detalle";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/ventas";
        }
    }
    @GetMapping("/comprobante/{id}")
    public String verComprobante(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        try {

            Venta venta = ventaService.buscarCompletaPorId(id);

            model.addAttribute("venta", venta);

            return "ventas/comprobante";

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );

            return "redirect:/ventas";
        }
    }
}
