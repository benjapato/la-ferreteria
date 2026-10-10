package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.entity.MovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.service.MovimientoStockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/stock-movements")
public class StockMovementsApiController {

    private static final int PAGE_SIZE = 20;
    private static final LocalDateTime MIN_DATE =
            LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE =
            LocalDateTime.of(2999, 12, 31, 23, 59, 59);

    private final MovimientoStockService movimientoStockService;

    public StockMovementsApiController(
            MovimientoStockService movimientoStockService
    ) {
        this.movimientoStockService = movimientoStockService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MovimientosRespuesta listar(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(defaultValue = "0") int page
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        TipoMovimientoStock tipo = normalizarTipo(type);
        Page<MovimientoStock> movimientos = movimientoStockService.buscar(
                query == null ? "" : query.trim(),
                tipo,
                from == null ? MIN_DATE : from.atStartOfDay(),
                to == null ? MAX_DATE : to.atTime(LocalTime.MAX),
                PageRequest.of(
                        Math.max(page, 0),
                        PAGE_SIZE,
                        Sort.by(
                                Sort.Order.desc("fecha"),
                                Sort.Order.desc("id")
                        )
                )
        );

        List<MovimientoStock> todos = movimientoStockService.listarTodos();
        long entradas = todos.stream().filter(this::esEntrada).count();
        long salidas = todos.size() - entradas;
        long ajustes = todos.stream()
                .filter(movimiento -> movimiento.getTipo()
                        == TipoMovimientoStock.AJUSTE_ENTRADA
                        || movimiento.getTipo()
                        == TipoMovimientoStock.AJUSTE_SALIDA)
                .count();

        return new MovimientosRespuesta(
                movimientos.getContent().stream().map(this::resumir).toList(),
                movimientos.getNumber(),
                movimientos.getTotalPages(),
                movimientos.getTotalElements(),
                movimientos.isFirst(),
                movimientos.isLast(),
                new MetricasMovimientoRespuesta(
                        todos.size(),
                        entradas,
                        salidas,
                        ajustes
                )
        );
    }

    private MovimientoRespuesta resumir(MovimientoStock movimiento) {
        String referenciaTipo = "AJUSTE";
        Long referenciaId = null;
        if (movimiento.getCompra() != null) {
            referenciaTipo = "COMPRA";
            referenciaId = movimiento.getCompra().getId();
        } else if (movimiento.getVenta() != null) {
            referenciaTipo = "VENTA";
            referenciaId = movimiento.getVenta().getId();
        }

        return new MovimientoRespuesta(
                movimiento.getId(),
                movimiento.getFecha(),
                movimiento.getProducto().getId(),
                movimiento.getProducto().getCodigo(),
                movimiento.getProducto().getNombre(),
                movimiento.getProducto().getUnidadMedida().name(),
                movimiento.getTipo().name(),
                movimiento.getCantidad(),
                movimiento.getStockAnterior(),
                movimiento.getStockPosterior(),
                movimiento.getUsuario() == null
                        ? "Sin registrar"
                        : movimiento.getUsuario().getNombreCompleto(),
                referenciaTipo,
                referenciaId,
                movimiento.getObservacion()
        );
    }

    private TipoMovimientoStock normalizarTipo(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return TipoMovimientoStock.valueOf(
                    type.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "El tipo de movimiento seleccionado no es valido"
            );
        }
    }

    private boolean esEntrada(MovimientoStock movimiento) {
        return movimiento.getTipo() == TipoMovimientoStock.ENTRADA_COMPRA
                || movimiento.getTipo()
                == TipoMovimientoStock.ENTRADA_ANULACION_VENTA
                || movimiento.getTipo()
                == TipoMovimientoStock.AJUSTE_ENTRADA;
    }

    public record MovimientosRespuesta(
            List<MovimientoRespuesta> movimientos,
            int pagina,
            int totalPaginas,
            long totalElementos,
            boolean primera,
            boolean ultima,
            MetricasMovimientoRespuesta metricas
    ) {
    }

    public record MetricasMovimientoRespuesta(
            long total,
            long entradas,
            long salidas,
            long ajustes
    ) {
    }

    public record MovimientoRespuesta(
            Long id,
            LocalDateTime fecha,
            Long productoId,
            String codigo,
            String producto,
            String unidad,
            String tipo,
            BigDecimal cantidad,
            BigDecimal stockAnterior,
            BigDecimal stockPosterior,
            String usuario,
            String referenciaTipo,
            Long referenciaId,
            String observacion
    ) {
    }
}
