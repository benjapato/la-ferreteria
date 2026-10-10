package com.LaFerreteria.LaFerreteria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "clientes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cliente_documento",
                        columnNames = "documento"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(
            message = "El nombre del cliente es obligatorio"
    )
    @Size(
            max = 150,
            message = "El nombre no puede superar los 150 caracteres"
    )
    @Column(
            nullable = false,
            length = 150
    )
    private String nombre;

    /*
     * Puede ser DNI, CUIT o CUIL.
     * Es opcional, pero no puede repetirse.
     */
    @Size(
            max = 30,
            message = "El documento no puede superar los 30 caracteres"
    )
    @Column(
            length = 30,
            unique = true
    )
    private String documento;

    @Size(
            max = 50,
            message = "El teléfono no puede superar los 50 caracteres"
    )
    @Column(length = 50)
    private String telefono;

    @Email(
            message = "El correo electrónico no tiene un formato válido"
    )
    @Size(
            max = 150,
            message = "El correo no puede superar los 150 caracteres"
    )
    @Column(length = 150)
    private String email;

    @Size(
            max = 250,
            message = "La dirección no puede superar los 250 caracteres"
    )
    @Column(length = 250)
    private String direccion;

    /*
     * Saldo positivo:
     * el cliente debe dinero a la ferretería.
     *
     * Saldo cero:
     * la cuenta está al día.
     *
     * Saldo negativo:
     * el cliente tiene dinero a favor.
     */
    @Column(
            name = "saldo_cuenta_corriente",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saldoCuentaCorriente =
            BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean activo = true;
}