package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.dto.ReporteCajaDTO;
import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.entity.PagoVenta;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.repository.PagoClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReporteCajaService {

    private final VentaRepository ventaRepository;
    private final PagoClienteRepository
            pagoClienteRepository;
    private final CajaRepository cajaRepository;

    public ReporteCajaService(
            VentaRepository ventaRepository,
            PagoClienteRepository pagoClienteRepository,
            CajaRepository cajaRepository
    ) {
        this.ventaRepository = ventaRepository;
        this.pagoClienteRepository =
                pagoClienteRepository;
        this.cajaRepository = cajaRepository;
    }

    @Transactional(readOnly = true)
    public ReporteCajaDTO generarReporte(
            LocalDate desde,
            LocalDate hasta
    ) {

        validarFechas(desde, hasta);

        LocalDateTime fechaDesde =
                desde.atStartOfDay();

        LocalDateTime fechaHasta =
                hasta.atTime(LocalTime.MAX);

        /*
         * Obtenemos solamente las ventas confirmadas.
         * Las ventas canceladas no deben aparecer
         * como ingresos.
         */
        List<Venta> ventas =
                ventaRepository
                        .findByEstadoAndFechaBetweenOrderByFechaDesc(
                                EstadoVenta.CONFIRMADA,
                                fechaDesde,
                                fechaHasta
                        );

        /*
         * Los pagos anulados no se incluyen
         * en el reporte.
         */
        List<PagoCliente> pagosClientes =
                pagoClienteRepository
                        .findByFechaBetweenOrderByFechaDesc(
                                fechaDesde,
                                fechaHasta
                        )
                        .stream()
                        .filter(pago ->
                                !Boolean.TRUE.equals(
                                        pago.getAnulado()
                                )
                        )
                        .toList();

        /*
         * Se incluyen las cajas cuya apertura
         * se realizó dentro del rango seleccionado.
         */
        List<Caja> cajas =
                cajaRepository
                        .findAllByOrderByFechaAperturaDesc()
                        .stream()
                        .filter(caja ->
                                caja.getFechaApertura() != null
                                        && !caja
                                        .getFechaApertura()
                                        .isBefore(fechaDesde)
                                        && !caja
                                        .getFechaApertura()
                                        .isAfter(fechaHasta)
                        )
                        .toList();

        /*
         * Totales de ventas.
         */
        BigDecimal ventasEfectivo = cero();

        BigDecimal ventasTransferencia =
                cero();

        BigDecimal ventasTarjetaDebito =
                cero();

        BigDecimal ventasTarjetaCredito =
                cero();

        BigDecimal ventasCuentaCorriente =
                cero();

        for (Venta venta : ventas) {

            BigDecimal total =
                    valor(venta.getTotal());

            switch (venta.getFormaPago()) {

                case EFECTIVO ->

                        ventasEfectivo =
                                ventasEfectivo
                                        .add(total);

                case TRANSFERENCIA ->

                        ventasTransferencia =
                                ventasTransferencia
                                        .add(total);

                case TARJETA_DEBITO ->

                        ventasTarjetaDebito =
                                ventasTarjetaDebito
                                        .add(total);

                case TARJETA_CREDITO ->

                        ventasTarjetaCredito =
                                ventasTarjetaCredito
                                        .add(total);

                case COMBINADO -> {

                    List<PagoVenta> pagosVenta =
                            venta.getPagos() == null
                                    ? List.of()
                                    : venta.getPagos();

                    for (PagoVenta pagoVenta : pagosVenta) {

                        BigDecimal monto = valor(
                                pagoVenta.getMonto()
                        );

                        switch (pagoVenta.getFormaPago()) {
                            case EFECTIVO -> ventasEfectivo =
                                    ventasEfectivo.add(monto);
                            case TRANSFERENCIA ->
                                    ventasTransferencia =
                                            ventasTransferencia.add(monto);
                            case TARJETA_DEBITO ->
                                    ventasTarjetaDebito =
                                            ventasTarjetaDebito.add(monto);
                            case TARJETA_CREDITO ->
                                    ventasTarjetaCredito =
                                            ventasTarjetaCredito.add(monto);
                            case CUENTA_CORRIENTE ->
                                    ventasCuentaCorriente =
                                            ventasCuentaCorriente.add(monto);
                            case COMBINADO -> {
                                // No es un medio valido dentro del desglose.
                            }
                        }
                    }
                }

                case CUENTA_CORRIENTE ->

                        ventasCuentaCorriente =
                                ventasCuentaCorriente
                                        .add(total);
            }
        }

        /*
         * Cuenta corriente no representa
         * dinero ingresado en caja todavía.
         */
        BigDecimal totalIngresosVentas =
                ventasEfectivo
                        .add(ventasTransferencia)
                        .add(ventasTarjetaDebito)
                        .add(ventasTarjetaCredito)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * Total facturado incluyendo las ventas
         * a cuenta corriente.
         */
        BigDecimal totalVentasFacturadas =
                totalIngresosVentas
                        .add(ventasCuentaCorriente)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * Totales de pagos de clientes.
         */
        BigDecimal pagosClientesEfectivo =
                cero();

        BigDecimal pagosClientesTransferencia =
                cero();

        BigDecimal pagosClientesTarjetaDebito =
                cero();

        BigDecimal pagosClientesTarjetaCredito =
                cero();

        BigDecimal pagosClientesOtro =
                cero();

        for (PagoCliente pago : pagosClientes) {

            BigDecimal monto =
                    valor(pago.getMonto());

            switch (pago.getMedioPago()) {

                case EFECTIVO ->

                        pagosClientesEfectivo =
                                pagosClientesEfectivo
                                        .add(monto);

                case TRANSFERENCIA ->

                        pagosClientesTransferencia =
                                pagosClientesTransferencia
                                        .add(monto);

                case TARJETA_DEBITO ->

                        pagosClientesTarjetaDebito =
                                pagosClientesTarjetaDebito
                                        .add(monto);

                case TARJETA_CREDITO ->

                        pagosClientesTarjetaCredito =
                                pagosClientesTarjetaCredito
                                        .add(monto);

                case OTRO ->

                        pagosClientesOtro =
                                pagosClientesOtro
                                        .add(monto);
            }
        }

        BigDecimal totalPagosClientes =
                pagosClientesEfectivo
                        .add(pagosClientesTransferencia)
                        .add(pagosClientesTarjetaDebito)
                        .add(pagosClientesTarjetaCredito)
                        .add(pagosClientesOtro)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * Dinero total que efectivamente ingresó.
         */
        BigDecimal totalGeneral =
                totalIngresosVentas
                        .add(totalPagosClientes)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        int cajasAbiertas =
                (int) cajas
                        .stream()
                        .filter(caja ->
                                caja.getEstado()
                                        == EstadoCaja.ABIERTA
                        )
                        .count();

        int cajasCerradas =
                (int) cajas
                        .stream()
                        .filter(caja ->
                                caja.getEstado()
                                        == EstadoCaja.CERRADA
                        )
                        .count();

        return new ReporteCajaDTO(

                cajas,
                ventas,
                pagosClientes,

                ajustar(ventasEfectivo),
                ajustar(ventasTransferencia),
                ajustar(ventasTarjetaDebito),
                ajustar(ventasTarjetaCredito),
                ajustar(ventasCuentaCorriente),
                ajustar(totalVentasFacturadas),
                ajustar(totalIngresosVentas),

                ajustar(pagosClientesEfectivo),
                ajustar(pagosClientesTransferencia),
                ajustar(pagosClientesTarjetaDebito),
                ajustar(pagosClientesTarjetaCredito),
                ajustar(pagosClientesOtro),
                ajustar(totalPagosClientes),

                ajustar(totalGeneral),

                cajasAbiertas,
                cajasCerradas
        );
    }

    private void validarFechas(
            LocalDate desde,
            LocalDate hasta
    ) {

        if (desde == null || hasta == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar las dos fechas"
            );
        }

        if (desde.isAfter(hasta)) {

            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }
    }

    private BigDecimal valor(
            BigDecimal numero
    ) {

        if (numero == null) {
            return cero();
        }

        return numero.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal ajustar(
            BigDecimal numero
    ) {

        return valor(numero);
    }

    private BigDecimal cero() {

        return BigDecimal.ZERO.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}
