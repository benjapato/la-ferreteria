package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.MovimientoCuentaCorriente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MovimientoCuentaCorrienteRepository
        extends JpaRepository<MovimientoCuentaCorriente, Long> {

    @EntityGraph(
            attributePaths = {
                    "venta",
                    "pagoCliente"
            }
    )
    List<MovimientoCuentaCorriente>
    findByClienteIdOrderByFechaDescIdDesc(
            Long clienteId
    );

    @EntityGraph(
            attributePaths = {
                    "venta",
                    "pagoCliente",
                    "pagoCliente.usuario",
                    "pagoCliente.caja"
            }
    )
    Page<MovimientoCuentaCorriente>
    findByClienteId(
            Long clienteId,
            Pageable pageable
    );

    List<MovimientoCuentaCorriente>
    findAllByOrderByFechaDescIdDesc();
}
