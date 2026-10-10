package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface MovimientoCajaRepository
        extends JpaRepository<MovimientoCaja, Long> {

    @EntityGraph(attributePaths = {"usuario"})
    List<MovimientoCaja> findByCajaIdOrderByFechaDesc(
            Long cajaId
    );
}
