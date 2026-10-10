package com.LaFerreteria.LaFerreteria.printing;

import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TicketDataService {
    private final VentaService ventas;
    public TicketDataService(VentaService ventas) { this.ventas = ventas; }

    @Transactional(readOnly = true)
    public TicketData obtener(Long id) {
        var venta = ventas.buscarCompletaPorId(id);
        var cliente = venta.getCliente();
        String nombre = venta.getNombreCliente();
        if (nombre == null || nombre.isBlank()) nombre = cliente == null ? "Consumidor final" : cliente.getNombre();
        var pagos = venta.getPagos().stream()
                .map(p -> new TicketData.Payment(p.getMonto().signum() < 0 ? "Saldo a favor" : pago(p.getFormaPago()), p.getMonto()))
                .toList();
        if (pagos.isEmpty()) pagos = List.of(new TicketData.Payment(pago(venta.getFormaPago()), venta.getTotal()));
        return new TicketData(venta.getId(), venta.getFecha(), nombre,
                cliente == null ? null : cliente.getDocumento(),
                venta.getUsuario() == null ? null : venta.getUsuario().getNombreCompleto(),
                venta.getCaja() == null ? null : venta.getCaja().getId(),
                venta.getEstado() == EstadoVenta.CANCELADA, venta.getTotal(), venta.getObservaciones(),
                venta.getDetalles().stream().map(d -> new TicketData.Item(
                        d.getProducto().getCodigo(), d.getProducto().getNombre(),
                        d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal())).toList(), pagos);
    }

    private String pago(FormaPago forma) {
        return switch (forma) {
            case EFECTIVO -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case TARJETA_DEBITO -> "Debito";
            case TARJETA_CREDITO -> "Credito";
            case CUENTA_CORRIENTE -> "Cuenta corriente";
            case COMBINADO -> "Combinado";
        };
    }
}
