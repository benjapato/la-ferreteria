package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.MovimientoCuentaCorriente;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCuentaCorriente;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCuentaCorrienteRepository;
import com.LaFerreteria.LaFerreteria.repository.PagoClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PagoClienteService {

    private final PagoClienteRepository pagoClienteRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoCuentaCorrienteRepository
            movimientoCuentaCorrienteRepository;
    private final CajaService cajaService;
    private final UsuarioActualService usuarioActualService;

    public PagoClienteService(
            PagoClienteRepository pagoClienteRepository,
            ClienteRepository clienteRepository,
            MovimientoCuentaCorrienteRepository
                    movimientoCuentaCorrienteRepository,
            CajaService cajaService,
            UsuarioActualService usuarioActualService
    ) {
        this.pagoClienteRepository =
                pagoClienteRepository;
        this.clienteRepository = clienteRepository;
        this.movimientoCuentaCorrienteRepository =
                movimientoCuentaCorrienteRepository;
        this.cajaService = cajaService;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<PagoCliente> listarTodos() {

        return pagoClienteRepository
                .findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public List<PagoCliente> listarPorCliente(
            Long clienteId
    ) {

        return pagoClienteRepository
                .findByClienteIdOrderByFechaDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public Optional<PagoCliente> buscarPorId(Long id) {

        return pagoClienteRepository.findById(id);
    }

    /*
     * Registra un pago.
     *
     * Si el pago supera la deuda,
     * el saldo queda negativo y representa
     * dinero a favor del cliente.
     */
    @Transactional
    public PagoCliente registrarPago(
            Long clienteId,
            PagoCliente pago
    ) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un cliente"
            );
        }

        if (pago == null) {
            throw new IllegalArgumentException(
                    "El pago no puede ser nulo"
            );
        }

        if (pago.getMedioPago() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un medio de pago"
            );
        }

        if (pago.getMonto() == null) {
            throw new IllegalArgumentException(
                    "El monto del pago es obligatorio"
            );
        }

        BigDecimal monto = pago.getMonto()
                .setScale(2, RoundingMode.HALF_UP);

        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero"
            );
        }

        Cliente cliente = clienteRepository
                .buscarPorIdParaActualizar(clienteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cliente seleccionado no existe"
                        )
                );

        BigDecimal saldoAnterior = obtenerSaldo(cliente);

        /*
         * No comprobamos que el pago sea menor
         * que la deuda porque permitimos generar
         * saldo a favor.
         */
        BigDecimal saldoPosterior = saldoAnterior
                .subtract(monto)
                .setScale(2, RoundingMode.HALF_UP);

        cliente.setSaldoCuentaCorriente(
                saldoPosterior
        );

        clienteRepository.save(cliente);

        pago.setId(null);
        pago.setFecha(LocalDateTime.now());
        pago.setCliente(cliente);
        pago.setMonto(monto);
        pago.setSaldoAnterior(saldoAnterior);
        pago.setSaldoPosterior(saldoPosterior);
        pago.setObservacion(
                normalizarObservacion(
                        pago.getObservacion()
                )
        );
        pago.setAnulado(false);
        pago.setFechaAnulacion(null);
        pago.setMotivoAnulacion(null);
        pago.setUsuario(
                usuarioActualService.obtenerUsuarioActual()
        );

        /*
         * Primero guardamos el pago.
         */
        PagoCliente pagoGuardado =
                pagoClienteRepository.save(pago);

        /*
         * Registramos el pago dentro de la caja abierta.
         */
        Caja caja = cajaService.registrarPagoCliente(
                pagoGuardado.getMedioPago(),
                pagoGuardado.getMonto()
        );

        /*
         * Asociamos el pago con la caja.
         */
        pagoGuardado.setCaja(caja);

        pagoGuardado = pagoClienteRepository.save(
                pagoGuardado
        );

        /*
         * Ahora creamos el movimiento de cuenta corriente.
         */
        MovimientoCuentaCorriente movimiento =
                new MovimientoCuentaCorriente();

        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCliente(cliente);
        movimiento.setTipo(
                TipoMovimientoCuentaCorriente.PAGO_CLIENTE
        );
        movimiento.setImporte(monto);
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoPosterior(saldoPosterior);
        movimiento.setVenta(null);
        movimiento.setPagoCliente(pagoGuardado);
        movimiento.setDescripcion(
                "Pago recibido mediante "
                        + pagoGuardado.getMedioPago()
                        .name()
                        .replace("_", " ")
        );

        movimientoCuentaCorrienteRepository.save(
                movimiento
        );

        return pagoGuardado;
    }

    /*
     * Anula un pago que fue cargado por error.
     *
     * La operación:
     * - vuelve a sumar el importe al saldo del cliente;
     * - descuenta el importe de la caja original;
     * - marca el pago como anulado;
     * - agrega un movimiento de anulación al historial.
     */
    @Transactional
    public PagoCliente anularPago(
            Long clienteId,
            Long pagoId,
            String motivo
    ) {

        if (clienteId == null) {
            throw new IllegalArgumentException(
                    "El ID del cliente es obligatorio"
            );
        }

        if (pagoId == null) {
            throw new IllegalArgumentException(
                    "El ID del pago es obligatorio"
            );
        }

        String motivoNormalizado =
                normalizarMotivoAnulacion(motivo);

        PagoCliente pago = pagoClienteRepository.findById(pagoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe el pago con ID " + pagoId
                        )
                );

        if (pago.getCliente() == null
                || !clienteId.equals(pago.getCliente().getId())) {

            throw new IllegalArgumentException(
                    "El pago no pertenece al cliente seleccionado"
            );
        }

        if (Boolean.TRUE.equals(pago.getAnulado())) {
            throw new IllegalArgumentException(
                    "El pago ya se encuentra anulado"
            );
        }

        if (pago.getCaja() == null) {
            throw new IllegalArgumentException(
                    "El pago no tiene una caja asociada"
            );
        }

        /*
         * Primero verificamos y descontamos la caja.
         * CajaService impide modificar una caja cerrada.
         */
        cajaService.descontarPagoCliente(
                pago.getCaja().getId(),
                pago.getMedioPago(),
                pago.getMonto()
        );

        Cliente cliente = clienteRepository
                .buscarPorIdParaActualizar(clienteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cliente seleccionado no existe"
                        )
                );

        BigDecimal saldoAnterior = obtenerSaldo(cliente);

        BigDecimal saldoPosterior = saldoAnterior
                .add(pago.getMonto())
                .setScale(2, RoundingMode.HALF_UP);

        cliente.setSaldoCuentaCorriente(
                saldoPosterior
        );

        clienteRepository.save(cliente);

        pago.setAnulado(true);
        pago.setFechaAnulacion(LocalDateTime.now());
        pago.setMotivoAnulacion(motivoNormalizado);

        PagoCliente pagoAnulado =
                pagoClienteRepository.save(pago);

        MovimientoCuentaCorriente movimiento =
                new MovimientoCuentaCorriente();

        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCliente(cliente);
        movimiento.setTipo(
                TipoMovimientoCuentaCorriente
                        .ANULACION_PAGO_CLIENTE
        );
        movimiento.setImporte(
                pagoAnulado.getMonto()
        );
        movimiento.setSaldoAnterior(saldoAnterior);
        movimiento.setSaldoPosterior(saldoPosterior);
        movimiento.setVenta(null);
        movimiento.setPagoCliente(pagoAnulado);
        movimiento.setDescripcion(
                limitarDescripcion(
                        "Anulación del pago #"
                                + pagoAnulado.getId()
                                + ": "
                                + motivoNormalizado
                )
        );

        movimientoCuentaCorrienteRepository.save(
                movimiento
        );

        return pagoAnulado;
    }

    private BigDecimal obtenerSaldo(
            Cliente cliente
    ) {

        BigDecimal saldo =
                cliente.getSaldoCuentaCorriente() == null
                        ? BigDecimal.ZERO
                        : cliente.getSaldoCuentaCorriente();

        return saldo.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String normalizarObservacion(
            String observacion
    ) {

        if (observacion == null
                || observacion.isBlank()) {

            return null;
        }

        return observacion.trim();
    }

    private String normalizarMotivoAnulacion(
            String motivo
    ) {

        if (motivo == null || motivo.isBlank()) {
            return "Sin motivo";
        }

        String motivoNormalizado = motivo.trim();

        if (motivoNormalizado.length() > 300) {
            throw new IllegalArgumentException(
                    "El motivo de anulación no puede superar los 300 caracteres"
            );
        }

        return motivoNormalizado;
    }

    private String limitarDescripcion(
            String descripcion
    ) {

        if (descripcion.length() <= 300) {
            return descripcion;
        }

        return descripcion.substring(0, 300);
    }
}
