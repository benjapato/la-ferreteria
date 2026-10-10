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
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.enums.EstadoCompra;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCuentaCorrienteRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.LaFerreteria.LaFerreteria.service.CompraService;
import com.LaFerreteria.LaFerreteria.service.PagoClienteService;
import com.LaFerreteria.LaFerreteria.service.ProductoService;
import com.LaFerreteria.LaFerreteria.service.ReporteCajaService;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class OperacionesCriticasIntegrationTests {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private CajaService cajaService;

    @Autowired
    private PagoClienteService pagoClienteService;

    @Autowired
    private ReporteCajaService reporteCajaService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private MovimientoCuentaCorrienteRepository
            movimientoCuentaCorrienteRepository;

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
    void ventaEnEfectivoYCancelacionReviertenStockYCaja() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("MART-001", "10.000");

        Venta venta = ventaService.crear(
                crearVenta(
                        producto,
                        "2.000",
                        FormaPago.EFECTIVO,
                        null
                )
        );

        assertEquals(EstadoVenta.CONFIRMADA, venta.getEstado());
        assertMoney("300.00", venta.getTotal());
        assertMoney("200.00", venta.getCostoTotal());
        assertMoney("100.00", venta.getGananciaTotal());
        assertNotNull(venta.getUsuario());
        assertEquals(caja.getId(), venta.getCaja().getId());
        assertMoney(
                "8.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertMoney(
                "300.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getVentasEfectivo()
        );

        Venta cancelada = ventaService.cancelar(venta.getId());

        assertEquals(EstadoVenta.CANCELADA, cancelada.getEstado());
        assertMoney(
                "10.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
        assertEquals(2, movimientoStockRepository.count());
        assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.cancelar(venta.getId())
        );
    }

    @Test
    void ventaSinStockNoModificaDatos() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("TORN-001", "1.000");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.crear(
                        crearVenta(
                                producto,
                                "2.000",
                                FormaPago.EFECTIVO,
                                null
                        )
                )
        );

        assertTrue(error.getMessage().contains("Stock insuficiente"));
        assertEquals(0, ventaRepository.count());
        assertEquals(0, movimientoStockRepository.count());
        assertMoney(
                "1.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
    }

    @Test
    void pagoCombinadoDistribuyeImportesYSeRevierteAlCancelar() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("COMB-001", "10.000");

        Venta ventaNueva = crearVenta(
                producto,
                "2.000",
                FormaPago.COMBINADO,
                null
        );

        PagoVenta efectivo = new PagoVenta();
        efectivo.setFormaPago(FormaPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("100.00"));

        PagoVenta transferencia = new PagoVenta();
        transferencia.setFormaPago(FormaPago.TRANSFERENCIA);
        transferencia.setMonto(new BigDecimal("200.00"));

        ventaNueva.setPagos(
                new ArrayList<>(List.of(efectivo, transferencia))
        );

        Venta venta = ventaService.crear(ventaNueva);
        Caja actualizada = cajaService.buscarPorId(caja.getId())
                .orElseThrow();

        assertEquals(FormaPago.COMBINADO, venta.getFormaPago());
        assertEquals(2, venta.getPagos().size());
        assertMoney("100.00", actualizada.getVentasEfectivo());
        assertMoney("200.00", actualizada.getVentasTransferencia());
        assertMoney("300.00", actualizada.getTotalGeneral());

        var reporte = reporteCajaService.generarReporte(
                LocalDate.now(),
                LocalDate.now()
        );

        assertMoney("100.00", reporte.ventasEfectivo());
        assertMoney("200.00", reporte.ventasTransferencia());
        assertMoney("300.00", reporte.totalIngresosVentas());

        ventaService.cancelar(venta.getId());

        Caja revertida = cajaService.buscarPorId(caja.getId())
                .orElseThrow();

        assertMoney("0.00", revertida.getVentasEfectivo());
        assertMoney("0.00", revertida.getVentasTransferencia());
        assertMoney("0.00", revertida.getTotalGeneral());
        assertMoney(
                "10.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void pagoCombinadoInsuficienteGeneraDeudaYSeRevierteAlCancelar() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("COMB-DEUDA", "10.000");
        Cliente cliente = guardarCliente();

        Venta ventaNueva = crearVenta(
                producto,
                "2.000",
                FormaPago.COMBINADO,
                cliente
        );

        PagoVenta efectivo = new PagoVenta();
        efectivo.setFormaPago(FormaPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("100.00"));
        ventaNueva.setPagos(new ArrayList<>(List.of(efectivo)));

        Venta venta = ventaService.crear(ventaNueva);

        assertEquals(2, venta.getPagos().size());
        assertMoney(
                "200.00",
                venta.getPagos().stream()
                        .filter(pago -> pago.getFormaPago()
                                == FormaPago.CUENTA_CORRIENTE)
                        .findFirst()
                        .orElseThrow()
                        .getMonto()
        );
        assertMoney(
                "100.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
        assertMoney(
                "200.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );

        var reporte = reporteCajaService.generarReporte(
                LocalDate.now(),
                LocalDate.now()
        );

        assertMoney("100.00", reporte.ventasEfectivo());
        assertMoney("200.00", reporte.ventasCuentaCorriente());
        assertMoney("100.00", reporte.totalIngresosVentas());
        assertMoney("300.00", reporte.totalVentasFacturadas());

        ventaService.cancelar(venta.getId());

        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
        assertMoney(
                "0.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertEquals(2, movimientoCuentaCorrienteRepository.count());
    }

    @Test
    void pagoCombinadoSuperiorGeneraSaldoAFavorYSeRevierteAlCancelar() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("COMB-FAVOR", "10.000");
        Cliente cliente = guardarCliente();

        Venta ventaNueva = crearVenta(
                producto,
                "2.000",
                FormaPago.COMBINADO,
                cliente
        );

        PagoVenta efectivo = new PagoVenta();
        efectivo.setFormaPago(FormaPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("350.00"));
        ventaNueva.setPagos(new ArrayList<>(List.of(efectivo)));

        Venta venta = ventaService.crear(ventaNueva);

        assertMoney(
                "-50.00",
                venta.getPagos().stream()
                        .filter(pago -> pago.getFormaPago()
                                == FormaPago.CUENTA_CORRIENTE)
                        .findFirst()
                        .orElseThrow()
                        .getMonto()
        );
        assertMoney(
                "350.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
        assertMoney(
                "-50.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );

        var reporte = reporteCajaService.generarReporte(
                LocalDate.now(),
                LocalDate.now()
        );

        assertMoney("350.00", reporte.ventasEfectivo());
        assertMoney("-50.00", reporte.ventasCuentaCorriente());
        assertMoney("350.00", reporte.totalIngresosVentas());
        assertMoney("300.00", reporte.totalVentasFacturadas());

        ventaService.cancelar(venta.getId());

        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
        assertMoney(
                "0.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertEquals(2, movimientoCuentaCorrienteRepository.count());
    }

    @Test
    void pagoCombinadoConImporteIncorrectoNoModificaLaVenta() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("COMB-ERR", "10.000");

        Venta venta = crearVenta(
                producto,
                "2.000",
                FormaPago.COMBINADO,
                null
        );

        PagoVenta efectivo = new PagoVenta();
        efectivo.setFormaPago(FormaPago.EFECTIVO);
        efectivo.setMonto(new BigDecimal("100.00"));

        PagoVenta transferencia = new PagoVenta();
        transferencia.setFormaPago(FormaPago.TRANSFERENCIA);
        transferencia.setMonto(new BigDecimal("100.00"));

        venta.setPagos(
                new ArrayList<>(List.of(efectivo, transferencia))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ventaService.crear(venta)
        );

        assertTrue(error.getMessage().contains("coincidir con el total"));
        assertEquals(0, ventaRepository.count());
        assertMoney(
                "10.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );
    }

    @Test
    void detalleVentaCargaProductosYPagosSinConflictoDeColecciones() {

        cajaService.abrirCaja();
        Producto producto = guardarProducto("DET-001", "10.000");

        Venta creada = ventaService.crear(
                crearVenta(
                        producto,
                        "2.000",
                        FormaPago.EFECTIVO,
                        null
                )
        );

        entityManager.flush();
        entityManager.clear();

        Venta completa = ventaService.buscarCompletaPorId(creada.getId());

        assertEquals(1, completa.getDetalles().size());
        assertEquals(
                producto.getId(),
                completa.getDetalles().getFirst().getProducto().getId()
        );
        assertEquals(1, completa.getPagos().size());
        assertMoney("300.00", completa.getPagos().getFirst().getMonto());
    }

    @Test
    void productosSePaginanDeVeinteEnVeinte() {

        for (int numero = 1; numero <= 21; numero++) {
            guardarProducto(
                    "PAG-" + String.format("%02d", numero),
                    "1.000"
            );
        }

        var primeraPagina = productoService.buscar(
                "PAG-",
                PageRequest.of(
                        0,
                        20,
                        Sort.by("nombre").ascending()
                )
        );

        var segundaPagina = productoService.buscar(
                "PAG-",
                PageRequest.of(
                        1,
                        20,
                        Sort.by("nombre").ascending()
                )
        );

        assertEquals(20, primeraPagina.getNumberOfElements());
        assertEquals(21, primeraPagina.getTotalElements());
        assertEquals(2, primeraPagina.getTotalPages());
        assertEquals(1, segundaPagina.getNumberOfElements());
    }

    @Test
    void cuentaCorrientePagoYAnulacionMantienenSaldosConsistentes() {

        Caja caja = cajaService.abrirCaja();
        Producto producto = guardarProducto("PINT-001", "10.000");
        Cliente cliente = guardarCliente();

        Venta venta = ventaService.crear(
                crearVenta(
                        producto,
                        "2.000",
                        FormaPago.CUENTA_CORRIENTE,
                        cliente
                )
        );

        assertMoney(
                "300.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );

        PagoCliente pago = new PagoCliente();
        pago.setMonto(new BigDecimal("100.00"));
        pago.setMedioPago(MedioPago.EFECTIVO);

        PagoCliente pagoGuardado = pagoClienteService.registrarPago(
                cliente.getId(),
                pago
        );

        assertMoney(
                "200.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertMoney(
                "100.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );

        PagoCliente pagoAnulado = pagoClienteService.anularPago(
                cliente.getId(),
                pagoGuardado.getId(),
                ""
        );

        assertTrue(Boolean.TRUE.equals(pagoAnulado.getAnulado()));
        assertEquals(
                "Sin motivo",
                pagoAnulado.getMotivoAnulacion()
        );
        assertMoney(
                "300.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertMoney(
                "0.00",
                cajaService.buscarPorId(caja.getId())
                        .orElseThrow()
                        .getTotalGeneral()
        );

        ventaService.cancelar(venta.getId());

        assertMoney(
                "0.00",
                clienteRepository.findById(cliente.getId())
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertMoney(
                "10.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );
        assertEquals(4, movimientoCuentaCorrienteRepository.count());
    }

    @Test
    void confirmarCompraAumentaStockYActualizaCosto() {

        Producto producto = guardarProducto("TALAD-001", "5.000");
        Proveedor proveedor = guardarProveedor();

        Compra compra = compraService.crear(
                crearCompra(producto, proveedor, "3.000", "120.00")
        );

        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());
        assertMoney("360.00", compra.getTotal());
        assertMoney(
                "5.000",
                productoRepository.findById(producto.getId())
                        .orElseThrow()
                        .getStockActual()
        );

        Compra confirmada = compraService.confirmar(compra.getId());
        Producto actualizado = productoRepository
                .findById(producto.getId())
                .orElseThrow();

        assertEquals(EstadoCompra.CONFIRMADA, confirmada.getEstado());
        assertMoney("8.000", actualizado.getStockActual());
        assertMoney("120.00", actualizado.getPrecioCompra());
        assertEquals(1, movimientoStockRepository.count());
        assertEquals(
                TipoMovimientoStock.ENTRADA_COMPRA,
                movimientoStockRepository.findAll().getFirst().getTipo()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> compraService.confirmar(compra.getId())
        );
    }

    @Test
    void cajaNoPermiteDosAperturasNiOperacionesDespuesDelCierre() {

        Caja caja = cajaService.abrirCaja();

        assertThrows(
                IllegalArgumentException.class,
                cajaService::abrirCaja
        );

        cajaService.registrarVenta(
                FormaPago.TRANSFERENCIA,
                new BigDecimal("250.00")
        );

        Caja cerrada = cajaService.cerrarCaja();

        assertEquals(caja.getId(), cerrada.getId());
        assertEquals(EstadoCaja.CERRADA, cerrada.getEstado());
        assertNotNull(cerrada.getFechaCierre());
        assertMoney("250.00", cerrada.getTotalVentas());
        assertMoney("250.00", cerrada.getTotalGeneral());
        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarPagoCliente(
                        MedioPago.EFECTIVO,
                        new BigDecimal("10.00")
                )
        );
    }

    private Producto guardarProducto(
            String codigo,
            String stock
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
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        producto.setCategoria(categoria);
        producto.setActivo(true);

        return productoRepository.save(producto);
    }

    private Cliente guardarCliente() {

        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente de prueba");
        cliente.setDocumento("TEST-CLIENTE");
        cliente.setSaldoCuentaCorriente(BigDecimal.ZERO);
        cliente.setActivo(true);

        return clienteRepository.save(cliente);
    }

    private Proveedor guardarProveedor() {

        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Proveedor de prueba");
        proveedor.setCuit("20-12345678-9");
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
