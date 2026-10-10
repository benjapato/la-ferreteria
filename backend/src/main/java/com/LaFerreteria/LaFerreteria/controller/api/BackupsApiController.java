package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.dto.CopiaSeguridadDTO;
import com.LaFerreteria.LaFerreteria.service.CopiaSeguridadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/backups")
public class BackupsApiController {

    private final CopiaSeguridadService copiaSeguridadService;
    private final boolean automaticasHabilitadas;
    private final String cron;
    private final String zona;

    public BackupsApiController(
            CopiaSeguridadService copiaSeguridadService,
            @Value("${app.copias-seguridad.automaticas.habilitadas:true}")
            boolean automaticasHabilitadas,
            @Value("${app.copias-seguridad.automaticas.cron:0 0 18 * * *}")
            String cron,
            @Value("${app.copias-seguridad.automaticas.zona:America/Argentina/Buenos_Aires}")
            String zona
    ) {
        this.copiaSeguridadService = copiaSeguridadService;
        this.automaticasHabilitadas = automaticasHabilitadas;
        this.cron = cron;
        this.zona = zona;
    }

    @GetMapping
    public CopiasRespuesta listar() {
        List<CopiaSeguridadDTO> copias = copiaSeguridadService.listarCopias();
        return new CopiasRespuesta(
                copias.stream().map(this::resumir).toList(),
                copiaSeguridadService.obtenerDirectorioCopias().toString(),
                automaticasHabilitadas,
                cron,
                zona,
                copias.stream().mapToLong(CopiaSeguridadDTO::tamanioBytes).sum()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CopiaRespuesta crear() {
        return resumir(copiaSeguridadService.crearCopia());
    }

    @DeleteMapping("/{nombreArchivo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String nombreArchivo) {
        copiaSeguridadService.eliminarCopia(nombreArchivo);
    }

    @GetMapping("/{nombreArchivo}/download")
    public ResponseEntity<Resource> descargar(
            @PathVariable String nombreArchivo
    ) {
        Resource recurso = copiaSeguridadService.obtenerArchivo(nombreArchivo);
        long tamanio = copiaSeguridadService.obtenerTamanio(nombreArchivo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(nombreArchivo, StandardCharsets.UTF_8)
                        .build()
        );
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(tamanio);

        return ResponseEntity.ok().headers(headers).body(recurso);
    }

    private CopiaRespuesta resumir(CopiaSeguridadDTO copia) {
        return new CopiaRespuesta(
                copia.nombre(),
                copia.tamanioBytes(),
                copia.tamanioLegible(),
                copia.fechaModificacion()
        );
    }

    public record CopiasRespuesta(
            List<CopiaRespuesta> copias,
            String directorio,
            boolean automaticasHabilitadas,
            String cron,
            String zona,
            long tamanioTotalBytes
    ) {
    }

    public record CopiaRespuesta(
            String nombre,
            long tamanioBytes,
            String tamanioLegible,
            LocalDateTime fechaModificacion
    ) {
    }
}
