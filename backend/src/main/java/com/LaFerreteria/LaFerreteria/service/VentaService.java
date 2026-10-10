package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.*;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.LaFerreteria.LaFerreteria.dto.ReporteVentasDTO;
import com.LaFerreteria.LaFerreteria.dto.VentasPorMedioDTO;
import java.time.LocalDate;
import java.time.LocalTime;
import com.LaFerreteria.LaFerreteria.dto.ProductoMasVendidoDTO;
import com.LaFerreteria.LaFerreteria.repository.DetalleVentaRepository;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCuentaCorriente;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCuentaCorrienteRepository;


import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoCuentaCorrienteRepository movimientoCuentaCorrienteRepository;
    private final CajaService cajaService;
    private final UsuarioActualService usuarioActualService;

    public VentaService(
            VentaRepository ventaRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            DetalleVentaRepository detalleVentaRepository,
            ClienteRepository clienteRepository,
            MovimientoCuentaCorrienteRepository
                    movimientoCuentaCorrienteRepository,
            CajaService cajaService,
            UsuarioActualService usuarioActualService
    ) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.clienteRepository = clienteRepository;
        this.movimientoCuentaCorrienteRepository =
                movimientoCuentaCorrienteRepository;
        this.cajaService = cajaService;
        this.usuarioActualService = usuarioActualService;
    }
    @Transactional(readOnly = true)
    public List<Venta> listarTodas() {
        return ventaRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public Page<Venta> listarTodas(Pageable pageable) {
        return ventaRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public VentasPorMedioDTO resumirMediosDePagoDelDia(LocalDate dia) {
        EnumMap<FormaPago, BigDecimal> totales = new EnumMap<>(FormaPago.class);
        for (FormaPago forma : FormaPago.values()) {
            totales.put(forma, BigDecimal.ZERO.setScale(2));
        }
        LocalDateTime desde = dia.atStartOfDay();
        LocalDateTime hasta = dia.plusDays(1).atStartOfDay();
        List<VentaRepository.TotalPorMedio> grupos = new ArrayList<>(
                ventaRepository.sumarVentasSimplesPorMedio(
                        EstadoVenta.CONFIRMADA, FormaPago.COMBINADO, desde, hasta));
        grupos.addAll(ventaRepository.sumarPagosCombinadosPorMedio(
                EstadoVenta.CONFIRMADA, FormaPago.COMBINADO, desde, hasta));
        for (VentaRepository.TotalPorMedio grupo : grupos) {
            totales.merge(grupo.getFormaPago(), grupo.getMonto(), BigDecimal::add);
        }
        BigDecimal efectivo = totales.get(FormaPago.EFECTIVO);
        BigDecimal transferencia = totales.get(FormaPago.TRANSFERENCIA);
        BigDecimal debito = totales.get(FormaPago.TARJETA_DEBITO);
        BigDecimal credito = totales.get(FormaPago.TARJETA_CREDITO);
        return new VentasPorMedioDTO(efectivo, transferencia, debito, credito,
                totales.get(FormaPago.CUENTA_CORRIENTE),
                efectivo.add(transferencia).add(debito).add(credito));
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorId(Long id) {
        return ventaRepository.findById(id);
    }

    /*
     * Registra una venta confirmada,
     * descuenta el stock y guarda los movimientos.
     */
    @Transactional
    public Venta crear(Venta venta) {

        validarVentaRecibida(venta);

        if (cajaService.buscarCajaAbierta().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe abrir la caja antes de realizar ventas"
            );
        }

        venta.setId(null);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.CONFIRMADA);
        venta.setUsuario(
                usuarioActualService.obtenerUsuarioActual()
        );

        Cliente cliente = buscarClienteParaVenta(
                venta.getCliente(),
                venta.getFormaPago()
        );

        venta.setCliente(cliente);

        if (cliente != null) {

            /*
             * Guardamos una copia del nombre.
             * Así la venta conserva el nombre histórico.
             */
            venta.setNombreCliente(
                    cliente.getNombre()
            );

        } else {

            venta.setNombreCliente(
                    normalizarNombreCliente(
                            venta.getNombreCliente()
                    )
            );
        }

        venta.setObservaciones(
                normalizarTextoOpcional(venta.getObservaciones())
        );

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal costoTotal = BigDecimal.ZERO;

        Set<Long> productosAgregados = new HashSet<>();

        /*
         * Primero validamos y calculamos todo.
         * Todavía no modificamos el stock.
         */
        for (DetalleVenta detalle : venta.getDetalles()) {

            Producto producto = buscarProductoValido(
                    detalle.getProducto()
            );

            if (!productosAgregados.add(producto.getId())) {
                throw new IllegalArgumentException(
                        "El producto " + producto.getNombre()
                                + " está repetido en la venta"
                );
            }

            BigDecimal cantidad = detalle.getCantidad();

            validarCantidad(cantidad);

            BigDecimal stockActual =
                    producto.getStockActual() == null
                            ? BigDecimal.ZERO
                            : producto.getStockActual();

            if (stockActual.compareTo(cantidad) < 0) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para "
                                + producto.getNombre()
                                + ". Disponible: "
                                + stockActual
                                .stripTrailingZeros()
                                .toPlainString()
                );
            }

            BigDecimal precioUnitario =
                    producto.getPrecioVenta();

            if (precioUnitario == null
                    || precioUnitario.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "El producto " + producto.getNombre()
                                + " no tiene un precio de venta válido"
                );
            }

            BigDecimal costoUnitario =
                    producto.getPrecioCompra() == null
                            ? BigDecimal.ZERO
                            : producto.getPrecioCompra();

            BigDecimal subtotal = cantidad
                    .multiply(precioUnitario)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal costoSubtotal = cantidad
                    .multiply(costoUnitario)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal ganancia = subtotal
                    .subtract(costoSubtotal)
                    .setScale(2, RoundingMode.HALF_UP);

            detalle.setId(null);
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setCostoUnitario(costoUnitario);
            detalle.setSubtotal(subtotal);
            detalle.setCostoSubtotal(costoSubtotal);
            detalle.setGanancia(ganancia);

            total = total.add(subtotal);
            costoTotal = costoTotal.add(costoSubtotal);
        }

        venta.setTotal(
                total.setScale(2, RoundingMode.HALF_UP)
        );

        venta.setCostoTotal(
                costoTotal.setScale(2, RoundingMode.HALF_UP)
        );

        venta.setGananciaTotal(
                total.subtract(costoTotal)
                        .setScale(2, RoundingMode.HALF_UP)
        );

        prepararPagos(
                venta,
                venta.getTotal()
        );

        /*
         * Guardamos la venta y sus detalles.
         */
        Venta ventaGuardada = ventaRepository.save(venta);



        /*
         * Descontamos stock y registramos los movimientos.
         */
        for (DetalleVenta detalle : ventaGuardada.getDetalles()) {

            Producto producto = productoRepository
                    .buscarPorIdParaActualizar(
                            detalle.getProducto().getId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El producto seleccionado no existe"
                            )
                    );

            BigDecimal stockAnterior =
                    producto.getStockActual() == null
                            ? BigDecimal.ZERO
                            : producto.getStockActual();

            if (stockAnterior.compareTo(
                    detalle.getCantidad()
            ) < 0) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para "
                                + producto.getNombre()
                                + ". Disponible: "
                                + stockAnterior
                                .stripTrailingZeros()
                                .toPlainString()
                );
            }

            BigDecimal stockPosterior = stockAnterior.subtract(
                    detalle.getCantidad()
            );

            producto.setStockActual(stockPosterior);

            productoRepository.save(producto);

            MovimientoStock movimiento = new MovimientoStock();

            movimiento.setFecha(LocalDateTime.now());
            movimiento.setProducto(producto);
            movimiento.setTipo(
                    TipoMovimientoStock.SALIDA_VENTA
            );
            movimiento.setCantidad(detalle.getCantidad());
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockPosterior(stockPosterior);
            movimiento.setCompra(null);
            movimiento.setVenta(ventaGuardada);
            movimiento.setUsuario(ventaGuardada.getUsuario());
            movimiento.setObservacion(
                    "Salida de mercadería por venta #"
                            + ventaGuardada.getId()
            );

            movimientoStockRepository.save(movimiento);
        }

        BigDecimal ajusteCuentaCorriente =
                obtenerAjusteCuentaCorriente(ventaGuardada);

        if (ajusteCuentaCorriente.compareTo(BigDecimal.ZERO) != 0) {

            Cliente clienteCuenta = ventaGuardada.getCliente();

            if (clienteCuenta == null) {
                throw new IllegalArgumentException(
                        "La venta no tiene un cliente asociado para ajustar la cuenta corriente"
                );
            }

            BigDecimal saldoAnterior =
                    clienteCuenta.getSaldoCuentaCorriente() == null
                            ? BigDecimal.ZERO
                            : clienteCuenta.getSaldoCuentaCorriente();

            saldoAnterior = saldoAnterior.setScale(
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal saldoNuevo = saldoAnterior
                    .add(ajusteCuentaCorriente)
                    .setScale(2, RoundingMode.HALF_UP);

            clienteCuenta.setSaldoCuentaCorriente(
                    saldoNuevo
            );

            clienteRepository.save(clienteCuenta);

            MovimientoCuentaCorriente movimiento =
                    new MovimientoCuentaCorriente();

            movimiento.setFecha(LocalDateTime.now());
            movimiento.setCliente(clienteCuenta);
            movimiento.setTipo(
                    TipoMovimientoCuentaCorriente
                            .VENTA_CUENTA_CORRIENTE
            );
            movimiento.setImporte(
                    ajusteCuentaCorriente.abs()
            );
            movimiento.setSaldoAnterior(saldoAnterior);
            movimiento.setSaldoPosterior(saldoNuevo);
            movimiento.setVenta(ventaGuardada);
            movimiento.setPagoCliente(null);
            movimiento.setDescripcion(
                    descripcionAjusteCuentaCorriente(
                            ventaGuardada,
                            ajusteCuentaCorriente
                    )
            );

            movimientoCuentaCorrienteRepository.save(
                    movimiento
            );
        }
        /*
         * Toda venta queda asociada a la caja abierta.
         *
         * La cuenta corriente no suma dinero,
         * pero igualmente queda asociada.
         */
        Caja caja = null;

        if (ventaGuardada.getFormaPago()
                == FormaPago.COMBINADO) {

            for (PagoVenta pago : ventaGuardada.getPagos()) {
                if (pago.getFormaPago()
                        == FormaPago.CUENTA_CORRIENTE) {
                    continue;
                }

                caja = cajaService.registrarVenta(
                        pago.getFormaPago(),
                        pago.getMonto()
                );
            }

        } else {
            caja = cajaService.registrarVenta(
                    ventaGuardada.getFormaPago(),
                    ventaGuardada.getTotal()
            );
        }

        ventaGuardada.setCaja(caja);

        ventaGuardada = ventaRepository.save(
                ventaGuardada
        );

        return ventaGuardada;
    }

    /*
     * Cancela una venta confirmada
     * y devuelve los productos al stock.
     */
    @Transactional
    public Venta cancelar(Long id) {

        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la venta con ID " + id
                        )
                );

        if (venta.getEstado() != EstadoVenta.CONFIRMADA) {
            throw new IllegalArgumentException(
                    "Solamente se pueden cancelar ventas confirmadas"
            );
        }

        Usuario usuarioActual =
                usuarioActualService.obtenerUsuarioActual();

        BigDecimal ajusteCuentaCorriente =
                obtenerAjusteCuentaCorriente(venta);

        Cliente clienteCuenta = null;

        if (ajusteCuentaCorriente.compareTo(BigDecimal.ZERO) != 0) {

            if (venta.getCliente() == null) {
                throw new IllegalArgumentException(
                        "La venta no tiene un cliente asociado para revertir la cuenta corriente"
                );
            }

            clienteCuenta = clienteRepository
                    .buscarPorIdParaActualizar(
                            venta.getCliente().getId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El cliente asociado a la venta ya no existe"
                            )
                    );
        }
        /*
         * Si la venta había ingresado dinero,
         * descontamos su importe de la caja.
         */
        if (venta.getFormaPago()
                != FormaPago.CUENTA_CORRIENTE
                && venta.getCaja() != null) {

            if (venta.getFormaPago() == FormaPago.COMBINADO
                    && venta.getPagos() != null
                    && !venta.getPagos().isEmpty()) {

                for (PagoVenta pago : venta.getPagos()) {
                    if (pago.getFormaPago()
                            == FormaPago.CUENTA_CORRIENTE) {
                        continue;
                    }

                    cajaService.descontarVenta(
                            venta.getCaja().getId(),
                            pago.getFormaPago(),
                            pago.getMonto()
                    );
                }

            } else {
                cajaService.descontarVenta(
                        venta.getCaja().getId(),
                        venta.getFormaPago(),
                        venta.getTotal()
                );
            }
        }

        /*
         * Devolvemos al stock todos los productos
         * incluidos en la venta.
         */
        for (DetalleVenta detalle : venta.getDetalles()) {

            Producto producto = productoRepository
                    .buscarPorIdParaActualizar(
                            detalle.getProducto().getId()
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "El producto seleccionado no existe"
                            )
                    );

            BigDecimal stockAnterior =
                    producto.getStockActual() == null
                            ? BigDecimal.ZERO
                            : producto.getStockActual();

            BigDecimal stockPosterior = stockAnterior.add(
                    detalle.getCantidad()
            );

            producto.setStockActual(stockPosterior);

            productoRepository.save(producto);

            /*
             * Registramos el movimiento de devolución.
             */
            MovimientoStock movimiento = new MovimientoStock();

            movimiento.setFecha(LocalDateTime.now());
            movimiento.setProducto(producto);
            movimiento.setTipo(
                    TipoMovimientoStock.ENTRADA_ANULACION_VENTA
            );
            movimiento.setCantidad(detalle.getCantidad());
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockPosterior(stockPosterior);
            movimiento.setCompra(null);
            movimiento.setVenta(venta);
            movimiento.setUsuario(usuarioActual);
            movimiento.setObservacion(
                    "Devolución de stock por cancelación de venta #"
                            + venta.getId()
            );

            movimientoStockRepository.save(movimiento);
        }

        /*
         * Si fue una venta a cuenta corriente,
         * quitamos del saldo el total generado por la venta.
         *
         * También funciona cuando el cliente
         * tiene dinero a favor.
         */
        /*
         * Quitamos del saldo el importe de la venta
         * y registramos la cancelación.
         */
        if (clienteCuenta != null) {

            BigDecimal saldoAnterior =
                    clienteCuenta.getSaldoCuentaCorriente() == null
                            ? BigDecimal.ZERO
                            : clienteCuenta.getSaldoCuentaCorriente();

            saldoAnterior = saldoAnterior.setScale(
                    2,
                    RoundingMode.HALF_UP
            );

            BigDecimal saldoNuevo = saldoAnterior
                    .subtract(ajusteCuentaCorriente)
                    .setScale(2, RoundingMode.HALF_UP);

            clienteCuenta.setSaldoCuentaCorriente(
                    saldoNuevo
            );

            clienteRepository.save(clienteCuenta);

            MovimientoCuentaCorriente movimiento =
                    new MovimientoCuentaCorriente();

            movimiento.setFecha(LocalDateTime.now());
            movimiento.setCliente(clienteCuenta);
            movimiento.setTipo(
                    TipoMovimientoCuentaCorriente
                            .CANCELACION_VENTA
            );
            movimiento.setImporte(
                    ajusteCuentaCorriente.abs()
            );
            movimiento.setSaldoAnterior(saldoAnterior);
            movimiento.setSaldoPosterior(saldoNuevo);
            movimiento.setVenta(venta);
            movimiento.setPagoCliente(null);
            movimiento.setDescripcion(
                    "Cancelacion del ajuste de cuenta corriente de la venta #"
                            + venta.getId()
            );

            movimientoCuentaCorrienteRepository.save(
                    movimiento
            );
        }

        venta.setEstado(EstadoVenta.CANCELADA);

        return ventaRepository.save(venta);
    }

    private void validarVentaRecibida(Venta venta) {

        if (venta == null) {
            throw new IllegalArgumentException(
                    "La venta no puede ser nula"
            );
        }

        if (venta.getFormaPago() == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una forma de pago"
            );
        }

        if (venta.getDetalles() == null
                || venta.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe agregar al menos un producto a la venta"
            );
        }
    }

    private void prepararPagos(
            Venta venta,
            BigDecimal total
    ) {

        if (venta.getFormaPago() == FormaPago.CUENTA_CORRIENTE) {
            venta.setPagos(new ArrayList<>());
            return;
        }

        if (venta.getFormaPago() != FormaPago.COMBINADO) {

            PagoVenta pago = crearPago(
                    venta,
                    venta.getFormaPago(),
                    total
            );

            venta.setPagos(
                    new ArrayList<>(List.of(pago))
            );
            return;
        }

        List<PagoVenta> pagosRecibidos =
                venta.getPagos() == null
                        ? List.of()
                        : venta.getPagos();

        List<PagoVenta> pagosNormalizados =
                new ArrayList<>();

        Set<FormaPago> formasAgregadas =
                new HashSet<>();

        BigDecimal totalPagos = BigDecimal.ZERO;

        for (PagoVenta pagoRecibido : pagosRecibidos) {

            if (pagoRecibido == null
                    || pagoRecibido.getMonto() == null
                    || pagoRecibido.getMonto()
                    .compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            FormaPago forma = pagoRecibido.getFormaPago();

            if (!esMedioDeCobro(forma)) {
                throw new IllegalArgumentException(
                        "El medio incluido en el pago combinado no es valido"
                );
            }

            BigDecimal monto = pagoRecibido.getMonto()
                    .setScale(2, RoundingMode.HALF_UP);

            if (monto.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Los importes del pago combinado no pueden ser negativos"
                );
            }

            if (!formasAgregadas.add(forma)) {
                throw new IllegalArgumentException(
                        "No se puede repetir un medio en el pago combinado"
                );
            }

            pagosNormalizados.add(
                    crearPago(venta, forma, monto)
            );
            totalPagos = totalPagos.add(monto);
        }

        totalPagos = totalPagos.setScale(
                2,
                RoundingMode.HALF_UP
        );

        BigDecimal ajusteCuentaCorriente = total
                .subtract(totalPagos)
                .setScale(2, RoundingMode.HALF_UP);

        if (ajusteCuentaCorriente.compareTo(BigDecimal.ZERO) != 0) {
            if (venta.getCliente() == null) {
                throw new IllegalArgumentException(
                        "La suma de los pagos (" + totalPagos.toPlainString()
                                + ") debe coincidir con el total de la venta ("
                                + total.toPlainString()
                                + ") o debe seleccionar un cliente para usar su cuenta corriente"
                );
            }

            if (pagosNormalizados.isEmpty()) {
                throw new IllegalArgumentException(
                        "Use cuenta corriente como forma de pago cuando no hay otro medio de cobro"
                );
            }

            pagosNormalizados.add(
                    crearPago(
                            venta,
                            FormaPago.CUENTA_CORRIENTE,
                            ajusteCuentaCorriente
                    )
            );
        } else if (pagosNormalizados.size() < 2) {
            throw new IllegalArgumentException(
                    "El pago combinado debe utilizar al menos dos medios"
            );
        }

        venta.setPagos(pagosNormalizados);
    }

    private PagoVenta crearPago(
            Venta venta,
            FormaPago formaPago,
            BigDecimal monto
    ) {

        PagoVenta pago = new PagoVenta();
        pago.setVenta(venta);
        pago.setFormaPago(formaPago);
        pago.setMonto(
                monto.setScale(2, RoundingMode.HALF_UP)
        );

        return pago;
    }

    private boolean esMedioDeCobro(FormaPago formaPago) {

        return formaPago == FormaPago.EFECTIVO
                || formaPago == FormaPago.TRANSFERENCIA
                || formaPago == FormaPago.TARJETA_DEBITO
                || formaPago == FormaPago.TARJETA_CREDITO;
    }

    private BigDecimal obtenerAjusteCuentaCorriente(Venta venta) {

        if (venta.getFormaPago() == FormaPago.CUENTA_CORRIENTE) {
            return venta.getTotal().setScale(2, RoundingMode.HALF_UP);
        }

        if (venta.getFormaPago() != FormaPago.COMBINADO
                || venta.getPagos() == null) {
            return BigDecimal.ZERO.setScale(2);
        }

        return venta.getPagos().stream()
                .filter(pago -> pago.getFormaPago()
                        == FormaPago.CUENTA_CORRIENTE)
                .map(PagoVenta::getMonto)
                .findFirst()
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String descripcionAjusteCuentaCorriente(
            Venta venta,
            BigDecimal ajuste
    ) {

        if (venta.getFormaPago() == FormaPago.CUENTA_CORRIENTE) {
            return "Venta a cuenta corriente #" + venta.getId();
        }

        if (ajuste.compareTo(BigDecimal.ZERO) > 0) {
            return "Deuda generada por pago combinado en venta #"
                    + venta.getId();
        }

        return "Saldo a favor generado por pago combinado en venta #"
                + venta.getId();
    }

    private Producto buscarProductoValido(
            Producto productoRecibido
    ) {

        if (productoRecibido == null
                || productoRecibido.getId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un producto"
            );
        }

        Producto producto = productoRepository
                .findById(productoRecibido.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El producto seleccionado no existe"
                        )
                );

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new IllegalArgumentException(
                    "El producto " + producto.getNombre()
                            + " está inactivo"
            );
        }

        return producto;
    }

    private void validarCantidad(BigDecimal cantidad) {

        if (cantidad == null) {
            throw new IllegalArgumentException(
                    "La cantidad es obligatoria"
            );
        }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero"
            );
        }
    }

    private String normalizarNombreCliente(String nombreCliente) {

        if (nombreCliente == null
                || nombreCliente.isBlank()) {

            return "Consumidor final";
        }

        return nombreCliente.trim();
    }

    private String normalizarTextoOpcional(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
    @Transactional(readOnly = true)
    public Venta buscarCompletaPorId(Long id) {

        Venta venta = ventaRepository.buscarCompletaPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la venta con ID " + id
                        )
                );

        // Hibernate no permite cargar dos colecciones List en la misma consulta.
        venta.getPagos().size();

        return venta;
    }
    @Transactional(readOnly = true)
    public ReporteVentasDTO generarReporte(
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {

        if (fechaDesde == null || fechaHasta == null) {
            throw new IllegalArgumentException(
                    "Debe indicar ambas fechas"
            );
        }

        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        LocalDateTime desde = fechaDesde.atStartOfDay();

        LocalDateTime hasta = fechaHasta.atTime(
                LocalTime.MAX
        );

        List<Venta> ventas = ventaRepository
                .findByEstadoAndFechaBetweenOrderByFechaDesc(
                        EstadoVenta.CONFIRMADA,
                        desde,
                        hasta
                );

        BigDecimal totalVendido = ventas.stream()
                .map(Venta::getTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costoTotal = ventas.stream()
                .map(Venta::getCostoTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gananciaTotal = ventas.stream()
                .map(Venta::getGananciaTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReporteVentasDTO(
                ventas,
                totalVendido,
                costoTotal,
                gananciaTotal
        );
    }
    @Transactional(readOnly = true)
    public List<ProductoMasVendidoDTO> generarReporteProductosMasVendidos(
            LocalDate fechaDesde,
            LocalDate fechaHasta
    ) {

        if (fechaDesde == null || fechaHasta == null) {
            throw new IllegalArgumentException(
                    "Debe indicar ambas fechas"
            );
        }

        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }

        LocalDateTime desde = fechaDesde.atStartOfDay();

        LocalDateTime hasta = fechaHasta.atTime(
                LocalTime.MAX
        );

        return detalleVentaRepository.buscarProductosMasVendidos(
                EstadoVenta.CONFIRMADA,
                desde,
                hasta
        );
    }
    private Cliente buscarClienteParaVenta(
            Cliente clienteRecibido,
            FormaPago formaPago
    ) {

        boolean requiereCliente =
                formaPago == FormaPago.CUENTA_CORRIENTE;

        boolean puedeActualizarCuenta = requiereCliente
                || formaPago == FormaPago.COMBINADO;

        if (clienteRecibido == null
                || clienteRecibido.getId() == null) {

            if (requiereCliente) {
                throw new IllegalArgumentException(
                        "Debe seleccionar un cliente para vender a cuenta corriente"
                );
            }

            return null;
        }

        Cliente cliente = (puedeActualizarCuenta
                ? clienteRepository.buscarPorIdParaActualizar(
                        clienteRecibido.getId()
                )
                : clienteRepository.findById(
                        clienteRecibido.getId()
                ))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El cliente seleccionado no existe"
                        )
                );

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new IllegalArgumentException(
                    "El cliente seleccionado está inactivo"
            );
        }

        return cliente;
    }
}
