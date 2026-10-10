package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.controller.CajaController;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CajaControllerTests {

    @Test
    void aperturaExitosaDesdeInicioNoMuestraMensaje() {

        CajaService cajaService = mock(CajaService.class);
        CajaController controller = new CajaController(cajaService);
        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        String vista = controller.abrirCajaDesdeInicio(
                new BigDecimal("5000.00"),
                redirectAttributes
        );

        assertEquals("redirect:/inicio", vista);
        assertFalse(
                redirectAttributes.getFlashAttributes()
                        .containsKey("mensaje")
        );
        assertFalse(
                redirectAttributes.getFlashAttributes()
                        .containsKey("error")
        );
        verify(cajaService).abrirCaja(
                new BigDecimal("5000.00")
        );
    }

    @Test
    void errorDeAperturaDesdeInicioSigueSiendoVisible() {

        CajaService cajaService = mock(CajaService.class);
        CajaController controller = new CajaController(cajaService);
        RedirectAttributesModelMap redirectAttributes =
                new RedirectAttributesModelMap();

        doThrow(new IllegalArgumentException("La caja ya está abierta"))
                .when(cajaService)
                .abrirCaja(new BigDecimal("5000.00"));

        String vista = controller.abrirCajaDesdeInicio(
                new BigDecimal("5000.00"),
                redirectAttributes
        );

        assertEquals("redirect:/inicio", vista);
        assertEquals(
                "La caja ya está abierta",
                redirectAttributes.getFlashAttributes().get("error")
        );
        assertFalse(
                redirectAttributes.getFlashAttributes()
                        .containsKey("mensaje")
        );
    }
}
