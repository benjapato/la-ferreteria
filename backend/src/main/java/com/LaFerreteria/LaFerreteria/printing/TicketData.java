package com.LaFerreteria.LaFerreteria.printing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TicketData(Long id, LocalDateTime fecha, String cliente, String documento,
                         String vendedor, Long cajaId, boolean cancelada, BigDecimal total,
                         String observaciones, List<Item> items, List<Payment> pagos) {
    public record Item(String codigo, String nombre, BigDecimal cantidad,
                       BigDecimal precio, BigDecimal subtotal) {}
    public record Payment(String nombre, BigDecimal monto) {}
}
