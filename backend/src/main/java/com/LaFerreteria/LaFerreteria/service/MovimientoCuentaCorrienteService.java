package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.MovimientoCuentaCorriente;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCuentaCorrienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class MovimientoCuentaCorrienteService {

    private final MovimientoCuentaCorrienteRepository repository;

    public MovimientoCuentaCorrienteService(
            MovimientoCuentaCorrienteRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MovimientoCuentaCorriente> listarPorCliente(
            Long clienteId
    ) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "El ID del cliente es obligatorio"
            );
        }

        return repository
                .findByClienteIdOrderByFechaDescIdDesc(
                        clienteId
                );
    }

    @Transactional(readOnly = true)
    public List<MovimientoCuentaCorriente> listarTodos() {
        return repository.findAllByOrderByFechaDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Page<MovimientoCuentaCorriente> listarPorCliente(
            Long clienteId,
            Pageable pageable
    ) {
        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "El ID del cliente es obligatorio"
            );
        }
        return repository.findByClienteId(clienteId, pageable);
    }
}
