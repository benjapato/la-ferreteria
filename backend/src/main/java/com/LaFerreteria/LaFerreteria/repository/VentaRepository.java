package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    interface TotalPorMedio {
        FormaPago getFormaPago();
        BigDecimal getMonto();
    }

    @Query("""
        SELECT v.formaPago AS formaPago, SUM(v.total) AS monto
        FROM Venta v
        WHERE v.estado = :estado AND v.formaPago <> :combinado
          AND v.fecha >= :desde AND v.fecha < :hasta
        GROUP BY v.formaPago
        """)
    List<TotalPorMedio> sumarVentasSimplesPorMedio(
            @Param("estado") EstadoVenta estado,
            @Param("combinado") FormaPago combinado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT p.formaPago AS formaPago, SUM(p.monto) AS monto
        FROM PagoVenta p JOIN p.venta v
        WHERE v.estado = :estado AND v.formaPago = :combinado
          AND v.fecha >= :desde AND v.fecha < :hasta
        GROUP BY p.formaPago
        """)
    List<TotalPorMedio> sumarPagosCombinadosPorMedio(
            @Param("estado") EstadoVenta estado,
            @Param("combinado") FormaPago combinado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    List<Venta> findAllByOrderByFechaDesc();

    List<Venta> findByEstadoOrderByFechaDesc(
            EstadoVenta estado
    );

    List<Venta> findByFormaPagoOrderByFechaDesc(
            FormaPago formaPago
    );

    List<Venta> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    );
    @EntityGraph(
            attributePaths = {
                    "detalles",
                    "detalles.producto",
                    "cliente",
                    "usuario",
                    "caja"
            }
    )
    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        WHERE v.id = :id
        """)
    Optional<Venta> buscarCompletaPorId(
            @Param("id") Long id
    );
    List<Venta> findByEstadoAndFechaBetweenOrderByFechaDesc(
            EstadoVenta estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    );

    @EntityGraph(attributePaths = {"usuario"})
    Page<Venta> findByClienteId(
            Long clienteId,
            Pageable pageable
    );
}
