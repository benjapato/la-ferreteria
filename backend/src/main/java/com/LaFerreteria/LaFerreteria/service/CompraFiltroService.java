package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.repository.CompraFiltroRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CompraFiltroService {

    private final CompraFiltroRepository compraFiltroRepository;

    public CompraFiltroService(
            CompraFiltroRepository compraFiltroRepository
    ) {
        this.compraFiltroRepository =
                compraFiltroRepository;
    }

    @Transactional(readOnly = true)
    public List<Compra> buscar(
            String numero,
            String proveedor,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado
    ) {

        Specification<Compra> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> filtros =
                            new ArrayList<>();

                    agregarFiltroNumero(
                            filtros,
                            root,
                            criteriaBuilder,
                            numero
                    );

                    agregarFiltroProveedor(
                            filtros,
                            root,
                            criteriaBuilder,
                            proveedor
                    );

                    agregarFiltroFecha(
                            filtros,
                            root,
                            criteriaBuilder,
                            fechaDesde,
                            fechaHasta
                    );

                    agregarFiltroEstado(
                            filtros,
                            root,
                            criteriaBuilder,
                            estado
                    );

                    return criteriaBuilder.and(
                            filtros.toArray(
                                    Predicate[]::new
                            )
                    );
                };

        Sort orden =
                Sort.by(
                        Sort.Order.desc("fecha"),
                        Sort.Order.desc("id")
                );

        return compraFiltroRepository.findAll(
                specification,
                orden
        );
    }

    @Transactional(readOnly = true)
    public Page<Compra> buscar(
            String numero,
            String proveedor,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            Pageable pageable
    ) {

        Specification<Compra> specification = crearSpecification(
                numero,
                proveedor,
                fechaDesde,
                fechaHasta,
                estado
        );

        return compraFiltroRepository.findAll(
                specification,
                pageable
        );
    }

    private Specification<Compra> crearSpecification(
            String numero,
            String proveedor,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> filtros = new ArrayList<>();

            agregarFiltroNumero(
                    filtros,
                    root,
                    criteriaBuilder,
                    numero
            );
            agregarFiltroProveedor(
                    filtros,
                    root,
                    criteriaBuilder,
                    proveedor
            );
            agregarFiltroFecha(
                    filtros,
                    root,
                    criteriaBuilder,
                    fechaDesde,
                    fechaHasta
            );
            agregarFiltroEstado(
                    filtros,
                    root,
                    criteriaBuilder,
                    estado
            );

            return criteriaBuilder.and(
                    filtros.toArray(Predicate[]::new)
            );
        };
    }

    private void agregarFiltroNumero(
            List<Predicate> filtros,
            Root<Compra> root,
            CriteriaBuilder criteriaBuilder,
            String numero
    ) {

        if (
                numero == null
                        || numero.isBlank()
        ) {
            return;
        }

        try {

            Long idCompra =
                    Long.parseLong(
                            numero.trim()
                    );

            filtros.add(
                    criteriaBuilder.equal(
                            root.get("id"),
                            idCompra
                    )
            );

        } catch (NumberFormatException exception) {

            /*
             * Si el número ingresado no es válido,
             * no devolvemos resultados.
             */
            filtros.add(
                    criteriaBuilder.disjunction()
            );
        }
    }

    private void agregarFiltroProveedor(
            List<Predicate> filtros,
            Root<Compra> root,
            CriteriaBuilder criteriaBuilder,
            String proveedor
    ) {

        if (
                proveedor == null
                        || proveedor.isBlank()
        ) {
            return;
        }

        String texto =
                proveedor
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        String patron =
                "%" + texto + "%";

        Join<Object, Object> proveedorJoin =
                root.join(
                        "proveedor",
                        JoinType.LEFT
                );

        Predicate nombreProveedor =
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                proveedorJoin
                                        .<String>get(
                                                "nombre"
                                        )
                        ),
                        patron
                );

        filtros.add(nombreProveedor);
    }

    private void agregarFiltroFecha(
            List<Predicate> filtros,
            Root<Compra> root,
            CriteriaBuilder criteriaBuilder,
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {

        if (
                fechaDesde == null
                        && fechaHasta == null
        ) {
            return;
        }

        Path<?> fechaGenerica =
                root.get("fecha");

        /*
         * Caso en el que Compra.fecha
         * sea LocalDate.
         */
        if (
                LocalDate.class.equals(
                        fechaGenerica.getJavaType()
                )
        ) {

            Path<LocalDate> fecha =
                    root.get("fecha");

            if (fechaDesde != null) {

                filtros.add(
                        criteriaBuilder
                                .greaterThanOrEqualTo(
                                        fecha,
                                        fechaDesde
                                )
                );
            }

            if (fechaHasta != null) {

                filtros.add(
                        criteriaBuilder
                                .lessThanOrEqualTo(
                                        fecha,
                                        fechaHasta
                                )
                );
            }

            return;
        }

        /*
         * Caso en el que Compra.fecha
         * sea LocalDateTime.
         */

        Path<LocalDateTime> fecha =
                root.get("fecha");

        if (fechaDesde != null) {

            filtros.add(
                    criteriaBuilder
                            .greaterThanOrEqualTo(
                                    fecha,
                                    fechaDesde.atStartOfDay()
                            )
            );
        }

        if (fechaHasta != null) {

            LocalDateTime limiteSuperior =
                    fechaHasta
                            .plusDays(1)
                            .atStartOfDay();

            filtros.add(
                    criteriaBuilder.lessThan(
                            fecha,
                            limiteSuperior
                    )
            );
        }
    }

    private void agregarFiltroEstado(
            List<Predicate> filtros,
            Root<Compra> root,
            CriteriaBuilder criteriaBuilder,
            String estado
    ) {

        if (
                estado == null
                        || estado.isBlank()
        ) {
            return;
        }

        Path<?> campoEstado =
                root.get("estado");

        try {

            Object estadoConvertido =
                    convertirAEnum(
                            campoEstado.getJavaType(),
                            estado
                    );

            filtros.add(
                    criteriaBuilder.equal(
                            campoEstado,
                            estadoConvertido
                    )
            );

        } catch (IllegalArgumentException exception) {

            /*
             * Si llega un estado inexistente,
             * la consulta no devuelve resultados.
             */
            filtros.add(
                    criteriaBuilder.disjunction()
            );
        }
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private Object convertirAEnum(
            Class<?> tipoEnum,
            String valor
    ) {

        Class<? extends Enum> claseEnum =
                (Class<? extends Enum>)
                        tipoEnum.asSubclass(
                                Enum.class
                        );

        return Enum.valueOf(
                claseEnum,
                valor
        );
    }
}
