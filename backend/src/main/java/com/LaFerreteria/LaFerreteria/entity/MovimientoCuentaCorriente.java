package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCuentaCorriente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_cuenta_corriente")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoCuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "cliente_id",
            nullable = false
    )
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 40
    )
    private TipoMovimientoCuentaCorriente tipo;

    /*
     * Importe de la operación.
     * Siempre se guarda como número positivo.
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal importe;

    /*
     * Saldo positivo: el cliente debe.
     * Saldo negativo: el cliente tiene dinero a favor.
     */
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

    /*
     * Solamente se completa cuando el movimiento
     * se originó en una venta o cancelación.
     */
    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;

    /*
     * Solamente se completa cuando el movimiento
     * se originó en un pago.
     */
    @ManyToOne
    @JoinColumn(name = "pago_cliente_id")
    private PagoCliente pagoCliente;

    @Column(length = 300)
    private String descripcion;
}