package com.LaFerreteria.LaFerreteria.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReactAppController {

    @GetMapping({
            "/app",
            "/app/",
            "/app/login",
            "/app/ventas",
            "/app/ventas/nueva",
            "/app/productos",
            "/app/clientes",
            "/app/clientes/{id}",
            "/app/compras",
            "/app/compras/nueva",
            "/app/reportes",
            "/app/usuarios",
            "/app/copias-seguridad",
            "/app/caja",
            "/app/movimientos-stock"
    })
    public String reactApp() {
        return "forward:/app/index.html";
    }
}
