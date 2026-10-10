package com.LaFerreteria.LaFerreteria.dto;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.entity.Venta;

import java.math.BigDecimal;
import java.util.List;

public record ReporteCajaDTO(

        List<Caja> cajas,
        List<Venta> ventas,
        List<PagoCliente> pagosClientes,

        BigDecimal ventasEfectivo,
        BigDecimal ventasTransferencia,
        BigDecimal ventasTarjetaDebito,
        BigDecimal ventasTarjetaCredito,
        BigDecimal ventasCuentaCorriente,
        BigDecimal totalVentasFacturadas,
        BigDecimal totalIngresosVentas,

        BigDecimal pagosClientesEfectivo,
        BigDecimal pagosClientesTransferencia,
        BigDecimal pagosClientesTarjetaDebito,
        BigDecimal pagosClientesTarjetaCredito,
        BigDecimal pagosClientesOtro,
        BigDecimal totalPagosClientes,

        BigDecimal totalGeneral,

        int cajasAbiertas,
        int cajasCerradas

) {

    public int cantidadCajas() {
        return cajas.size();
    }

    public int cantidadVentas() {
        return ventas.size();
    }

    public int cantidadPagosClientes() {
        return pagosClientes.size();
    }
}