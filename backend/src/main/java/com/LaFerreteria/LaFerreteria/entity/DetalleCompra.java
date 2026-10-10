package com.LaFerreteria.LaFerreteria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalles_compra")
@Getter
@Setter
@NoArgsConstructor
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(
            value = "0.001",
            message = "La cantidad debe ser mayor que cero"
    )
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal cantidad;

    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El costo unitario debe ser mayor que cero"
    )
    @Column(
            name = "costo_unitario",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal costoUnitario;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;
}