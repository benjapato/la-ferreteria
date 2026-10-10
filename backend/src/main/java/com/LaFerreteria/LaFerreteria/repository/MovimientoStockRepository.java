package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;

public interface MovimientoStockRepository
        extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findAllByOrderByFechaDesc();

    List<MovimientoStock> findByProductoIdOrderByFechaDesc(
            Long productoId
    );

    Page<MovimientoStock> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "producto",
            "producto.categoria",
            "usuario",
            "compra",
            "venta"
    })
    @Query("""
        SELECT m
        FROM MovimientoStock m
        WHERE (
            :search = ''
            OR LOWER(m.producto.codigo) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(m.producto.nombre) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        AND (:type IS NULL OR m.tipo = :type)
        AND m.fecha >= :fromDate
        AND m.fecha <= :toDate
        """)
    Page<MovimientoStock> buscar(
            @Param("search") String search,
            @Param("type") TipoMovimientoStock type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
}
