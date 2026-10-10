package com.LaFerreteria.LaFerreteria.controller;

import com.LaFerreteria.LaFerreteria.service.CajaService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CajaModelAdvice {

    private final CajaService cajaService;

    public CajaModelAdvice(
            CajaService cajaService
    ) {
        this.cajaService = cajaService;
    }

    /*
     * Este dato estará disponible en todas
     * las páginas Thymeleaf.
     */
    @ModelAttribute
    public void agregarCajaAbierta(
            Authentication authentication,
            Model model
    ) {

        boolean autenticado =
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication
                        instanceof AnonymousAuthenticationToken);

        if (!autenticado) {
            model.addAttribute(
                    "cajaAbiertaGlobal",
                    null
            );
            return;
        }

        model.addAttribute(
                "cajaAbiertaGlobal",
                cajaService.buscarCajaAbierta()
                        .orElse(null)
        );
    }
}
