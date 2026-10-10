package com.LaFerreteria.LaFerreteria.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UsuarioModelAdvice {

    @ModelAttribute
    public void cargarUsuarioActual(
            Authentication authentication,
            Model model
    ) {

        boolean autenticado =
                authentication != null
                        && authentication.isAuthenticated()
                        && !(authentication
                        instanceof AnonymousAuthenticationToken);

        model.addAttribute(
                "usuarioAutenticado",
                autenticado
        );

        if (!autenticado) {
            model.addAttribute(
                    "esAdministrador",
                    false
            );
            return;
        }

        boolean esAdministrador =
                authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(autoridad ->
                                autoridad
                                        .getAuthority()
                                        .equals(
                                                "ROLE_ADMINISTRADOR"
                                        )
                        );

        model.addAttribute(
                "nombreUsuarioActual",
                authentication.getName()
        );

        model.addAttribute(
                "esAdministrador",
                esAdministrador
        );

        model.addAttribute(
                "rolUsuarioActual",
                esAdministrador
                        ? "Administrador"
                        : "Vendedor"
        );
    }
}
