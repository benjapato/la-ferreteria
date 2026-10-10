package com.LaFerreteria.LaFerreteria.config;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArqueoCajaInicializador
        implements ApplicationRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ArqueoCajaInicializador.class
            );

    private final CajaRepository cajaRepository;

    public ArqueoCajaInicializador(
            CajaRepository cajaRepository
    ) {
        this.cajaRepository = cajaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        List<Caja> cajas = cajaRepository.findAll();
        List<Caja> cajasActualizadas = new ArrayList<>();

        for (Caja caja : cajas) {

            boolean requiereInicializacion =
                    caja.getMontoInicial() == null
                            || caja.getIngresosEfectivo() == null
                            || caja.getRetirosEfectivo() == null
                            || caja.getEfectivoEsperado() == null;

            if (!requiereInicializacion) {
                continue;
            }

            caja.setMontoInicial(
                    valor(caja.getMontoInicial())
            );
            caja.setIngresosEfectivo(
                    valor(caja.getIngresosEfectivo())
            );
            caja.setRetirosEfectivo(
                    valor(caja.getRetirosEfectivo())
            );
            caja.setEfectivoEsperado(
                    valor(caja.getMontoInicial())
                            .add(valor(caja.getVentasEfectivo()))
                            .add(valor(
                                    caja.getPagosClientesEfectivo()
                            ))
                            .add(valor(
                                    caja.getIngresosEfectivo()
                            ))
                            .subtract(valor(
                                    caja.getRetirosEfectivo()
                            ))
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
            );

            cajasActualizadas.add(caja);
        }

        if (!cajasActualizadas.isEmpty()) {
            cajaRepository.saveAll(cajasActualizadas);
            LOGGER.info(
                    "Arqueo inicializado para {} cajas existentes",
                    cajasActualizadas.size()
            );
        }
    }

    private BigDecimal valor(
            BigDecimal numero
    ) {

        return (numero == null ? BigDecimal.ZERO : numero)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
