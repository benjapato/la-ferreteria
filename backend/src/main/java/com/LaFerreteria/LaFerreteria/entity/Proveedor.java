package com.LaFerreteria.LaFerreteria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(
            max = 150,
            message = "El nombre no puede superar los 150 caracteres"
    )
    @Column(nullable = false, length = 150)
    private String nombre;

    @Size(
            max = 150,
            message = "La razón social no puede superar los 150 caracteres"
    )
    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Size(
            max = 13,
            message = "El CUIT no puede superar los 13 caracteres"
    )
    @Column(unique = true, length = 13)
    private String cuit;

    @Size(
            max = 30,
            message = "El teléfono no puede superar los 30 caracteres"
    )
    @Column(length = 30)
    private String telefono;

    @Email(message = "El correo electrónico no es válido")
    @Size(
            max = 150,
            message = "El correo no puede superar los 150 caracteres"
    )
    @Column(length = 150)
    private String email;

    @Size(
            max = 255,
            message = "La dirección no puede superar los 255 caracteres"
    )
    @Column(length = 255)
    private String direccion;

    @Column(nullable = false)
    private Boolean activo = true;
}
