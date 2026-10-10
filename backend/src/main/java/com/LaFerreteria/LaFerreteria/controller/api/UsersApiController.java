package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.dto.UsuarioFormularioDTO;
import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import com.LaFerreteria.LaFerreteria.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UsersApiController {

    private final UsuarioService usuarioService;

    public UsersApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public UsuariosRespuesta listar(
            @RequestParam(defaultValue = "") String search
    ) {
        List<Usuario> usuarios = usuarioService.listar(search);
        List<Usuario> todos = usuarioService.listar(null);
        long activos = todos.stream()
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .count();
        long administradores = todos.stream()
                .filter(usuario -> usuario.getRol() == RolUsuario.ADMINISTRADOR)
                .count();

        return new UsuariosRespuesta(
                usuarios.stream().map(this::resumir).toList(),
                todos.size(),
                activos,
                todos.size() - activos,
                administradores
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta crear(
            @Valid @RequestBody UsuarioFormularioDTO formulario,
            Authentication authentication
    ) {
        formulario.setId(null);
        return resumir(usuarioService.guardar(
                formulario,
                authentication.getName()
        ));
    }

    @PutMapping("/{id}")
    public UsuarioRespuesta actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioFormularioDTO formulario,
            Authentication authentication
    ) {
        formulario.setId(id);
        return resumir(usuarioService.guardar(
                formulario,
                authentication.getName()
        ));
    }

    @PostMapping("/{id}/toggle")
    public UsuarioRespuesta cambiarEstado(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return resumir(usuarioService.cambiarEstado(
                id,
                authentication.getName()
        ));
    }

    private UsuarioRespuesta resumir(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getNombreUsuario(),
                usuario.getRol().name(),
                Boolean.TRUE.equals(usuario.getActivo()),
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion()
        );
    }

    public record UsuariosRespuesta(
            List<UsuarioRespuesta> usuarios,
            long total,
            long activos,
            long inactivos,
            long administradores
    ) {
    }

    public record UsuarioRespuesta(
            Long id,
            String nombreCompleto,
            String nombreUsuario,
            String rol,
            boolean activo,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {
    }
}
