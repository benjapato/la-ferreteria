package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.MovimientoCaja;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.enums.MedioPago;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoCaja;
import com.LaFerreteria.LaFerreteria.service.CajaService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ArqueoCajaIntegrationTests {

    @Autowired
    private CajaService cajaService;

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
    void arqueoCalculaEfectivoEsperadoYDiferencia() {

        Caja caja = cajaService.abrirCaja(
                new BigDecimal("1000.00")
        );

        cajaService.registrarVenta(
                FormaPago.EFECTIVO,
                new BigDecimal("500.00")
        );
        cajaService.registrarVenta(
                FormaPago.TRANSFERENCIA,
                new BigDecimal("900.00")
        );
        cajaService.registrarPagoCliente(
                MedioPago.EFECTIVO,
                new BigDecimal("100.00")
        );
        cajaService.registrarMovimiento(
                TipoMovimientoCaja.INGRESO,
                new BigDecimal("200.00"),
                "Cambio adicional"
        );
        cajaService.registrarMovimiento(
                TipoMovimientoCaja.RETIRO,
                new BigDecimal("150.00"),
                "Pago de flete"
        );

        Caja abierta = cajaService.buscarPorId(caja.getId())
                .orElseThrow();

        assertEquals(
                new BigDecimal("1650.00"),
                abierta.getEfectivoEsperado()
        );
        assertEquals(
                new BigDecimal("200.00"),
                abierta.getIngresosEfectivo()
        );
        assertEquals(
                new BigDecimal("150.00"),
                abierta.getRetirosEfectivo()
        );

        List<MovimientoCaja> movimientos =
                cajaService.listarMovimientos(caja.getId());

        assertEquals(2, movimientos.size());
        assertNotNull(movimientos.getFirst().getUsuario());

        Caja cerrada = cajaService.cerrarCaja(
                new BigDecimal("1600.00")
        );

        assertEquals(EstadoCaja.CERRADA, cerrada.getEstado());
        assertEquals(
                new BigDecimal("1600.00"),
                cerrada.getEfectivoContado()
        );
        assertEquals(
                new BigDecimal("-50.00"),
                cerrada.getDiferenciaEfectivo()
        );
    }

    @Test
    void validaImportesYRetirosYAdmiteMotivoVacio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.abrirCaja(
                        new BigDecimal("-0.01")
                )
        );

        cajaService.abrirCaja(new BigDecimal("100.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarMovimiento(
                        TipoMovimientoCaja.INGRESO,
                        BigDecimal.ZERO,
                        "Ingreso"
                )
        );
        MovimientoCaja sinMotivo =
                cajaService.registrarMovimiento(
                        TipoMovimientoCaja.INGRESO,
                        BigDecimal.ONE,
                        "   "
                );

        assertEquals(
                "Sin motivo",
                sinMotivo.getMotivo()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarMovimiento(
                        TipoMovimientoCaja.RETIRO,
                        new BigDecimal("101.01"),
                        "Retiro excesivo"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.cerrarCaja(
                        new BigDecimal("-1.00")
                )
        );
    }

    @Test
    void cajaCerradaNoAceptaNuevosMovimientos() {

        cajaService.abrirCaja(new BigDecimal("250.00"));
        Caja cerrada = cajaService.cerrarCaja(
                new BigDecimal("275.00")
        );

        assertEquals(
                new BigDecimal("25.00"),
                cerrada.getDiferenciaEfectivo()
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> cajaService.registrarMovimiento(
                        TipoMovimientoCaja.INGRESO,
                        new BigDecimal("10.00"),
                        "Ingreso tardio"
                )
        );

        assertEquals(
                "Debe abrir la caja antes de realizar esta operación",
                error.getMessage()
        );
    }
}
