package com.LaFerreteria.LaFerreteria.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalles_venta")
@Getter
@Setter
@NoArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "venta_id",
            nullable = false
    )
    private Venta venta;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "producto_id",
            nullable = false
    )
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
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

    /*
     * Precio al que se vendió el producto
     * en esta venta particular.
     */
    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El precio unitario debe ser mayor que cero"
    )
    @Column(
            name = "precio_unitario",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal precioUnitario;

    /*
     * Precio de compra que tenía el producto
     * al momento de realizar la venta.
     */
    @NotNull(message = "El costo unitario es obligatorio")
    @DecimalMin(
            value = "0.00",
            message = "El costo unitario no puede ser negativo"
    )
    @Column(
            name = "costo_unitario",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal costoUnitario;

    /*
     * cantidad × precio unitario.
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal subtotal = BigDecimal.ZERO;

    /*
     * cantidad × costo unitario.
     */
    @Column(
            name = "costo_subtotal",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal costoSubtotal = BigDecimal.ZERO;

    /*
     * subtotal - costo subtotal.
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal ganancia = BigDecimal.ZERO;
}