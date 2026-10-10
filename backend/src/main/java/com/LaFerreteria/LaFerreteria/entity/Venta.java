package com.LaFerreteria.LaFerreteria.entity;

import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /*
     * Por ahora el cliente es opcional.
     * Si queda vacío, se utilizará "Consumidor final".
     */
    @Size(
            max = 150,
            message = "El nombre del cliente no puede superar los 150 caracteres"
    )
    @Column(name = "nombre_cliente", length = 150)
    private String nombreCliente;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "forma_pago",
            nullable = false,
            length = 30
    )
    private FormaPago formaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.CONFIRMADA;

    /*
     * Total cobrado al cliente.
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal total = BigDecimal.ZERO;

    /*
     * Suma de los costos de los productos vendidos.
     */
    @Column(
            name = "costo_total",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal costoTotal = BigDecimal.ZERO;

    /*
     * Ganancia bruta:
     * total de venta - costo total.
     */
    @Column(
            name = "ganancia_total",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal gananciaTotal = BigDecimal.ZERO;

    @Size(
            max = 500,
            message = "Las observaciones no pueden superar los 500 caracteres"
    )
    @Column(length = 500)
    private String observaciones;

    @OneToMany(
            mappedBy = "venta",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(
            mappedBy = "venta",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PagoVenta> pagos = new ArrayList<>();

    /*
     * Caja en la que se registró la venta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id")
    private Caja caja;



    /*
     * Usuario que registró la venta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
