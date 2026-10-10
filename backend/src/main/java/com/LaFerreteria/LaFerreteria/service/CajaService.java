package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.MovimientoCaja;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCaja;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCajaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class CajaService {

    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("9999999999999.99");

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UsuarioActualService usuarioActualService;

    public CajaService(
            CajaRepository cajaRepository,
            MovimientoCajaRepository movimientoCajaRepository,
            UsuarioActualService usuarioActualService
    ) {
        this.cajaRepository = cajaRepository;
        this.movimientoCajaRepository =
                movimientoCajaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<Caja> listarTodas() {

        return cajaRepository
                .findAllByOrderByFechaAperturaDesc();
    }

    @Transactional(readOnly = true)
    public Page<Caja> listarTodas(Pageable pageable) {
        return cajaRepository.findAllBy(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Caja> buscarPorId(Long id) {

        return cajaRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Caja> buscarCajaAbierta() {

        return cajaRepository
                .findFirstByEstadoOrderByFechaAperturaDesc(
                        EstadoCaja.ABIERTA
                );
    }

    @Transactional(readOnly = true)
    public Optional<Caja> buscarArqueoPendiente() {
        return cajaRepository
                .findFirstByCierreAutomaticoTrueAndEfectivoContadoIsNullOrderByFechaCierreDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Caja> buscarFaltanteUltimoCierre() {
        return cajaRepository
                .findFirstByEstadoOrderByFechaCierreDescIdDesc(EstadoCaja.CERRADA)
                .filter(caja -> caja.getEfectivoContado() != null
                        && caja.getDiferenciaEfectivo() != null
                        && caja.getDiferenciaEfectivo().signum() < 0);
    }

    @Transactional(readOnly = true)
    public List<MovimientoCaja> listarMovimientos(
            Long cajaId
    ) {

        if (cajaId == null) {
            return List.of();
        }

        return movimientoCajaRepository
                .findByCajaIdOrderByFechaDesc(cajaId);
    }

    /*
     * Abre una caja nueva.
     *
     * No permite tener dos cajas abiertas.
     */
    @Transactional
    public Caja abrirCaja() {

        return abrirCaja(BigDecimal.ZERO);
    }

    @Transactional
    public Caja abrirCaja(
            BigDecimal montoInicial
    ) {

        BigDecimal importeInicial =
                normalizarMontoNoNegativo(
                        montoInicial == null
                                ? BigDecimal.ZERO
                                : montoInicial,
                        "El monto inicial"
                );

        if (!cajaRepository
                .buscarArqueosPendientesParaActualizar()
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe completar el arqueo de la caja cerrada automaticamente antes de abrir una nueva"
            );
        }

        if (!cajaRepository
                .buscarPorEstadoParaActualizar(
                        EstadoCaja.ABIERTA
                )
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "Ya existe una caja abierta"
            );
        }

        Caja caja = new Caja();

        caja.setFechaApertura(LocalDateTime.now());
        caja.setFechaCierre(null);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setUsuarioApertura(
                usuarioActualService.obtenerUsuarioActual()
        );
        caja.setUsuarioCierre(null);

        caja.setVentasEfectivo(BigDecimal.ZERO);
        caja.setVentasTransferencia(BigDecimal.ZERO);
        caja.setVentasTarjetaDebito(BigDecimal.ZERO);
        caja.setVentasTarjetaCredito(BigDecimal.ZERO);

        caja.setPagosClientesEfectivo(BigDecimal.ZERO);
        caja.setPagosClientesTransferencia(BigDecimal.ZERO);
        caja.setPagosClientesTarjetaDebito(BigDecimal.ZERO);
        caja.setPagosClientesTarjetaCredito(BigDecimal.ZERO);
        caja.setPagosClientesOtro(BigDecimal.ZERO);

        caja.setTotalVentas(BigDecimal.ZERO);
        caja.setTotalPagosClientes(BigDecimal.ZERO);
        caja.setTotalGeneral(BigDecimal.ZERO);

        caja.setMontoInicial(importeInicial);
        caja.setIngresosEfectivo(BigDecimal.ZERO);
        caja.setRetirosEfectivo(BigDecimal.ZERO);
        caja.setEfectivoEsperado(importeInicial);
        caja.setEfectivoContado(null);
        caja.setDiferenciaEfectivo(null);
        caja.setCierreAutomatico(false);

        return cajaRepository.save(caja);
    }

    /*
     * Cierra la caja que actualmente está abierta.
     */
    @Transactional
    public Caja cerrarCaja() {

        Caja caja = obtenerCajaAbiertaObligatoriaParaActualizar();

        recalcularTotales(caja);

        return completarCierre(
                caja,
                valor(caja.getEfectivoEsperado())
        );
    }

    @Transactional
    public Caja cerrarCaja(
            BigDecimal efectivoContado
    ) {

        BigDecimal importeContado =
                normalizarMontoNoNegativo(
                        efectivoContado,
                        "El efectivo contado"
                );

        Caja caja = obtenerCajaAbiertaObligatoriaParaActualizar();

        recalcularTotales(caja);

        return completarCierre(caja, importeContado);
    }

    @Transactional
    public Optional<Caja> cerrarCajaAutomaticamenteSiCorresponde(
            LocalDateTime ahora,
            LocalTime horaCierre
    ) {
        if (ahora == null || horaCierre == null) {
            throw new IllegalArgumentException(
                    "La fecha y hora del cierre automatico son obligatorias"
            );
        }

        Optional<Caja> cajaAbierta = cajaRepository
                .buscarPorEstadoParaActualizar(EstadoCaja.ABIERTA)
                .stream()
                .findFirst();

        if (cajaAbierta.isEmpty()) {
            return Optional.empty();
        }

        Caja caja = cajaAbierta.get();
        LocalDateTime cierreProgramado = LocalDateTime.of(
                caja.getFechaApertura().toLocalDate(),
                horaCierre
        );

        if (ahora.isBefore(cierreProgramado)) {
            return Optional.empty();
        }

        recalcularTotales(caja);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setFechaCierre(cierreProgramado);
        caja.setEfectivoContado(null);
        caja.setDiferenciaEfectivo(null);
        caja.setUsuarioCierre(null);
        caja.setCierreAutomatico(true);

        return Optional.of(cajaRepository.save(caja));
    }

    @Transactional
    public Caja regularizarArqueoAutomatico(
            Long cajaId,
            BigDecimal efectivoContado
    ) {
        BigDecimal importeContado = normalizarMontoNoNegativo(
                efectivoContado,
                "El efectivo contado"
        );

        Caja caja = cajaRepository.buscarPorIdParaActualizar(cajaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe la caja indicada"
                ));

        if (caja.getEstado() != EstadoCaja.CERRADA
                || !Boolean.TRUE.equals(caja.getCierreAutomatico())
                || caja.getEfectivoContado() != null) {
            throw new IllegalArgumentException(
                    "La caja no tiene un arqueo automatico pendiente"
            );
        }

        BigDecimal esperado = valor(caja.getEfectivoEsperado());
        caja.setEfectivoContado(importeContado);
        caja.setDiferenciaEfectivo(
                importeContado.subtract(esperado)
                        .setScale(2, RoundingMode.HALF_UP)
        );
        caja.setUsuarioCierre(
                usuarioActualService.obtenerUsuarioActual()
        );

        return cajaRepository.save(caja);
    }

    @Transactional
    public MovimientoCaja registrarMovimiento(
            TipoMovimientoCaja tipo,
            BigDecimal monto,
            String motivo
    ) {

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar el tipo de movimiento"
            );
        }

        BigDecimal importe = normalizarMonto(monto);
        String motivoNormalizado = normalizarMotivo(motivo);

        Caja caja = obtenerCajaAbiertaObligatoriaParaActualizar();

        recalcularTotales(caja);

        if (tipo == TipoMovimientoCaja.INGRESO) {
            caja.setIngresosEfectivo(
                    valor(caja.getIngresosEfectivo())
                            .add(importe)
            );
        } else {
            if (valor(caja.getEfectivoEsperado())
                    .compareTo(importe) < 0) {
                throw new IllegalArgumentException(
                        "El retiro supera el efectivo esperado en caja"
                );
            }

            caja.setRetirosEfectivo(
                    valor(caja.getRetirosEfectivo())
                            .add(importe)
            );
        }

        recalcularTotales(caja);
        cajaRepository.save(caja);

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setCaja(caja);
        movimiento.setTipo(tipo);
        movimiento.setMonto(importe);
        movimiento.setMotivo(motivoNormalizado);
        movimiento.setUsuario(
                usuarioActualService.obtenerUsuarioActual()
        );

        return movimientoCajaRepository.save(movimiento);
    }

    private Caja completarCierre(
            Caja caja,
            BigDecimal efectivoContado
    ) {

        BigDecimal contado = valor(efectivoContado);
        BigDecimal esperado = valor(
                caja.getEfectivoEsperado()
        );

        caja.setEfectivoContado(contado);
        caja.setDiferenciaEfectivo(
                contado.subtract(esperado)
                        .setScale(2, RoundingMode.HALF_UP)
        );

        caja.setEstado(EstadoCaja.CERRADA);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setUsuarioCierre(
                usuarioActualService.obtenerUsuarioActual()
        );
        caja.setCierreAutomatico(false);

        return cajaRepository.save(caja);
    }

    /*
     * Registra una venta dentro de la caja.
     *
     * Todas las ventas requieren una caja abierta.
     *
     * CUENTA_CORRIENTE no suma dinero, pero la venta
     * queda asociada a la caja donde fue realizada.
     */
    @Transactional
    public Caja registrarVenta(
            FormaPago formaPago,
            BigDecimal monto
    ) {

        if (formaPago == null) {
            throw new IllegalArgumentException(
                    "La forma de pago es obligatoria"
            );
        }

        BigDecimal importe = normalizarMonto(monto);

        Caja caja = obtenerCajaAbiertaObligatoriaParaActualizar();

        switch (formaPago) {

            case EFECTIVO -> caja.setVentasEfectivo(
                    valor(caja.getVentasEfectivo())
                            .add(importe)
            );

            case TRANSFERENCIA ->
                    caja.setVentasTransferencia(
                            valor(caja.getVentasTransferencia())
                                    .add(importe)
                    );

            case TARJETA_DEBITO ->
                    caja.setVentasTarjetaDebito(
                            valor(caja.getVentasTarjetaDebito())
                                    .add(importe)
                    );

            case TARJETA_CREDITO ->
                    caja.setVentasTarjetaCredito(
                            valor(caja.getVentasTarjetaCredito())
                                    .add(importe)
                    );

            case COMBINADO -> throw new IllegalArgumentException(
                    "El pago combinado debe registrarse por cada medio"
            );

            case CUENTA_CORRIENTE -> {
                /*
                 * No se suma dinero porque el cliente
                 * todavía no realizó ningún pago.
                 */
            }
        }

        recalcularTotales(caja);

        return cajaRepository.save(caja);
    }

    /*
     * Registra un pago realizado por un cliente.
     */
    @Transactional
    public Caja registrarPagoCliente(
            MedioPago medioPago,
            BigDecimal monto
    ) {

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "El medio de pago es obligatorio"
            );
        }

        BigDecimal importe = normalizarMonto(monto);

        Caja caja = obtenerCajaAbiertaObligatoriaParaActualizar();

        switch (medioPago) {

            case EFECTIVO ->
                    caja.setPagosClientesEfectivo(
                            valor(
                                    caja.getPagosClientesEfectivo()
                            ).add(importe)
                    );

            case TRANSFERENCIA ->
                    caja.setPagosClientesTransferencia(
                            valor(
                                    caja.getPagosClientesTransferencia()
                            ).add(importe)
                    );

            case TARJETA_DEBITO ->
                    caja.setPagosClientesTarjetaDebito(
                            valor(
                                    caja.getPagosClientesTarjetaDebito()
                            ).add(importe)
                    );

            case TARJETA_CREDITO ->
                    caja.setPagosClientesTarjetaCredito(
                            valor(
                                    caja.getPagosClientesTarjetaCredito()
                            ).add(importe)
                    );

            case OTRO ->
                    caja.setPagosClientesOtro(
                            valor(
                                    caja.getPagosClientesOtro()
                            ).add(importe)
                    );
        }

        recalcularTotales(caja);

        return cajaRepository.save(caja);
    }

    /*
     * Quita de la caja un pago de cliente anulado.
     *
     * Solamente se puede descontar cuando la caja donde
     * se registró el pago todavía está abierta.
     */
    @Transactional
    public Caja descontarPagoCliente(
            Long cajaId,
            MedioPago medioPago,
            BigDecimal monto
    ) {

        if (cajaId == null) {
            throw new IllegalArgumentException(
                    "El pago no tiene una caja asociada"
            );
        }

        Caja caja = cajaRepository.buscarPorIdParaActualizar(cajaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la caja asociada al pago"
                        )
                );

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalArgumentException(
                    "No se puede anular el pago porque la caja donde se registró ya está cerrada"
            );
        }

        if (medioPago == null) {
            throw new IllegalArgumentException(
                    "El pago no tiene un medio de pago"
            );
        }

        BigDecimal importe = normalizarMonto(monto);

        switch (medioPago) {

            case EFECTIVO ->
                    caja.setPagosClientesEfectivo(
                            restar(
                                    caja.getPagosClientesEfectivo(),
                                    importe
                            )
                    );

            case TRANSFERENCIA ->
                    caja.setPagosClientesTransferencia(
                            restar(
                                    caja.getPagosClientesTransferencia(),
                                    importe
                            )
                    );

            case TARJETA_DEBITO ->
                    caja.setPagosClientesTarjetaDebito(
                            restar(
                                    caja.getPagosClientesTarjetaDebito(),
                                    importe
                            )
                    );

            case TARJETA_CREDITO ->
                    caja.setPagosClientesTarjetaCredito(
                            restar(
                                    caja.getPagosClientesTarjetaCredito(),
                                    importe
                            )
                    );

            case OTRO ->
                    caja.setPagosClientesOtro(
                            restar(
                                    caja.getPagosClientesOtro(),
                                    importe
                            )
                    );
        }

        recalcularTotales(caja);

        return cajaRepository.save(caja);
    }

    /*
     * Quita de la caja una venta cancelada.
     *
     * Solo se puede descontar si la caja
     * donde se hizo la venta sigue abierta.
     */
    @Transactional
    public Caja descontarVenta(
            Long cajaId,
            FormaPago formaPago,
            BigDecimal monto
    ) {

        if (cajaId == null) {
            throw new IllegalArgumentException(
                    "La venta no tiene una caja asociada"
            );
        }

        Caja caja = cajaRepository.buscarPorIdParaActualizar(cajaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la caja asociada a la venta"
                        )
                );

        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalArgumentException(
                    "No se puede cancelar una venta cobrada porque su caja ya está cerrada"
            );
        }

        if (formaPago == null) {
            throw new IllegalArgumentException(
                    "La venta no tiene una forma de pago"
            );
        }

        BigDecimal importe = normalizarMonto(monto);

        switch (formaPago) {

            case EFECTIVO -> caja.setVentasEfectivo(
                    restar(
                            caja.getVentasEfectivo(),
                            importe
                    )
            );

            case TRANSFERENCIA ->
                    caja.setVentasTransferencia(
                            restar(
                                    caja.getVentasTransferencia(),
                                    importe
                            )
                    );

            case TARJETA_DEBITO ->
                    caja.setVentasTarjetaDebito(
                            restar(
                                    caja.getVentasTarjetaDebito(),
                                    importe
                            )
                    );

            case TARJETA_CREDITO ->
                    caja.setVentasTarjetaCredito(
                            restar(
                                    caja.getVentasTarjetaCredito(),
                                    importe
                            )
                    );

            case COMBINADO -> throw new IllegalArgumentException(
                    "El pago combinado debe descontarse por cada medio"
            );

            case CUENTA_CORRIENTE -> {
                /*
                 * La venta a cuenta corriente no había
                 * sumado dinero, por lo tanto no se resta.
                 */
            }
        }

        recalcularTotales(caja);

        return cajaRepository.save(caja);
    }

    private Caja obtenerCajaAbiertaObligatoria() {

        return cajaRepository
                .findFirstByEstadoOrderByFechaAperturaDesc(
                        EstadoCaja.ABIERTA
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Debe abrir la caja antes de realizar esta operación"
                        )
                );
    }

    private Caja obtenerCajaAbiertaObligatoriaParaActualizar() {

        return cajaRepository
                .buscarPorEstadoParaActualizar(
                        EstadoCaja.ABIERTA
                )
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Debe abrir la caja antes de realizar esta operación"
                        )
                );
    }

    private void recalcularTotales(Caja caja) {

        BigDecimal totalVentas =
                valor(caja.getVentasEfectivo())
                        .add(valor(
                                caja.getVentasTransferencia()
                        ))
                        .add(valor(
                                caja.getVentasTarjetaDebito()
                        ))
                        .add(valor(
                                caja.getVentasTarjetaCredito()
                        ))
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalPagosClientes =
                valor(caja.getPagosClientesEfectivo())
                        .add(valor(
                                caja.getPagosClientesTransferencia()
                        ))
                        .add(valor(
                                caja.getPagosClientesTarjetaDebito()
                        ))
                        .add(valor(
                                caja.getPagosClientesTarjetaCredito()
                        ))
                        .add(valor(
                                caja.getPagosClientesOtro()
                        ))
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalGeneral =
                totalVentas
                        .add(totalPagosClientes)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal efectivoEsperado =
                valor(caja.getMontoInicial())
                        .add(valor(caja.getVentasEfectivo()))
                        .add(valor(
                                caja.getPagosClientesEfectivo()
                        ))
                        .add(valor(caja.getIngresosEfectivo()))
                        .subtract(valor(caja.getRetirosEfectivo()))
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        caja.setTotalVentas(totalVentas);
        caja.setTotalPagosClientes(
                totalPagosClientes
        );
        caja.setTotalGeneral(totalGeneral);
        caja.setEfectivoEsperado(efectivoEsperado);
    }

    private BigDecimal normalizarMonto(
            BigDecimal monto
    ) {

        if (monto == null) {
            throw new IllegalArgumentException(
                    "El monto es obligatorio"
            );
        }

        BigDecimal importe = monto.setScale(
                2,
                RoundingMode.HALF_UP
        );

        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero"
            );
        }

        validarMontoMaximo(importe);

        return importe;
    }

    private BigDecimal normalizarMontoNoNegativo(
            BigDecimal monto,
            String nombre
    ) {

        if (monto == null) {
            throw new IllegalArgumentException(
                    nombre + " es obligatorio"
            );
        }

        BigDecimal importe = monto.setScale(
                2,
                RoundingMode.HALF_UP
        );

        if (importe.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    nombre + " no puede ser negativo"
            );
        }

        validarMontoMaximo(importe);

        return importe;
    }

    private void validarMontoMaximo(
            BigDecimal importe
    ) {

        if (importe.compareTo(MONTO_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "El monto supera el maximo permitido"
            );
        }
    }

    private String normalizarMotivo(
            String motivo
    ) {

        if (motivo == null || motivo.isBlank()) {
            return "Sin motivo";
        }

        String texto = motivo.trim();

        if (texto.length() > 200) {
            throw new IllegalArgumentException(
                    "El motivo no puede superar los 200 caracteres"
            );
        }

        return texto;
    }

    private BigDecimal valor(
            BigDecimal numero
    ) {

        if (numero == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        return numero.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal restar(
            BigDecimal valorActual,
            BigDecimal importe
    ) {

        BigDecimal resultado =
                valor(valorActual).subtract(importe);

        if (resultado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "No se puede descontar el importe porque supera el total registrado en la caja"
            );
        }

        return resultado.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }
}
