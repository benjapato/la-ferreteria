package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CajaRepository
        extends JpaRepository<Caja, Long> {

    List<Caja> findAllByOrderByFechaAperturaDesc();

    @EntityGraph(attributePaths = {"usuarioApertura", "usuarioCierre"})
    Page<Caja> findAllBy(Pageable pageable);

    Optional<Caja> findFirstByEstadoOrderByFechaAperturaDesc(
            EstadoCaja estado
    );

    Optional<Caja> findFirstByCierreAutomaticoTrueAndEfectivoContadoIsNullOrderByFechaCierreDesc();

    Optional<Caja> findFirstByEstadoOrderByFechaCierreDescIdDesc(EstadoCaja estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Caja c
        WHERE c.id = :id
        """)
    Optional<Caja> buscarPorIdParaActualizar(
            @Param("id") Long id
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Caja c
        WHERE c.estado = :estado
        ORDER BY c.fechaApertura DESC
        """)
    List<Caja> buscarPorEstadoParaActualizar(
            @Param("estado") EstadoCaja estado
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Caja c
        WHERE c.cierreAutomatico = true
          AND c.efectivoContado IS NULL
        ORDER BY c.fechaCierre DESC
        """)
    List<Caja> buscarArqueosPendientesParaActualizar();

    boolean existsByEstado(
            EstadoCaja estado
    );
}
