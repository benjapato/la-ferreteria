package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class CierreAutomaticoCajaService implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            CierreAutomaticoCajaService.class
    );

    private final CajaService cajaService;
    private final boolean habilitado;
    private final LocalTime horaCierre;
    private final ZoneId zona;

    public CierreAutomaticoCajaService(
            CajaService cajaService,
            @Value("${app.caja.cierre-automatico.habilitado:true}") boolean habilitado,
            @Value("${app.caja.cierre-automatico.hora:23:00}") String horaCierre,
            @Value("${app.caja.cierre-automatico.zona:America/Argentina/Buenos_Aires}") String zona
    ) {
        this.cajaService = cajaService;
        this.habilitado = habilitado;
        this.horaCierre = LocalTime.parse(horaCierre);
        this.zona = ZoneId.of(zona);
    }

    @Override
    public void run(ApplicationArguments args) {
        verificarCierrePendiente();
    }

    @Scheduled(
            cron = "${app.caja.cierre-automatico.verificacion-cron:0 * * * * *}",
            zone = "${app.caja.cierre-automatico.zona:America/Argentina/Buenos_Aires}"
    )
    public void verificarCierrePendiente() {
        if (!habilitado) {
            return;
        }

        cajaService.cerrarCajaAutomaticamenteSiCorresponde(
                        LocalDateTime.now(zona),
                        horaCierre
                )
                .map(Caja::getId)
                .ifPresent(id -> LOGGER.info(
                        "Caja {} cerrada automaticamente; arqueo pendiente",
                        id
                ));
    }
}
