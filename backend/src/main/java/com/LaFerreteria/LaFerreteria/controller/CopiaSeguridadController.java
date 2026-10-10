package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.dto.CopiaSeguridadDTO;
import com.LaFerreteria.LaFerreteria.service.CopiaSeguridadService;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequestMapping("/copias-seguridad")
public class CopiaSeguridadController {

    private final CopiaSeguridadService
            copiaSeguridadService;

    @Value("${app.copias-seguridad.automaticas.habilitadas:true}")
    private boolean copiasAutomaticasHabilitadas;

    public CopiaSeguridadController(
            CopiaSeguridadService copiaSeguridadService
    ) {
        this.copiaSeguridadService =
                copiaSeguridadService;
    }

    @GetMapping
    public String mostrarCopias(
            Model model
    ) {

        try {

            List<CopiaSeguridadDTO> copias =
                    copiaSeguridadService
                            .listarCopias();

            Path directorio =
                    copiaSeguridadService
                            .obtenerDirectorioCopias();

            model.addAttribute(
                    "copias",
                    copias
            );

            model.addAttribute(
                    "directorioCopias",
                    directorio.toString()
            );

            model.addAttribute(
                    "copiasAutomaticasHabilitadas",
                    copiasAutomaticasHabilitadas
            );

        } catch (IllegalArgumentException excepcion) {

            model.addAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "copias-seguridad/lista";
    }

    @PostMapping("/crear")
    public String crearCopia(
            RedirectAttributes redirectAttributes
    ) {

        try {

            CopiaSeguridadDTO copia =
                    copiaSeguridadService
                            .crearCopia();

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Copia creada correctamente: "
                            + copia.nombre()
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/copias-seguridad";
    }

    @GetMapping("/descargar/{nombreArchivo}")
    public ResponseEntity<Resource> descargarCopia(
            @PathVariable String nombreArchivo
    ) {

        Resource recurso =
                copiaSeguridadService
                        .obtenerArchivo(
                                nombreArchivo
                        );

        long tamanio =
                copiaSeguridadService
                        .obtenerTamanio(
                                nombreArchivo
                        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(
                                nombreArchivo,
                                StandardCharsets.UTF_8
                        )
                        .build()
        );

        headers.setContentType(
                MediaType.APPLICATION_OCTET_STREAM
        );

        headers.setContentLength(
                tamanio
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(recurso);
    }

    @PostMapping("/eliminar/{nombreArchivo}")
    public String eliminarCopia(
            @PathVariable String nombreArchivo,
            RedirectAttributes redirectAttributes
    ) {

        try {

            copiaSeguridadService.eliminarCopia(
                    nombreArchivo
            );

            redirectAttributes.addFlashAttribute(
                    "mensaje",
                    "Copia eliminada correctamente"
            );

        } catch (IllegalArgumentException excepcion) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    excepcion.getMessage()
            );
        }

        return "redirect:/copias-seguridad";
    }
}
