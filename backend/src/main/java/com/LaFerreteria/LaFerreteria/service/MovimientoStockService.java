package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.MovimientoStock;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoStockService {

    private final MovimientoStockRepository movimientoStockRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioActualService usuarioActualService;

    public MovimientoStockService(
            MovimientoStockRepository movimientoStockRepository,
            ProductoRepository productoRepository,
            UsuarioActualService usuarioActualService
    ) {
        this.movimientoStockRepository = movimientoStockRepository;
        this.productoRepository = productoRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<MovimientoStock> listarTodos() {
        return movimientoStockRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public Page<MovimientoStock> listarTodos(Pageable pageable) {
        return movimientoStockRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovimientoStock> buscar(
            String search,
            TipoMovimientoStock type,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        return movimientoStockRepository.buscar(
                search,
                type,
                fromDate,
                toDate,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<MovimientoStock> listarPorProducto(Long productoId) {
        return movimientoStockRepository
                .findByProductoIdOrderByFechaDesc(productoId);
    }

    @Transactional
    public MovimientoStock ajustarStock(
            Long productoId,
            TipoMovimientoStock tipo,
            BigDecimal cantidad,
            String observacion
    ) {

        if (productoId == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un producto"
            );
        }

        if (tipo != TipoMovimientoStock.AJUSTE_ENTRADA
                && tipo != TipoMovimientoStock.AJUSTE_SALIDA) {

            throw new IllegalArgumentException(
                    "El tipo de ajuste seleccionado no es válido"
            );
        }

        if (cantidad == null
                || cantidad.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero"
            );
        }

        Producto producto = productoRepository
                .buscarPorIdParaActualizar(productoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El producto seleccionado no existe"
                        )
                );

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException(
                    "No se puede ajustar un producto inactivo"
            );
        }

        BigDecimal stockAnterior =
                producto.getStockActual() == null
                        ? BigDecimal.ZERO
                        : producto.getStockActual();

        BigDecimal stockPosterior;

        if (tipo == TipoMovimientoStock.AJUSTE_ENTRADA) {

            stockPosterior = stockAnterior.add(cantidad);

        } else {

            if (stockAnterior.compareTo(cantidad) < 0) {
                throw new IllegalArgumentException(
                        "El ajuste dejaría el stock negativo. "
                                + "Stock disponible: "
                                + stockAnterior
                                .stripTrailingZeros()
                                .toPlainString()
                );
            }

            stockPosterior = stockAnterior.subtract(cantidad);
        }

        producto.setStockActual(stockPosterior);

        productoRepository.save(producto);

        MovimientoStock movimiento = new MovimientoStock();

        movimiento.setFecha(LocalDateTime.now());
        movimiento.setProducto(producto);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockPosterior(stockPosterior);
        movimiento.setCompra(null);
        movimiento.setVenta(null);
        movimiento.setUsuario(
                usuarioActualService.obtenerUsuarioActual()
        );
        movimiento.setObservacion(
                normalizarObservacion(observacion, tipo)
        );

        return movimientoStockRepository.save(movimiento);
    }

    private String normalizarObservacion(
            String observacion,
            TipoMovimientoStock tipo
    ) {

        if (observacion != null && !observacion.isBlank()) {
            return observacion.trim();
        }

        if (tipo == TipoMovimientoStock.AJUSTE_ENTRADA) {
            return "Ajuste manual de entrada";
        }

        return "Ajuste manual de salida";
    }
}
