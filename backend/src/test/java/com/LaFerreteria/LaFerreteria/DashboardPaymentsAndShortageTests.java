package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.dto.VentasPorMedioDTO;
import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.entity.PagoVenta;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.enums.EstadoVenta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import com.LaFerreteria.LaFerreteria.service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.caja.cierre-automatico.habilitado=false",
        "app.copias-seguridad.automaticas.habilitadas=false"
})
@AutoConfigureMockMvc
@Transactional
class DashboardPaymentsAndShortageTests {
    @Autowired MockMvc mvc;
    @Autowired VentaRepository ventas;
    @Autowired VentaService ventaService;
    @Autowired CajaRepository cajas;

    @BeforeEach
    void cerrarCajasPrevias() {
        cajas.findAll().forEach(caja -> {
            caja.setEstado(EstadoCaja.CERRADA);
            caja.setFechaCierre(LocalDate.now().minusDays(2).atTime(23, 0));
            caja.setEfectivoContado(caja.getEfectivoEsperado());
            caja.setDiferenciaEfectivo(BigDecimal.ZERO);
        });
    }

    @Test
    void separaSimplesCombinadosYCuentaCorrienteSinDuplicarPagos() {
        LocalDate dia = LocalDate.of(2025, 2, 10);
        venta(dia.atStartOfDay(), FormaPago.EFECTIVO, "100.10", false);
        venta(dia.atTime(9, 0), FormaPago.TRANSFERENCIA, "200.20", true);
        venta(dia.atTime(10, 0), FormaPago.TARJETA_DEBITO, "300.30", true);
        venta(dia.atTime(11, 0), FormaPago.TARJETA_CREDITO, "400.40", true);
        venta(dia.atTime(12, 0), FormaPago.CUENTA_CORRIENTE, "50.00", true);
        Venta combinado = venta(dia.atTime(13, 0), FormaPago.COMBINADO, "100.00", false);
        pago(combinado, FormaPago.EFECTIVO, "10.00");
        pago(combinado, FormaPago.TRANSFERENCIA, "20.00");
        pago(combinado, FormaPago.TARJETA_DEBITO, "30.00");
        pago(combinado, FormaPago.TARJETA_CREDITO, "40.00");
        Venta deuda = venta(dia.atTime(14, 0), FormaPago.COMBINADO, "100.00", false);
        pago(deuda, FormaPago.TRANSFERENCIA, "70.00");
        pago(deuda, FormaPago.CUENTA_CORRIENTE, "30.00");
        Venta favor = venta(dia.atTime(15, 0), FormaPago.COMBINADO, "100.00", false);
        pago(favor, FormaPago.EFECTIVO, "120.00");
        pago(favor, FormaPago.CUENTA_CORRIENTE, "-20.00");
        ventas.flush();

        VentasPorMedioDTO resumen = ventaService.resumirMediosDePagoDelDia(dia);
        dinero("230.10", resumen.efectivo());
        dinero("290.20", resumen.transferencia());
        dinero("330.30", resumen.debito());
        dinero("440.40", resumen.credito());
        dinero("60.00", resumen.cuentaCorriente());
        dinero("1291.00", resumen.totalCobrado());
    }

    @Test
    void excluyeAnuladasYOtrosDiasIncluyendoElLimiteDeMedianoche() {
        LocalDate dia = LocalDate.of(2025, 3, 10);
        venta(dia.minusDays(1).atTime(23, 59, 59), FormaPago.EFECTIVO, "900", true);
        venta(dia.plusDays(1).atStartOfDay(), FormaPago.TRANSFERENCIA, "800", true);
        venta(dia.atTime(8, 0), FormaPago.EFECTIVO, "700", true).setEstado(EstadoVenta.CANCELADA);
        Venta anulada = venta(dia.atTime(9, 0), FormaPago.COMBINADO, "100", false);
        anulada.setEstado(EstadoVenta.CANCELADA);
        pago(anulada, FormaPago.EFECTIVO, "50");
        pago(anulada, FormaPago.TARJETA_CREDITO, "50");
        venta(dia.atStartOfDay(), FormaPago.EFECTIVO, "1.25", true);
        venta(dia.atTime(23, 59, 59), FormaPago.TRANSFERENCIA, "2.75", false);
        ventas.flush();
        VentasPorMedioDTO resumen = ventaService.resumirMediosDePagoDelDia(dia);
        dinero("1.25", resumen.efectivo());
        dinero("2.75", resumen.transferencia());
        dinero("0", resumen.credito());
        dinero("4", resumen.totalCobrado());
    }

    @Test
    void diaSinVentasTieneCerosEnTodosLosMedios() {
        VentasPorMedioDTO resumen = ventaService.resumirMediosDePagoDelDia(LocalDate.of(2000, 1, 1));
        dinero("0", resumen.efectivo());
        dinero("0", resumen.transferencia());
        dinero("0", resumen.debito());
        dinero("0", resumen.credito());
        dinero("0", resumen.cuentaCorriente());
        dinero("0", resumen.totalCobrado());
    }

