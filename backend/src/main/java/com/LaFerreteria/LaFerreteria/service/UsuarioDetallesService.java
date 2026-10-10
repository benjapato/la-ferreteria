package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioDetallesService
        implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(
            String nombreUsuario
    ) throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository
                .findByNombreUsuarioIgnoreCase(
                        nombreUsuario
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario o contraseña incorrectos"
                        )
                );

        return User
                .withUsername(
                        usuario.getNombreUsuario()
                )
                .password(
                        usuario.getContrasena()
                )
                .roles(
                        usuario.getRol().name()
                )
                .disabled(
                        !Boolean.TRUE.equals(
                                usuario.getActivo()
                        )
                )
                .build();
    }
}
