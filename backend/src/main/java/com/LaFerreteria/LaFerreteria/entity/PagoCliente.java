package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_clientes")
@Getter
@Setter
@NoArgsConstructor
public class PagoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "cliente_id",
            nullable = false
    )
    private Cliente cliente;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El monto debe ser mayor que cero"
    )
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal monto;

    @NotNull(message = "El medio de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(
            name = "medio_pago",
            nullable = false,
            length = 30
    )
    private MedioPago medioPago;

    @Column(
            name = "saldo_anterior",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saldoAnterior;

    @Column(
            name = "saldo_posterior",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal saldoPosterior;

    @Size(
            max = 300,
            message = "La observación no puede superar los 300 caracteres"
    )
    @Column(length = 300)
    private String observacion;

    /*
     * Caja en la que se recibió el pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;

    /*
     * Se usa Boolean en lugar de boolean para que los pagos
     * que ya existían antes de esta modificación puedan quedar
     * con null. En el servicio, null se interpreta como no anulado.
     */
    @Column(name = "anulado")
    private Boolean anulado = false;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Size(
            max = 300,
            message = "El motivo de anulación no puede superar los 300 caracteres"
    )
    @Column(
            name = "motivo_anulacion",
            length = 300
    )
    private String motivoAnulacion;

    /*
     * Usuario que recibió y registró el pago.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
