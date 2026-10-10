package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface ClienteRepository
        extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNombreAsc();

    List<Cliente> findByActivoTrueOrderByNombreAsc();

    Optional<Cliente> findByDocumento(String documento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM Cliente c
        WHERE c.id = :id
        """)
    Optional<Cliente> buscarPorIdParaActualizar(
            @Param("id") Long id
    );

    boolean existsByDocumento(String documento);

    @Query("""
        SELECT c
        FROM Cliente c
        WHERE LOWER(c.nombre)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(COALESCE(c.documento, ''))
                  LIKE LOWER(CONCAT('%', :texto, '%'))
        ORDER BY c.nombre ASC
        """)
    List<Cliente> buscarPorNombreODocumento(
            @Param("texto") String texto
    );

    @Query("""
        SELECT c
        FROM Cliente c
        WHERE LOWER(c.nombre)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(COALESCE(c.documento, ''))
                  LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    Page<Cliente> buscarPaginaPorNombreODocumento(
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("""
        SELECT c
        FROM Cliente c
        WHERE (
            :texto = ''
            OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(c.documento, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(c.telefono, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
        )
          AND (:activo IS NULL OR c.activo = :activo)
          AND (:soloDeuda = false OR c.saldoCuentaCorriente > 0)
          AND (:soloAlDia = false OR c.saldoCuentaCorriente = 0)
          AND (:soloAFavor = false OR c.saldoCuentaCorriente < 0)
        """)
    Page<Cliente> buscarCatalogo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            @Param("soloDeuda") boolean soloDeuda,
            @Param("soloAlDia") boolean soloAlDia,
            @Param("soloAFavor") boolean soloAFavor,
            Pageable pageable
    );
}
