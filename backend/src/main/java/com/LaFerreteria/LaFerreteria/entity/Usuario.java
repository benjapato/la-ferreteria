package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_usuarios_nombre_usuario",
                        columnNames = "nombre_usuario"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "nombre_completo",
            nullable = false,
            length = 120
    )
    private String nombreCompleto;

    @Column(
            name = "nombre_usuario",
            nullable = false,
            length = 60
    )
    private String nombreUsuario;

    @Column(
            name = "contrasena",
            nullable = false,
            length = 255
    )
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "rol",
            nullable = false,
            length = 30
    )
    private RolUsuario rol;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(
            name = "fecha_creacion",
            nullable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(
            name = "fecha_actualizacion",
            nullable = false
    )
    private LocalDateTime fechaActualizacion;

    @PrePersist
    private void antesDeCrear() {

        LocalDateTime ahora = LocalDateTime.now();

        fechaCreacion = ahora;
        fechaActualizacion = ahora;

        if (activo == null) {
            activo = true;
        }
    }

    @PreUpdate
    private void antesDeActualizar() {

        fechaActualizacion = LocalDateTime.now();
    }
}
