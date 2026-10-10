package com.LaFerreteria.LaFerreteria.dto;

import java.math.BigDecimal;

public record VentasPorMedioDTO(
        BigDecimal efectivo,
        BigDecimal transferencia,
        BigDecimal debito,
        BigDecimal credito,
        BigDecimal cuentaCorriente,
        BigDecimal totalCobrado
) {
}
