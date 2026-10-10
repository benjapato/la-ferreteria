package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.dto.UsuarioFormularioDTO;
import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar(
            String buscar
    ) {

        if (buscar == null || buscar.isBlank()) {
            return usuarioRepository
                    .findAllByOrderByNombreCompletoAsc();
        }

        String texto = buscar.trim();

        return usuarioRepository
                .findByNombreCompletoContainingIgnoreCaseOrNombreUsuarioContainingIgnoreCaseOrderByNombreCompletoAsc(
                        texto,
                        texto
                );
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(
            Long id
    ) {

        return usuarioRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El usuario no existe"
                        )
                );
    }

    @Transactional(readOnly = true)
    public UsuarioFormularioDTO crearFormulario(
            Long id
    ) {

        Usuario usuario = buscarPorId(id);

        UsuarioFormularioDTO formulario =
                new UsuarioFormularioDTO();

        formulario.setId(usuario.getId());
        formulario.setNombreCompleto(
                usuario.getNombreCompleto()
        );
        formulario.setNombreUsuario(
                usuario.getNombreUsuario()
        );
        formulario.setContrasena("");
        formulario.setRol(usuario.getRol());
        formulario.setActivo(usuario.getActivo());

        return formulario;
    }

    @Transactional
    public Usuario guardar(
            UsuarioFormularioDTO formulario,
            String usuarioActual
    ) {

        validarFormulario(formulario);

        if (formulario.getId() == null) {
            return crear(formulario);
        }

        return actualizar(
                formulario,
                usuarioActual
        );
    }

    @Transactional
    public Usuario cambiarEstado(
            Long id,
            String usuarioActual
    ) {

        Usuario usuario = buscarPorId(id);

        boolean nuevoEstado =
                !Boolean.TRUE.equals(
                        usuario.getActivo()
                );

        if (
                usuario.getNombreUsuario()
                        .equalsIgnoreCase(usuarioActual)
                        && !nuevoEstado
        ) {
            throw new IllegalArgumentException(
                    "No podés desactivar tu propio usuario"
            );
        }

        validarQueNoSeQuiteUltimoAdministrador(
                usuario,
                usuario.getRol(),
                nuevoEstado
        );

        usuario.setActivo(nuevoEstado);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario crearAdministradorInicial(
            String nombreCompleto,
            String nombreUsuario,
            String contrasena
    ) {

        if (
                usuarioRepository.countByRolAndActivoTrue(
                        RolUsuario.ADMINISTRADOR
                ) > 0
        ) {
            return null;
        }

        validarContrasenaNueva(contrasena);

        Usuario usuario = new Usuario();

        usuario.setNombreCompleto(
                normalizarNombre(nombreCompleto)
        );
        usuario.setNombreUsuario(
                normalizarNombreUsuario(nombreUsuario)
        );
        usuario.setContrasena(
                passwordEncoder.encode(contrasena)
        );
        usuario.setRol(
                RolUsuario.ADMINISTRADOR
        );
        usuario.setActivo(true);

        return usuarioRepository.save(usuario);
    }

    private Usuario crear(
            UsuarioFormularioDTO formulario
    ) {

        String nombreUsuario =
                normalizarNombreUsuario(
                        formulario.getNombreUsuario()
                );

        if (
                usuarioRepository
                        .existsByNombreUsuarioIgnoreCase(
                                nombreUsuario
                        )
        ) {
            throw new IllegalArgumentException(
                    "El nombre de usuario ya está registrado"
            );
        }

        validarContrasenaNueva(
                formulario.getContrasena()
        );

        Usuario usuario = new Usuario();

        usuario.setNombreCompleto(
                normalizarNombre(
                        formulario.getNombreCompleto()
                )
        );
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setContrasena(
                passwordEncoder.encode(
                        formulario.getContrasena()
                )
        );
        usuario.setRol(formulario.getRol());
        usuario.setActivo(
                formulario.getActivo() == null
                        || formulario.getActivo()
        );

        return usuarioRepository.save(usuario);
    }

    private Usuario actualizar(
            UsuarioFormularioDTO formulario,
            String usuarioActual
    ) {

        Usuario usuario = buscarPorId(
                formulario.getId()
        );

        String nombreUsuario =
                normalizarNombreUsuario(
                        formulario.getNombreUsuario()
                );

        if (
                usuarioRepository
                        .existsByNombreUsuarioIgnoreCaseAndIdNot(
                                nombreUsuario,
                                usuario.getId()
                        )
        ) {
            throw new IllegalArgumentException(
                    "El nombre de usuario ya está registrado"
            );
        }

        boolean activoNuevo =
                Boolean.TRUE.equals(
                        formulario.getActivo()
                );

        if (
                usuario.getNombreUsuario()
                        .equalsIgnoreCase(usuarioActual)
                        && !activoNuevo
        ) {
            throw new IllegalArgumentException(
                    "No podés desactivar tu propio usuario"
            );
        }

        validarQueNoSeQuiteUltimoAdministrador(
                usuario,
                formulario.getRol(),
                activoNuevo
        );

        usuario.setNombreCompleto(
                normalizarNombre(
                        formulario.getNombreCompleto()
                )
        );
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setRol(formulario.getRol());
        usuario.setActivo(activoNuevo);

        if (
                formulario.getContrasena() != null
                        && !formulario
                        .getContrasena()
                        .isBlank()
        ) {
            validarContrasenaNueva(
                    formulario.getContrasena()
            );

            usuario.setContrasena(
                    passwordEncoder.encode(
                            formulario.getContrasena()
                    )
            );
        }

        return usuarioRepository.save(usuario);
    }

    private void validarFormulario(
            UsuarioFormularioDTO formulario
    ) {

        if (formulario == null) {
            throw new IllegalArgumentException(
                    "Los datos del usuario son obligatorios"
            );
        }

        if (
                formulario.getNombreCompleto() == null
                        || formulario
                        .getNombreCompleto()
                        .isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio"
            );
        }

        if (
                formulario.getNombreUsuario() == null
                        || formulario
                        .getNombreUsuario()
                        .isBlank()
        ) {
            throw new IllegalArgumentException(
                    "El nombre de usuario es obligatorio"
            );
        }

        if (formulario.getRol() == null) {
            throw new IllegalArgumentException(
                    "El rol es obligatorio"
            );
        }
    }

    private void validarContrasenaNueva(
            String contrasena
    ) {

        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException(
                    "La contraseña es obligatoria"
            );
        }

        if (contrasena.length() < 8) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }

        if (contrasena.length() > 100) {
            throw new IllegalArgumentException(
                    "La contraseña no puede superar los 100 caracteres"
            );
        }
    }

    private void validarQueNoSeQuiteUltimoAdministrador(
            Usuario usuarioActual,
            RolUsuario rolNuevo,
            boolean activoNuevo
    ) {

        boolean eraAdministradorActivo =
                usuarioActual.getRol()
                        == RolUsuario.ADMINISTRADOR
                        && Boolean.TRUE.equals(
                        usuarioActual.getActivo()
                );

        boolean seguiraAdministradorActivo =
                rolNuevo == RolUsuario.ADMINISTRADOR
                        && activoNuevo;

        if (
                eraAdministradorActivo
                        && !seguiraAdministradorActivo
                        && usuarioRepository
                        .countByRolAndActivoTrue(
                                RolUsuario.ADMINISTRADOR
                        ) <= 1
        ) {
            throw new IllegalArgumentException(
                    "Debe quedar al menos un administrador activo"
            );
        }
    }

    private String normalizarNombre(
            String nombre
    ) {

        return nombre.trim();
    }

    private String normalizarNombreUsuario(
            String nombreUsuario
    ) {

        String resultado = nombreUsuario
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!resultado.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "El usuario solo puede contener letras, números, punto, guion y guion bajo"
            );
        }

        return resultado;
    }
}
