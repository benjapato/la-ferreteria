package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VentaFiltroRepository
        extends JpaRepository<Venta, Long>,
        JpaSpecificationExecutor<Venta> {
}