    @Test
    void inicioExponeCobrosDelDiaAunqueSeHayaCerradoLaCaja() throws Exception {
        Caja cerrada = cajaCerrada(LocalDateTime.now(), "100", "100");
        cerrada.setPagosClientesEfectivo(new BigDecimal("999"));
        cerrada.setMontoInicial(new BigDecimal("1000"));
        Venta venta = venta(LocalDateTime.now(), FormaPago.TARJETA_DEBITO, "42.50", true);
        venta.setCaja(cerrada);
        ventas.flush();
        mvc.perform(get("/api/dashboard").with(user("admin").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasPorMedio.debito").value(42.5))
                .andExpect(jsonPath("$.ventasPorMedio.efectivo").value(0))
                .andExpect(jsonPath("$.ventasPorMedio.totalCobrado").value(42.5));
    }

    @Test
    void informaFaltanteAnteriorAVendedorYNoAlteraNuevaApertura() throws Exception {
        Caja anterior = cajaCerrada(LocalDate.now().minusDays(1).atTime(20, 0), "1000", "875");
        mvc.perform(get("/api/cash/status").with(user("admin").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faltanteUltimoCierre.id").value(anterior.getId()))
                .andExpect(jsonPath("$.faltanteUltimoCierre.fechaCierre").value(anterior.getFechaCierre().toString() + ":00"))
                .andExpect(jsonPath("$.faltanteUltimoCierre.faltante").value(125))
                .andExpect(jsonPath("$.faltanteUltimoCierre.efectivoEsperado").value(1000))
                .andExpect(jsonPath("$.faltanteUltimoCierre.efectivoContado").value(875));
        mvc.perform(post("/api/cash/open").with(user("admin").roles("VENDEDOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.arqueo.montoInicial").value(0));
        mvc.perform(get("/api/cash/status").with(user("admin").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cajaAbierta").value(true))
                .andExpect(jsonPath("$.faltanteUltimoCierre").doesNotExist());
        dinero("-125", cajas.findById(anterior.getId()).orElseThrow().getDiferenciaEfectivo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "110"})
    void noArrastraFaltanteViejoSiUltimoCierreNoTieneFaltante(String contado) throws Exception {
        LocalDateTime cierre = LocalDate.now().atTime(20, 0);
        cajaCerrada(cierre, "100", "50");
        cajaCerrada(cierre, "100", contado);
        mvc.perform(get("/api/cash/status").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faltanteUltimoCierre").doesNotExist());
    }

    @Test
    void cierreAutomaticoSinContarNoInventaFaltante() throws Exception {
        Caja caja = cajaCerrada(LocalDateTime.now(), "100", "50");
        caja.setCierreAutomatico(true);
        caja.setEfectivoContado(null);
        caja.setDiferenciaEfectivo(null);
        cajas.flush();
        mvc.perform(get("/api/cash/status").with(user("admin").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arqueoPendiente.id").value(caja.getId()))
                .andExpect(jsonPath("$.faltanteUltimoCierre").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100"})
    void regularizaConCeroYCalculaElFaltanteSinRepetirElArqueo(String esperado) throws Exception {
        Caja caja = cajaCerrada(LocalDateTime.now(), esperado, "0");
        caja.setCierreAutomatico(true);
        caja.setEfectivoContado(null);
        caja.setDiferenciaEfectivo(null);
        cajas.flush();
        mvc.perform(post("/api/cash/{id}/reconciliation", caja.getId())
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arqueoPendiente").value(false))
                .andExpect(jsonPath("$.arqueo.efectivoContado").value(0))
                .andExpect(jsonPath("$.arqueo.diferencia").value(new BigDecimal(esperado).negate().doubleValue()));
        mvc.perform(get("/api/cash/status").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arqueoPendiente").doesNotExist());
        mvc.perform(post("/api/cash/{id}/reconciliation", caja.getId())
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":50}"))
                .andExpect(status().isBadRequest());
        dinero("0", cajas.findById(caja.getId()).orElseThrow().getEfectivoContado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"efectivoContado\":-1}", "{\"efectivoContado\":0.001}", "{}"})
    void importesInvalidosNoRegularizanLaCaja(String solicitud) throws Exception {
        Caja caja = cajaCerrada(LocalDateTime.now(), "100", "0");
        caja.setCierreAutomatico(true);
        caja.setEfectivoContado(null);
        caja.setDiferenciaEfectivo(null);
        cajas.flush();
        mvc.perform(post("/api/cash/{id}/reconciliation", caja.getId())
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(solicitud))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/cash/status").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arqueoPendiente.id").value(caja.getId()));
    }

    private Venta venta(LocalDateTime fecha, FormaPago forma, String total, boolean conPago) {
        Venta venta = new Venta();
        venta.setFecha(fecha);
        venta.setFormaPago(forma);
        venta.setTotal(new BigDecimal(total));
        if (conPago) pago(venta, forma, total);
        return ventas.save(venta);
    }

    private void pago(Venta venta, FormaPago forma, String monto) {
        PagoVenta pago = new PagoVenta();
        pago.setVenta(venta);
        pago.setFormaPago(forma);
        pago.setMonto(new BigDecimal(monto));
        venta.getPagos().add(pago);
    }

    private Caja cajaCerrada(LocalDateTime fecha, String esperado, String contado) {
        Caja caja = new Caja();
        caja.setFechaApertura(fecha.minusHours(8));
        caja.setFechaCierre(fecha);
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setEfectivoEsperado(new BigDecimal(esperado));
        caja.setEfectivoContado(new BigDecimal(contado));
        caja.setDiferenciaEfectivo(new BigDecimal(contado).subtract(new BigDecimal(esperado)));
        return cajas.saveAndFlush(caja);
    }

    private void dinero(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
