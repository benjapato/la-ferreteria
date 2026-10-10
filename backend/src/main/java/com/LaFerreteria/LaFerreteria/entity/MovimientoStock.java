package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoStock tipo;

    @NotNull
    @DecimalMin(
            value = "0.001",
            message = "La cantidad debe ser mayor que cero"
    )
    @Column(
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal cantidad;

    @NotNull
    @Column(
            name = "stock_anterior",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal stockAnterior;

    @NotNull
    @Column(
            name = "stock_posterior",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal stockPosterior;

    /*
     * Será nulo cuando el movimiento sea un ajuste manual.
     */
    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(length = 300)
    private String observacion;

    /*
     * Usuario responsable del movimiento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
