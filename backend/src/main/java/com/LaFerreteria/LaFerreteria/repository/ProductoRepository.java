package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Producto p
        WHERE p.id = :id
        """)
    Optional<Producto> buscarPorIdParaActualizar(
            @Param("id") Long id
    );

    boolean existsByCodigoIgnoreCase(String codigo);

    List<Producto> findAllByOrderByNombreAsc();

    List<Producto> findByActivoTrueOrderByNombreAsc();

    List<Producto> findTop6ByActivoTrueOrderByNombreAsc();

    List<Producto> findTop6ByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(
            String nombre
    );

    @Query("""
        SELECT p
        FROM Producto p
        WHERE p.activo = true
          AND p.stockActual IS NOT NULL
          AND p.stockMinimo IS NOT NULL
          AND p.stockActual <= p.stockMinimo
        ORDER BY p.stockActual ASC, p.nombre ASC
        """)
    List<Producto> buscarProductosConStockBajo();

    @Query("""
        SELECT p
        FROM Producto p
        WHERE LOWER(p.codigo)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.nombre)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
        ORDER BY p.nombre ASC
        """)
    List<Producto> buscarPorCodigoONombre(
            @Param("texto") String texto
    );

    @Query("""
        SELECT p
        FROM Producto p
        WHERE LOWER(p.codigo)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.nombre)
                  LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    Page<Producto> buscarPaginaPorCodigoONombre(
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Producto p
        WHERE (
            :texto = ''
            OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(COALESCE(p.marca, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
            OR LOWER(p.categoria.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
        )
          AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
          AND (:activo IS NULL OR p.activo = :activo)
          AND (:soloStockBajo = false OR p.stockActual <= p.stockMinimo)
          AND (:soloSinStock = false OR p.stockActual <= 0)
        """)
    Page<Producto> buscarCatalogo(
            @Param("texto") String texto,
            @Param("categoriaId") Long categoriaId,
            @Param("activo") Boolean activo,
            @Param("soloStockBajo") boolean soloStockBajo,
            @Param("soloSinStock") boolean soloSinStock,
            Pageable pageable
    );
}
