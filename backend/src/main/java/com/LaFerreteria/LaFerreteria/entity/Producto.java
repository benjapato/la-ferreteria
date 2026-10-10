package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 50, message = "El código no puede superar los 50 caracteres")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    @Column(length = 500)
    private String descripcion;

    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    @Column(length = 100)
    private String marca;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(
            value = "0.00",
            message = "El precio de compra no puede ser negativo"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "El precio de compra debe tener como máximo 2 decimales"
    )
    @Column(
            name = "precio_compra",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(
            value = "0.01",
            message = "El precio de venta debe ser mayor que cero"
    )
    @Digits(
            integer = 13,
            fraction = 2,
            message = "El precio de venta debe tener como máximo 2 decimales"
    )
    @Column(
            name = "precio_venta",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal precioVenta;

    @NotNull(message = "El stock actual es obligatorio")
    @DecimalMin(
            value = "0.000",
            message = "El stock actual no puede ser negativo"
    )
    @Digits(
            integer = 12,
            fraction = 3,
            message = "El stock debe tener como máximo 3 decimales"
    )
    @Column(
            name = "stock_actual",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal stockActual = BigDecimal.ZERO;

    @NotNull(message = "El stock mínimo es obligatorio")
    @DecimalMin(
            value = "0.000",
            message = "El stock mínimo no puede ser negativo"
    )
    @Digits(
            integer = 12,
            fraction = 3,
            message = "El stock mínimo debe tener como máximo 3 decimales"
    )
    @Column(
            name = "stock_minimo",
            nullable = false,
            precision = 15,
            scale = 3
    )
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @NotNull(message = "La unidad de medida es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(
            name = "unidad_medida",
            nullable = false,
            length = 30
    )
    private UnidadMedida unidadMedida;

    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private Boolean activo = true;
}