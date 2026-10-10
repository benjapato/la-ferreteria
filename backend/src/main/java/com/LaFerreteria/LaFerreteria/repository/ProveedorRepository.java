package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository
        extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByCuit(String cuit);

    boolean existsByCuit(String cuit);

    List<Proveedor> findAllByOrderByNombreAsc();

    List<Proveedor> findByActivoTrueOrderByNombreAsc();
}