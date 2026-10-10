package com.LaFerreteria.LaFerreteria.dto;

import com.LaFerreteria.LaFerreteria.entity.Venta;

import java.math.BigDecimal;
import java.util.List;

public record ReporteVentasDTO(
        List<Venta> ventas,
        BigDecimal totalVendido,
        BigDecimal costoTotal,
        BigDecimal gananciaTotal
) {

    public int cantidadVentas() {
        return ventas.size();
    }
}