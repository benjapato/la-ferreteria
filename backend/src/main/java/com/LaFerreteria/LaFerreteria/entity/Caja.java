package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cajas")
@Getter
@Setter
@NoArgsConstructor
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "fecha_apertura",
            nullable = false
    )
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /*
     * El estado se cambia automáticamente:
     *
     * Abrir caja  -> ABIERTA
     * Cerrar caja -> CERRADA
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private EstadoCaja estado;

    /*
     * Ventas cobradas durante esta caja.
     */

    @Column(
            name = "ventas_efectivo",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal ventasEfectivo =
            BigDecimal.ZERO;

    @Column(
            name = "ventas_transferencia",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal ventasTransferencia =
            BigDecimal.ZERO;

    @Column(
            name = "ventas_tarjeta_debito",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal ventasTarjetaDebito =
            BigDecimal.ZERO;

    @Column(
            name = "ventas_tarjeta_credito",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal ventasTarjetaCredito =
            BigDecimal.ZERO;

    /*
     * Pagos de cuentas corrientes recibidos
     * durante esta caja.
     */

    @Column(
            name = "pagos_clientes_efectivo",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal pagosClientesEfectivo =
            BigDecimal.ZERO;

    @Column(
            name = "pagos_clientes_transferencia",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal pagosClientesTransferencia =
            BigDecimal.ZERO;

    @Column(
            name = "pagos_clientes_tarjeta_debito",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal pagosClientesTarjetaDebito =
            BigDecimal.ZERO;

    @Column(
            name = "pagos_clientes_tarjeta_credito",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal pagosClientesTarjetaCredito =
            BigDecimal.ZERO;

    @Column(
            name = "pagos_clientes_otro",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal pagosClientesOtro =
            BigDecimal.ZERO;

    /*
     * Totales calculados automáticamente.
     */

    @Column(
            name = "total_ventas",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalVentas =
            BigDecimal.ZERO;

    @Column(
            name = "total_pagos_clientes",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalPagosClientes =
            BigDecimal.ZERO;

    @Column(
            name = "total_general",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal totalGeneral =
            BigDecimal.ZERO;

    /*
     * Arqueo de efectivo.
     *
     * Las columnas nuevas se mantienen opcionales para que las cajas
     * historicas creadas antes del arqueo sigan siendo compatibles.
     */

    @Column(
            name = "monto_inicial",
            precision = 15,
            scale = 2
    )
    private BigDecimal montoInicial =
            BigDecimal.ZERO;

    @Column(
            name = "ingresos_efectivo",
            precision = 15,
            scale = 2
    )
    private BigDecimal ingresosEfectivo =
            BigDecimal.ZERO;

    @Column(
            name = "retiros_efectivo",
            precision = 15,
            scale = 2
    )
    private BigDecimal retirosEfectivo =
            BigDecimal.ZERO;

    @Column(
            name = "efectivo_esperado",
            precision = 15,
            scale = 2
    )
    private BigDecimal efectivoEsperado =
            BigDecimal.ZERO;

    @Column(
            name = "efectivo_contado",
            precision = 15,
            scale = 2
    )
    private BigDecimal efectivoContado;

    @Column(
            name = "diferencia_efectivo",
            precision = 15,
            scale = 2
    )
    private BigDecimal diferenciaEfectivo;

    @Column(name = "cierre_automatico")
    private Boolean cierreAutomatico = false;

    /*
     * Usuario que abrió la caja.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_apertura_id")
    private Usuario usuarioApertura;

    /*
     * Usuario que cerró la caja.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_cierre_id")
    private Usuario usuarioCierre;
}
