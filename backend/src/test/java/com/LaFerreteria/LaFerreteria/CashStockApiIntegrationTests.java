package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Caja;
import com.LaFerreteria.LaFerreteria.enums.EstadoCaja;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.service.CajaService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.caja.cierre-automatico.hora-aviso=00:00")
class CashStockApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private CajaService cajaService;

    @BeforeEach
    void cerrarCajaPreexistenteDentroDeLaTransaccion() {
        cajaRepository.findFirstByEstadoOrderByFechaAperturaDesc(
                        EstadoCaja.ABIERTA
                )
                .ifPresent(caja -> {
                    BigDecimal esperado = caja.getEfectivoEsperado();
                    caja.setEstado(EstadoCaja.CERRADA);
                    caja.setFechaCierre(LocalDateTime.now());
                    caja.setEfectivoContado(esperado);
                    caja.setDiferenciaEfectivo(BigDecimal.ZERO);
                    cajaRepository.save(caja);
                });
    }

    @Test
    void abreCajaConMontoCeroCuandoNoSeIndicaEfectivoInicial()
            throws Exception {
        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.arqueo.montoInicial").value(0.0))
                .andExpect(jsonPath("$.arqueo.efectivoEsperado").value(0.0));
    }

    @Test
    void administraCajaCompletaYConservaMotivoOpcional() throws Exception {
        MvcResult apertura = mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":100.00}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.arqueo.montoInicial").value(100.0))
                .andExpect(jsonPath("$.arqueo.efectivoEsperado").value(100.0))
                .andReturn();

        Number cajaId = JsonPath.read(
                apertura.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(
                        post("/api/cash/movements")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tipo": "RETIRO",
                                          "monto": 10.00,
                                          "motivo": ""
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arqueo.retiros").value(10.0))
                .andExpect(jsonPath("$.arqueo.efectivoEsperado").value(90.0))
                .andExpect(jsonPath("$.movimientos[0].motivo").value("Sin motivo"));

        mockMvc.perform(
                        post("/api/cash/close")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"efectivoContado\":85.00}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"))
                .andExpect(jsonPath("$.arqueo.efectivoContado").value(85.0))
                .andExpect(jsonPath("$.arqueo.diferencia").value(-5.0))
                .andExpect(jsonPath("$.cierreAutomatico").value(false))
                .andExpect(jsonPath("$.arqueoPendiente").value(false));

        mockMvc.perform(
                        get("/api/cash")
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actual").doesNotExist())
                .andExpect(jsonPath("$.cajas[?(@.id == %d)]", cajaId.longValue())
                        .isNotEmpty());

        mockMvc.perform(
                        get("/api/cash/{id}", cajaId.longValue())
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cajaId.longValue()))
                .andExpect(jsonPath("$.movimientos.length()").value(1))
                .andExpect(jsonPath("$.movimientos[0].tipo").value("RETIRO"));
    }

    @Test
    void cierraAutomaticamenteYExigeRegularizarAntesDeOtraApertura()
            throws Exception {
        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":100.00}")
                )
                .andExpect(status().isCreated());

        Caja abierta = cajaRepository
                .findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA)
                .orElseThrow();

        mockMvc.perform(
                        get("/api/cash/status")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cajaAbierta").value(true))
                .andExpect(jsonPath("$.avisoCierre").value(true))
                .andExpect(jsonPath("$.horaCierre").value("23:00"));

        LocalDateTime cierreProgramado = LocalDateTime.of(
                abierta.getFechaApertura().toLocalDate(),
                LocalTime.of(23, 0)
        );

        assertTrue(
                cajaService.cerrarCajaAutomaticamenteSiCorresponde(
                        cierreProgramado.minusSeconds(1),
                        LocalTime.of(23, 0)
                ).isEmpty()
        );
        assertTrue(
                cajaService.cerrarCajaAutomaticamenteSiCorresponde(
                        cierreProgramado.plusMinutes(1),
                        LocalTime.of(23, 0)
                ).isPresent()
        );

        Caja cerrada = cajaRepository.findById(abierta.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(cerrada.getCierreAutomatico()));
        assertTrue(cerrada.getEfectivoContado() == null);
        assertTrue(cerrada.getDiferenciaEfectivo() == null);

        mockMvc.perform(
                        get("/api/cash/status")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cajaAbierta").value(false))
                .andExpect(jsonPath("$.arqueoPendiente.id")
                        .value(abierta.getId()));

        mockMvc.perform(
                        get("/api/dashboard")
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cajaPendienteArqueo.id")
                        .value(abierta.getId()))
                .andExpect(jsonPath("$.cajaPendienteArqueo.efectivoEsperado")
                        .value(100.0));

        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":50.00}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("arqueo")));

        mockMvc.perform(
                        post("/api/cash/{id}/reconciliation", abierta.getId())
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"efectivoContado\":95.00}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/cash/{id}/reconciliation", abierta.getId())
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"efectivoContado\":95.00}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cierreAutomatico").value(true))
                .andExpect(jsonPath("$.arqueoPendiente").value(false))
                .andExpect(jsonPath("$.arqueo.efectivoContado").value(95.0))
                .andExpect(jsonPath("$.arqueo.diferencia").value(-5.0));

        assertFalse(cajaService.buscarArqueoPendiente().isPresent());

        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":50.00}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    void listaYFiltraLosMovimientosDeStock() throws Exception {
        String sufijo = String.valueOf(System.nanoTime());
        long categoriaId = crearCategoria("Categoria stock " + sufijo);
        long productoId = crearProducto(
                categoriaId,
                "MOV-" + sufijo,
                "Producto movimiento " + sufijo
        );

        mockMvc.perform(
                        post("/api/catalog/products/{id}/stock-adjustments", productoId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tipo": "AJUSTE_ENTRADA",
                                          "cantidad": 2.500,
                                          "observacion": "Conteo de prueba API"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(7.5));

        mockMvc.perform(
                        get("/api/stock-movements")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("query", "MOV-" + sufijo)
                                .param("type", "AJUSTE_ENTRADA")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.movimientos[0].codigo")
                        .value("MOV-" + sufijo))
                .andExpect(jsonPath("$.movimientos[0].tipo")
                        .value("AJUSTE_ENTRADA"))
                .andExpect(jsonPath("$.movimientos[0].stockAnterior").value(5.0))
                .andExpect(jsonPath("$.movimientos[0].stockPosterior").value(7.5))
                .andExpect(jsonPath("$.movimientos[0].referenciaTipo")
                        .value("AJUSTE"))
                .andExpect(jsonPath("$.metricas.ajustes")
                        .value(greaterThanOrEqualTo(1)));

        mockMvc.perform(
                        get("/api/stock-movements")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("from", "2026-08-10")
                                .param("to", "2026-08-01")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", containsString("fecha desde")));
    }

    @Test
    void vendedorAbreYCierraCajaPeroNoAdministraCajaNiStock()
            throws Exception {
        mockMvc.perform(
                        get("/api/cash")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":50}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.arqueo.montoInicial").value(50.0));

        mockMvc.perform(
                        post("/api/cash/movements")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tipo\":\"INGRESO\",\"monto\":10}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/cash/close")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"efectivoContado\":50}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        mockMvc.perform(
                        get("/api/stock-movements")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isForbidden());
    }

    private long crearCategoria(String nombre) throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/api/catalog/categories")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "nombre": "%s",
                                          "descripcion": "Categoria temporal"
                                        }
                                        """).formatted(nombre))
                )
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private long crearProducto(
            long categoriaId,
            String codigo,
            String nombre
    ) throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/api/catalog/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "codigo": "%s",
                                          "nombre": "%s",
                                          "descripcion": "Producto temporal",
                                          "marca": "Prueba API",
                                          "precioCompra": 100.00,
                                          "precioVenta": 150.00,
                                          "stockActual": 5.000,
                                          "stockMinimo": 2.000,
                                          "unidadMedida": "UNIDAD",
                                          "categoriaId": %d
                                        }
                                        """).formatted(codigo, nombre, categoriaId))
                )
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }
}
