package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.dto.ProductoMasVendidoDTO;
import com.LaFerreteria.LaFerreteria.entity.DetalleVenta;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetalleVentaRepository
        extends JpaRepository<DetalleVenta, Long> {

    List<DetalleVenta> findByVentaId(Long ventaId);

    List<DetalleVenta> findByProductoIdOrderByVentaFechaDesc(
            Long productoId
    );

    @Query("""
            SELECT new com.LaFerreteria.LaFerreteria.dto.ProductoMasVendidoDTO(
                d.producto.id,
                d.producto.codigo,
                d.producto.nombre,
                SUM(d.cantidad),
                COUNT(DISTINCT d.venta.id),
                SUM(d.subtotal),
                SUM(d.ganancia)
            )
            FROM DetalleVenta d
            WHERE d.venta.estado = :estado
              AND d.venta.fecha BETWEEN :fechaDesde AND :fechaHasta
            GROUP BY
                d.producto.id,
                d.producto.codigo,
                d.producto.nombre
            ORDER BY
                SUM(d.cantidad) DESC,
                d.producto.nombre ASC
            """)
    List<ProductoMasVendidoDTO> buscarProductosMasVendidos(
            @Param("estado") EstadoVenta estado,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta
    );
}