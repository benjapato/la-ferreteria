package com.LaFerreteria.LaFerreteria.dto;

import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioFormularioDTO {

    private Long id;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(
            max = 120,
            message = "El nombre completo no puede superar los 120 caracteres"
    )
    private String nombreCompleto;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(
            min = 3,
            max = 60,
            message = "El usuario debe tener entre 3 y 60 caracteres"
    )
    private String nombreUsuario;

    @Size(
            max = 100,
            message = "La contraseña no puede superar los 100 caracteres"
    )
    private String contrasena;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    private Boolean activo = true;
}
