package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.entity.Compra;
import com.LaFerreteria.LaFerreteria.entity.DetalleCompra;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.enums.EstadoCompra;
import com.LaFerreteria.LaFerreteria.repository.CompraRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.LaFerreteria.LaFerreteria.entity.MovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioActualService usuarioActualService;

    public CompraService(
            CompraRepository compraRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            MovimientoStockRepository movimientoStockRepository,
            UsuarioActualService usuarioActualService
    ) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<Compra> listarTodas() {
        return compraRepository.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public Page<Compra> listarTodas(Pageable pageable) {
        return compraRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Compra> listarPendientes() {
        return compraRepository.findByEstadoOrderByFechaDesc(
                EstadoCompra.PENDIENTE
        );
    }

    @Transactional(readOnly = true)
    public Optional<Compra> buscarPorId(Long id) {
        return compraRepository.findById(id);
    }

    /*
     * Registra una compra como pendiente.
     * Todavía no modifica el stock.
     */
    @Transactional
    public Compra crear(Compra compra) {

        validarCompraRecibida(compra);

        Proveedor proveedor = buscarProveedorValido(
                compra.getProveedor()
        );

        compra.setId(null);
        compra.setProveedor(proveedor);
        compra.setFecha(LocalDateTime.now());
        compra.setEstado(EstadoCompra.PENDIENTE);
        compra.setUsuario(
                usuarioActualService.obtenerUsuarioActual()
        );
        compra.setObservaciones(
                normalizarTextoOpcional(compra.getObservaciones())
        );

        BigDecimal total = BigDecimal.ZERO;

        Set<Long> productosAgregados = new HashSet<>();

        for (DetalleCompra detalle : compra.getDetalles()) {

            Producto producto = buscarProductoValido(
                    detalle.getProducto()
            );

            if (!productosAgregados.add(producto.getId())) {
                throw new IllegalArgumentException(
                        "El producto " + producto.getNombre()
                                + " está repetido en la compra"
                );
            }

            validarCantidad(detalle.getCantidad());
            validarCostoUnitario(detalle.getCostoUnitario());

            BigDecimal subtotal = detalle.getCantidad()
                    .multiply(detalle.getCostoUnitario())
                    .setScale(2, RoundingMode.HALF_UP);

            detalle.setId(null);
            detalle.setCompra(compra);
            detalle.setProducto(producto);
            detalle.setSubtotal(subtotal);

            total = total.add(subtotal);
        }

        compra.setTotal(
                total.setScale(2, RoundingMode.HALF_UP)
        );

        return compraRepository.save(compra);
    }

    /*
     * Confirma la compra y aumenta el stock.
     */
    @Transactional
    public Compra confirmar(Long id) {

        Compra compra = compraRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la compra con ID " + id
                        )
                );

        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Solamente se pueden confirmar compras pendientes"
            );
        }

        if (compra.getDetalles() == null
                || compra.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "La compra no contiene productos"
            );
        }

        Usuario usuarioActual =
                usuarioActualService.obtenerUsuarioActual();

        for (DetalleCompra detalle : compra.getDetalles()) {

            Producto producto = productoRepository
                    .buscarPorIdParaActualizar(
                            detalle.getProducto().getId()
                    )
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

            BigDecimal stockAnterior =
                    producto.getStockActual() == null
                            ? BigDecimal.ZERO
                            : producto.getStockActual();

            BigDecimal stockNuevo = stockAnterior.add(
                    detalle.getCantidad()
            );

            /*
             * Actualizamos el producto.
             */
            producto.setStockActual(stockNuevo);

            producto.setPrecioCompra(
                    detalle.getCostoUnitario()
            );

            productoRepository.save(producto);

            /*
             * Registramos el movimiento de stock.
             */
            MovimientoStock movimiento = new MovimientoStock();

            movimiento.setFecha(LocalDateTime.now());
            movimiento.setProducto(producto);
            movimiento.setTipo(TipoMovimientoStock.ENTRADA_COMPRA);
            movimiento.setCantidad(detalle.getCantidad());
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockPosterior(stockNuevo);
            movimiento.setCompra(compra);
            movimiento.setUsuario(usuarioActual);
            movimiento.setObservacion(
                    "Entrada de mercadería por compra #"
                            + compra.getId()
            );

            movimientoStockRepository.save(movimiento);
        }

        compra.setEstado(EstadoCompra.CONFIRMADA);

        return compraRepository.save(compra);
    }

    /*
     * Solamente se pueden cancelar compras pendientes.
     */
    @Transactional
    public Compra cancelar(Long id) {

        Compra compra = compraRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la compra con ID " + id
                        )
                );

        if (compra.getEstado() != EstadoCompra.PENDIENTE) {
            throw new IllegalArgumentException(
                    "Solamente se pueden cancelar compras pendientes"
            );
        }

        compra.setEstado(EstadoCompra.CANCELADA);

        return compraRepository.save(compra);
    }

    private void validarCompraRecibida(Compra compra) {

        if (compra == null) {
            throw new IllegalArgumentException(
                    "La compra no puede ser nula"
            );
        }

        if (compra.getDetalles() == null
                || compra.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe agregar al menos un producto a la compra"
            );
        }
    }

    private Proveedor buscarProveedorValido(
            Proveedor proveedorRecibido
    ) {

        if (proveedorRecibido == null
                || proveedorRecibido.getId() == null) {

            throw new IllegalArgumentException(
                    "Debe seleccionar un proveedor"
            );
        }

        Proveedor proveedor = proveedorRepository
                .findById(proveedorRecibido.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El proveedor seleccionado no existe"
                        )
                );

        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new IllegalArgumentException(
                    "El proveedor seleccionado está inactivo"
            );
        }

        return proveedor;
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

    private void validarCostoUnitario(
            BigDecimal costoUnitario
    ) {

        if (costoUnitario == null) {
            throw new IllegalArgumentException(
                    "El costo unitario es obligatorio"
            );
        }

        if (costoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El costo unitario debe ser mayor que cero"
            );
        }
    }

    private String normalizarTextoOpcional(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
    @Transactional(readOnly = true)
    public Compra buscarCompletaPorId(Long id) {

        return compraRepository.buscarCompletaPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No existe la compra con ID " + id
                        )
                );
    }
}
