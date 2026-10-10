package com.LaFerreteria.LaFerreteria.dto;

import java.math.BigDecimal;

public record ProductoMasVendidoDTO(
        Long productoId,
        String codigo,
        String nombre,
        BigDecimal cantidadVendida,
        Long cantidadVentas,
        BigDecimal totalVendido,
        BigDecimal gananciaGenerada
) {
}