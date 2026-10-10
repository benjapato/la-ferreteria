package com.LaFerreteria.LaFerreteria.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AutenticacionController {

    @GetMapping("/login")
    public String mostrarLogin(
            Authentication authentication
    ) {

        if (
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication
                        instanceof AnonymousAuthenticationToken)
        ) {
            return "redirect:/";
        }

        return "login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {

        return "acceso-denegado";
    }
}
