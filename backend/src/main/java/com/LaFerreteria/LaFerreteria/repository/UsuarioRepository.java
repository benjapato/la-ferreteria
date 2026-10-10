package com.LaFerreteria.LaFerreteria.repository;

import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );

    boolean existsByNombreUsuarioIgnoreCase(
            String nombreUsuario
    );

    boolean existsByNombreUsuarioIgnoreCaseAndIdNot(
            String nombreUsuario,
            Long id
    );

    long countByRolAndActivoTrue(
            RolUsuario rol
    );

    List<Usuario> findAllByOrderByNombreCompletoAsc();

    List<Usuario>
    findByNombreCompletoContainingIgnoreCaseOrNombreUsuarioContainingIgnoreCaseOrderByNombreCompletoAsc(
            String nombreCompleto,
            String nombreUsuario
    );
}
