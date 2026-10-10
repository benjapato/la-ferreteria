package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompraFiltroRepository
        extends JpaRepository<Compra, Long>,
        JpaSpecificationExecutor<Compra> {
}