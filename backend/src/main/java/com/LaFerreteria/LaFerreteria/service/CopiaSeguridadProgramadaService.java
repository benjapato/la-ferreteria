package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.dto.CopiaSeguridadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "app.copias-seguridad.automaticas",
        name = "habilitadas",
        havingValue = "true",
        matchIfMissing = true
)
public class CopiaSeguridadProgramadaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            CopiaSeguridadProgramadaService.class
    );

    private final CopiaSeguridadService copiaSeguridadService;

    public CopiaSeguridadProgramadaService(
            CopiaSeguridadService copiaSeguridadService
    ) {
        this.copiaSeguridadService = copiaSeguridadService;
    }

    @Scheduled(
            cron = "${app.copias-seguridad.automaticas.cron:0 0 18 * * *}",
            zone = "${app.copias-seguridad.automaticas.zona:America/Argentina/Buenos_Aires}"
    )
    public void crearCopiaDiaria() {

        try {
            CopiaSeguridadDTO copia =
                    copiaSeguridadService.crearCopia();

            LOGGER.info(
                    "Copia de seguridad automatica creada: {}",
                    copia.nombre()
            );

        } catch (RuntimeException excepcion) {
            LOGGER.error(
                    "No se pudo crear la copia de seguridad automatica: {}",
                    excepcion.getMessage(),
                    excepcion
            );
        }
    }
}
