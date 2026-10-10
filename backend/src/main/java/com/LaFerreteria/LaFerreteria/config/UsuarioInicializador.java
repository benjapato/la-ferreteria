package com.LaFerreteria.LaFerreteria.config;

import com.LaFerreteria.LaFerreteria.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class UsuarioInicializador
        implements ApplicationRunner {

    private final UsuarioService usuarioService;

    @Value("${app.usuario-inicial.nombre}")
    private String nombre;

    @Value("${app.usuario-inicial.usuario}")
    private String nombreUsuario;

    @Value("${app.usuario-inicial.contrasena}")
    private String contrasena;

    public UsuarioInicializador(
            UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(
            ApplicationArguments args
    ) {

        usuarioService.crearAdministradorInicial(
                nombre,
                nombreUsuario,
                contrasena
        );
    }
}
