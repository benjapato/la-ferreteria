package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.repository.VentaFiltroRepository;
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
public class VentaFiltroService {

    private final VentaFiltroRepository ventaFiltroRepository;

    public VentaFiltroService(
            VentaFiltroRepository ventaFiltroRepository
    ) {
        this.ventaFiltroRepository =
                ventaFiltroRepository;
    }

    @Transactional(readOnly = true)
    public List<Venta> buscar(
            String numero,
            String cliente,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            String formaPago
    ) {

        Specification<Venta> specification =
                (root, query, criteriaBuilder) -> {

                    List<Predicate> filtros =
                            new ArrayList<>();

                    agregarFiltroNumero(
                            filtros,
                            root,
                            criteriaBuilder,
                            numero
                    );

                    agregarFiltroCliente(
                            filtros,
                            root,
                            criteriaBuilder,
                            cliente
                    );

                    agregarFiltroFecha(
                            filtros,
                            root,
                            criteriaBuilder,
                            fechaDesde,
                            fechaHasta
                    );

                    if (
                            estado != null
                                    && !estado.isBlank()
                    ) {

                        filtros.add(
                                crearFiltroEnum(
                                        root,
                                        criteriaBuilder,
                                        "estado",
                                        estado
                                )
                        );
                    }

                    if (
                            formaPago != null
                                    && !formaPago.isBlank()
                    ) {

                        filtros.add(
                                crearFiltroEnum(
                                        root,
                                        criteriaBuilder,
                                        "formaPago",
                                        formaPago
                                )
                        );
                    }

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

        return ventaFiltroRepository.findAll(
                specification,
                orden
        );
    }

    @Transactional(readOnly = true)
    public Page<Venta> buscar(
            String numero,
            String cliente,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            String formaPago,
            Pageable pageable
    ) {

        Specification<Venta> specification = crearSpecification(
                numero,
                cliente,
                fechaDesde,
                fechaHasta,
                estado,
                formaPago
        );

        return ventaFiltroRepository.findAll(
                specification,
                pageable
        );
    }

    private Specification<Venta> crearSpecification(
            String numero,
            String cliente,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            String formaPago
    ) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> filtros = new ArrayList<>();

            agregarFiltroNumero(
                    filtros,
                    root,
                    criteriaBuilder,
                    numero
            );
            agregarFiltroCliente(
                    filtros,
                    root,
                    criteriaBuilder,
                    cliente
            );
            agregarFiltroFecha(
                    filtros,
                    root,
                    criteriaBuilder,
                    fechaDesde,
                    fechaHasta
            );

            if (estado != null && !estado.isBlank()) {
                filtros.add(
                        crearFiltroEnum(
                                root,
                                criteriaBuilder,
                                "estado",
                                estado
                        )
                );
            }

            if (formaPago != null && !formaPago.isBlank()) {
                filtros.add(
                        crearFiltroEnum(
                                root,
                                criteriaBuilder,
                                "formaPago",
                                formaPago
                        )
                );
            }

            return criteriaBuilder.and(
                    filtros.toArray(Predicate[]::new)
            );
        };
    }

    private void agregarFiltroNumero(
            List<Predicate> filtros,
            Root<Venta> root,
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

            Long idVenta =
                    Long.parseLong(
                            numero.trim()
                    );

            filtros.add(
                    criteriaBuilder.equal(
                            root.get("id"),
                            idVenta
                    )
            );

        } catch (NumberFormatException exception) {

            /*
             * Si el número no es válido,
             * no devolvemos resultados.
             */
            filtros.add(
                    criteriaBuilder.disjunction()
            );
        }
    }

    private void agregarFiltroCliente(
            List<Predicate> filtros,
            Root<Venta> root,
            CriteriaBuilder criteriaBuilder,
            String cliente
    ) {

        if (
                cliente == null
                        || cliente.isBlank()
        ) {
            return;
        }

        String texto =
                cliente
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        String patron =
                "%" + texto + "%";

        /*
         * LEFT JOIN permite encontrar también
         * ventas sin cliente registrado.
         */
        Join<Object, Object> clienteJoin =
                root.join(
                        "cliente",
                        JoinType.LEFT
                );

        Predicate nombreRegistrado =
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                clienteJoin
                                        .<String>get(
                                                "nombre"
                                        )
                        ),
                        patron
                );

        Predicate documento =
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                clienteJoin
                                        .<String>get(
                                                "documento"
                                        )
                        ),
                        patron
                );

        Predicate nombreManual =
                criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.<String>get(
                                        "nombreCliente"
                                )
                        ),
                        patron
                );

        Predicate coincidenciaCliente =
                criteriaBuilder.or(
                        nombreRegistrado,
                        documento,
                        nombreManual
                );

        /*
         * También permite escribir:
         * consumidor
         * consumidor final
         */
        if (texto.contains("consumidor")) {

            Predicate nombreManualNulo =
                    criteriaBuilder.isNull(
                            root.get(
                                    "nombreCliente"
                            )
                    );

            Predicate nombreManualVacio =
                    criteriaBuilder.equal(
                            criteriaBuilder.trim(
                                    root.<String>get(
                                            "nombreCliente"
                                    )
                            ),
                            ""
                    );

            Predicate consumidorFinal =
                    criteriaBuilder.and(
                            criteriaBuilder.isNull(
                                    root.get("cliente")
                            ),
                            criteriaBuilder.or(
                                    nombreManualNulo,
                                    nombreManualVacio
                            )
                    );

            filtros.add(
                    criteriaBuilder.or(
                            coincidenciaCliente,
                            consumidorFinal
                    )
            );

            return;
        }

        filtros.add(coincidenciaCliente);
    }

    private void agregarFiltroFecha(
            List<Predicate> filtros,
            Root<Venta> root,
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
         * Esta parte también funciona si fecha
         * está guardada como LocalDate.
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
         * Caso habitual: fecha es LocalDateTime.
         */

        Path<LocalDateTime> fecha =
                root.get("fecha");

        if (fechaDesde != null) {

            filtros.add(
                    criteriaBuilder
                            .greaterThanOrEqualTo(
                                    fecha,
                                    fechaDesde
                                            .atStartOfDay()
                            )
            );
        }

        if (fechaHasta != null) {

            /*
             * Se utiliza el comienzo del día
             * siguiente para incluir todo el
             * día elegido.
             */
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

    private Predicate crearFiltroEnum(
            Root<Venta> root,
            CriteriaBuilder criteriaBuilder,
            String nombreCampo,
            String valor
    ) {

        Path<?> campo =
                root.get(nombreCampo);

        try {

            Object valorConvertido =
                    valor;

            if (
                    campo
                            .getJavaType()
                            .isEnum()
            ) {

                valorConvertido =
                        convertirAEnum(
                                campo.getJavaType(),
                                valor
                        );
            }

            return criteriaBuilder.equal(
                    campo,
                    valorConvertido
            );

        } catch (IllegalArgumentException exception) {

            /*
             * Si se recibe un estado o forma
             * de pago inexistente, no devuelve
             * resultados.
             */
            return criteriaBuilder.disjunction();
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
