package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioActualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioActualService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerUsuarioActual() {

        Authentication autenticacion =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || "anonymousUser".equals(
                        autenticacion.getPrincipal()
                )) {

            throw new IllegalArgumentException(
                    "No hay un usuario autenticado"
            );
        }

        return usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        autenticacion.getName()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario autenticado no existe en la base de datos"
                        )
                );
    }
}
