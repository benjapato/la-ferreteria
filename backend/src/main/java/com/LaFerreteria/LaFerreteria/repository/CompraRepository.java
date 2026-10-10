package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.enums.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findAllByOrderByFechaDesc();

    List<Compra> findByEstadoOrderByFechaDesc(
            EstadoCompra estado
    );

    List<Compra> findByProveedorIdOrderByFechaDesc(
            Long proveedorId
    );
    @EntityGraph(
            attributePaths = {
                    "proveedor",
                    "usuario",
                    "detalles",
                    "detalles.producto"
            }
    )
    @Query("""
        SELECT DISTINCT c
        FROM Compra c
        WHERE c.id = :id
        """)
    Optional<Compra> buscarCompletaPorId(
            @Param("id") Long id
    );
}
