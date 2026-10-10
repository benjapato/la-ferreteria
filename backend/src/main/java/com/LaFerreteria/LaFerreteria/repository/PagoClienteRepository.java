package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PagoClienteRepository
        extends JpaRepository<PagoCliente, Long> {

    List<PagoCliente> findAllByOrderByFechaDesc();

    List<PagoCliente> findByClienteIdOrderByFechaDesc(
            Long clienteId
    );

    List<PagoCliente> findByFechaBetweenOrderByFechaDesc(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    );
}