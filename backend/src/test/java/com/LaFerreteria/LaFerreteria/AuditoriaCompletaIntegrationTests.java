package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.entity.DetalleCompra;
import com.LaFerreteria.LaFerreteria.entity.DetalleVenta;
import com.LaFerreteria.LaFerreteria.entity.PagoCliente;
import com.LaFerreteria.LaFerreteria.entity.PagoVenta;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.EstadoCompra;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.MovimientoStockService;
import com.LaFerreteria.LaFerreteria.service.PagoClienteService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuditoriaCompletaIntegrationTests {

    @Autowired
    private CajaService cajaService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private PagoClienteService pagoClienteService;

    @Autowired
    private MovimientoStockService movimientoStockService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @BeforeEach
    void autenticarAdministrador() {

        SecurityContext contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "Test123*",
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMINISTRADOR"
                                )
                        )
                )
        );

        SecurityContextHolder.setContext(contexto);
    }

    @AfterEach
    void limpiarAutenticacion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ventasPorTodosLosMediosActualizanCajaReportesYRanking() {

        cajaService.abrirCaja();
        Producto producto = guardarProducto(
                "MEDIOS-001",
                "20.000",
                "3.000"
        );

        for (FormaPago formaPago : List.of(
                FormaPago.EFECTIVO,
                FormaPago.TRANSFERENCIA,
                FormaPago.TARJETA_DEBITO,
                FormaPago.TARJETA_CREDITO
        )) {
            ventaService.crear(
                    crearVenta(producto, "1.000", formaPago, null)
            );
        }

        Venta ventaCancelada = ventaService.crear(
                crearVenta(
                        producto,
                        "2.000",
                        FormaPago.EFECTIVO,
                        null
                )
        );
        ventaService.cancelar(ventaCancelada.getId());

        Caja caja = cajaService.buscarCajaAbierta().orElseThrow();

        assertMoney("150.00", caja.getVentasEfectivo());
        assertMoney("150.00", caja.getVentasTransferencia());
        assertMoney("150.00", caja.getVentasTarjetaDebito());
        assertMoney("150.00", caja.getVentasTarjetaCredito());
        assertMoney("600.00", caja.getTotalVentas());
        assertMoney("600.00", caja.getTotalGeneral());
        assertMoney(
                "16.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );

        var reporte = ventaService.generarReporte(
                LocalDate.now(),
                LocalDate.now()
        );

        assertEquals(4, reporte.cantidadVentas());
        assertMoney("600.00", reporte.totalVendido());
        assertMoney("400.00", reporte.costoTotal());
        assertMoney("200.00", reporte.gananciaTotal());

        var ranking = ventaService.generarReporteProductosMasVendidos(
                LocalDate.now(),
                LocalDate.now()
        );

        assertEquals(1, ranking.size());
        assertEquals(producto.getId(), ranking.getFirst().productoId());
        assertMoney("4.000", ranking.getFirst().cantidadVendida());
        assertEquals(4L, ranking.getFirst().cantidadVentas());
        assertMoney("600.00", ranking.getFirst().totalVendido());
    }

    @Test
    void cuentaCorrienteSinClienteEsRechazadaSinCrearVenta() {

        cajaService.abrirCaja();
        Producto producto = guardarProducto(
                "CTA-ERR-001",
                "8.000",
                "1.000"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.crear(
                        crearVenta(
                                producto,
                                "1.000",
                                FormaPago.CUENTA_CORRIENTE,
                                null
                        )
                )
        );

        assertTrue(error.getMessage().contains("cliente"));
        assertEquals(0, ventaRepository.count());
        assertMoney(
                "8.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void pagoCombinadoNoPermiteRepetirUnMedio() {

        cajaService.abrirCaja();
        Producto producto = guardarProducto(
                "COMB-DUP-001",
                "8.000",
                "1.000"
        );

        Venta venta = crearVenta(
                producto,
                "2.000",
                FormaPago.COMBINADO,
                null
        );

        PagoVenta primero = crearPagoVenta(
                FormaPago.EFECTIVO,
                "100.00"
        );
        PagoVenta segundo = crearPagoVenta(
                FormaPago.EFECTIVO,
                "200.00"
        );
        venta.setPagos(new ArrayList<>(List.of(primero, segundo)));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.crear(venta)
        );

        assertTrue(error.getMessage().contains("repetir"));
        assertEquals(0, ventaRepository.count());
    }

    @Test
    void cajaCerradaImpideCancelarVentasYAnularPagos() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto(
                "CIERRE-001",
                "5.000",
                "1.000"
        );
        Cliente cliente = guardarCliente(
                "CIERRE-CLIENTE",
                "100.00"
        );

        Venta venta = ventaService.crear(
                crearVenta(
                        producto,
                        "1.000",
                        FormaPago.EFECTIVO,
                        null
                )
        );

        PagoCliente pago = crearPagoCliente(
                MedioPago.TRANSFERENCIA,
                "20.00"
        );
        pago = pagoClienteService.registrarPago(cliente.getId(), pago);

        Caja cerrada = cajaService.cerrarCaja();

        assertEquals(caja.getId(), cerrada.getId());
        assertMoney("170.00", cerrada.getTotalGeneral());

        Long pagoId = pago.getId();
        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.cancelar(venta.getId())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> pagoClienteService.anularPago(
                        cliente.getId(),
                        pagoId,
                        "Prueba con caja cerrada"
                )
        );

        assertEquals(
                EstadoVenta.CONFIRMADA,
                ventaRepository.findById(venta.getId())
                        .orElseThrow()
                        .getEstado()
        );
        assertFalse(
                Boolean.TRUE.equals(
                        pagoClienteService.buscarPorId(pagoId)
                                .orElseThrow()
                                .getAnulado()
                )
        );
        assertMoney(
                "4.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertMoney(
                "80.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
    }

    @Test
    void compraPendientePuedeCancelarseSinModificarStock() {

        Producto producto = guardarProducto(
                "COMP-CANCEL-001",
                "5.000",
                "1.000"
        );
        Proveedor proveedor = guardarProveedor("20-11111111-1");

        Compra compra = compraService.crear(
                crearCompra(
                        producto,
                        proveedor,
                        "3.000",
                        "120.00"
                )
        );

        Compra cancelada = compraService.cancelar(compra.getId());

        assertEquals(EstadoCompra.CANCELADA, cancelada.getEstado());
        assertMoney(
                "5.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertEquals(0, movimientoStockRepository.count());
        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.confirmar(compra.getId())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.cancelar(compra.getId())
        );
    }

    @Test
    void ajustesManualesRegistranEntradaSalidaYEvitanStockNegativo() {

        Producto producto = guardarProducto(
                "AJUSTE-001",
                "5.000",
                "2.000"
        );

        movimientoStockService.ajustarStock(
                producto.getId(),
                TipoMovimientoStock.AJUSTE_ENTRADA,
                new BigDecimal("3.000"),
                "Conteo de ingreso"
        );
        movimientoStockService.ajustarStock(
                producto.getId(),
                TipoMovimientoStock.AJUSTE_SALIDA,
                new BigDecimal("4.000"),
                "Corrección de inventario"
        );

        assertMoney(
                "4.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertEquals(2, movimientoStockRepository.count());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> movimientoStockService.ajustarStock(
                        producto.getId(),
                        TipoMovimientoStock.AJUSTE_SALIDA,
                        new BigDecimal("5.000"),
                        "Salida inválida"
                )
        );

        assertTrue(error.getMessage().contains("stock negativo"));
        assertMoney(
                "4.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void pagosDeClientesAceptanTodosLosMediosYActualizanCaja() {

        cajaService.abrirCaja();
        Cliente cliente = guardarCliente(
                "PAGOS-MEDIOS",
                "100.00"
        );

        for (MedioPago medioPago : MedioPago.values()) {
            pagoClienteService.registrarPago(
                    cliente.getId(),
                    crearPagoCliente(medioPago, "10.00")
            );
        }

        Caja caja = cajaService.buscarCajaAbierta().orElseThrow();

        assertMoney("10.00", caja.getPagosClientesEfectivo());
        assertMoney("10.00", caja.getPagosClientesTransferencia());
        assertMoney("10.00", caja.getPagosClientesTarjetaDebito());
        assertMoney("10.00", caja.getPagosClientesTarjetaCredito());
        assertMoney("10.00", caja.getPagosClientesOtro());
        assertMoney("50.00", caja.getTotalPagosClientes());
        assertMoney("50.00", caja.getTotalGeneral());
        assertMoney(
                "50.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertEquals(
                MedioPago.values().length,
                pagoClienteService.listarPorCliente(cliente.getId()).size()
        );
    }

    @Test
    void stockBajoYValidacionesDeReporteRespondenCorrectamente() {

        Producto bajo = guardarProducto(
                "BAJO-001",
                "2.000",
                "3.000"
        );
        guardarProducto("NORMAL-001", "3.000", "3.000");

        List<Producto> productosBajos =
                productoService.listarConStockBajo();

        assertEquals(2, productosBajos.size());
        assertEquals(bajo.getId(), productosBajos.getFirst().getId());

        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.generarReporte(null, LocalDate.now())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.generarReporte(
                        LocalDate.now(),
                        LocalDate.now().minusDays(1)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.generarReporteProductosMasVendidos(
                        LocalDate.now(),
                        LocalDate.now().minusDays(1)
                )
        );
    }

    @Test
    void cajaRechazaImportesInvalidosYPagoCombinadoDirecto() {

        cajaService.abrirCaja();

        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarVenta(
                        FormaPago.EFECTIVO,
                        BigDecimal.ZERO
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarVenta(
                        FormaPago.COMBINADO,
                        BigDecimal.ONE
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarPagoCliente(
                        null,
                        BigDecimal.ONE
                )
        );

        Caja caja = cajaService.buscarCajaAbierta().orElseThrow();
        assertMoney("0.00", caja.getTotalGeneral());
    }

    private Producto guardarProducto(
            String codigo,
            String stock,
            String stockMinimo
    ) {

        Categoria categoria = new Categoria();
        categoria.setNombre("Categoria " + codigo);
        categoria.setActiva(true);
        categoria = categoriaRepository.save(categoria);

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre("Producto " + codigo);
        producto.setPrecioCompra(new BigDecimal("100.00"));
        producto.setPrecioVenta(new BigDecimal("150.00"));
        producto.setStockActual(new BigDecimal(stock));
        producto.setStockMinimo(new BigDecimal(stockMinimo));
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        producto.setCategoria(categoria);
        producto.setActivo(true);

        return productoRepository.save(producto);
    }

    private Cliente guardarCliente(
            String documento,
            String saldo
    ) {

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente " + documento);
        cliente.setDocumento(documento);
        cliente.setSaldoCuentaCorriente(new BigDecimal(saldo));
        cliente.setActivo(true);

        return clienteRepository.save(cliente);
    }

    private Proveedor guardarProveedor(String cuit) {

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Proveedor " + cuit);
        proveedor.setCuit(cuit);
        proveedor.setActivo(true);

        return proveedorRepository.save(proveedor);
    }

    private Venta crearVenta(
            Producto producto,
            String cantidad,
            FormaPago formaPago,
            Cliente cliente
    ) {

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(new BigDecimal(cantidad));

        Venta venta = new Venta();
        venta.setFormaPago(formaPago);
        venta.setCliente(cliente);
        venta.setDetalles(new ArrayList<>(List.of(detalle)));

        return venta;
    }

    private Compra crearCompra(
            Producto producto,
            Proveedor proveedor,
            String cantidad,
            String costoUnitario
    ) {

        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setCantidad(new BigDecimal(cantidad));
        detalle.setCostoUnitario(new BigDecimal(costoUnitario));

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setDetalles(new ArrayList<>(List.of(detalle)));

        return compra;
    }

    private PagoVenta crearPagoVenta(
            FormaPago formaPago,
            String monto
    ) {

        PagoVenta pago = new PagoVenta();
        pago.setFormaPago(formaPago);
        pago.setMonto(new BigDecimal(monto));
        return pago;
    }

    private PagoCliente crearPagoCliente(
            MedioPago medioPago,
            String monto
    ) {

        PagoCliente pago = new PagoCliente();
        pago.setMedioPago(medioPago);
        pago.setMonto(new BigDecimal(monto));
        return pago;
    }

    private void assertMoney(
            String esperado,
            BigDecimal actual
    ) {

        assertNotNull(actual);
        assertEquals(
                0,
                new BigDecimal(esperado).compareTo(actual)
        );
    }
}
