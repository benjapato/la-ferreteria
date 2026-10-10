package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SalesApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void flujoCompletoCreaCatalogosAbreCajaYRegistraVenta()
            throws Exception {
        long categoriaId = crearCategoria("Herramientas API");
        long productoId = crearProducto(categoriaId, "API-001");
        long clienteId = crearCliente("Cliente API", "DOC-API");

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"montoInicial": 500.00}
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montoInicial").value(500.0));

        mockMvc.perform(
                        get("/api/sales/setup")
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[0].codigo").value("API-001"))
                .andExpect(jsonPath("$.clientes[0].nombre").value("Cliente API"))
                .andExpect(jsonPath("$.caja.id").exists());

        mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "clienteId": %d,
                                          "formaPago": "EFECTIVO",
                                          "observaciones": "Entregar con comprobante",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 2.000}
                                          ],
                                          "pagos": []
                                        }
                                        """).formatted(clienteId, productoId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cliente").value("Cliente API"))
                .andExpect(jsonPath("$.documentoCliente").value("DOC-API"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.observaciones").value("Entregar con comprobante"))
                .andExpect(jsonPath("$.vendedor").value("Administrador Test"))
                .andExpect(jsonPath("$.cajaId").isNumber())
                .andExpect(jsonPath("$.total").value(300.0))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(2.0));

        assertEquals(1, ventaRepository.count());
        assertDecimal(
                "8.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
        assertDecimal(
                "300.00",
                cajaRepository.findAll().getFirst().getTotalGeneral()
        );
    }

    @Test
    void pagoCombinadoIncorrectoDevuelveErrorSinCrearVenta()
            throws Exception {
        long categoriaId = crearCategoria("Combinados API");
        long productoId = crearProducto(categoriaId, "API-COMB");

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"montoInicial": 0.00}
                                        """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "formaPago": "COMBINADO",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 2.000}
                                          ],
                                          "pagos": [
                                            {"formaPago": "EFECTIVO", "monto": 100.00},
                                            {"formaPago": "TRANSFERENCIA", "monto": 100.00}
                                          ]
                                        }
                                        """).formatted(productoId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "debe coincidir con el total"
                        )
                ));

        assertEquals(0, ventaRepository.count());
        assertDecimal(
                "10.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void pagoCombinadoConClienteCompletaLaDiferenciaEnCuentaCorriente()
            throws Exception {
        long categoriaId = crearCategoria("Cuenta combinada API");
        long productoId = crearProducto(categoriaId, "API-CTA-COMB");
        long clienteId = crearCliente("Cliente Cuenta API", "DOC-CTA-API");

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\": 0}")
                )
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "clienteId": %d,
                                          "formaPago": "COMBINADO",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 2.000}
                                          ],
                                          "pagos": [
                                            {"formaPago": "EFECTIVO", "monto": 100.00}
                                          ]
                                        }
                                        """).formatted(clienteId, productoId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pagos[0].formaPago")
                        .value("EFECTIVO"))
                .andExpect(jsonPath("$.pagos[0].monto").value(100.0))
                .andExpect(jsonPath("$.pagos[1].formaPago")
                        .value("CUENTA_CORRIENTE"))
                .andExpect(jsonPath("$.pagos[1].monto").value(200.0))
                .andReturn();

        assertDecimal(
                "100.00",
                cajaRepository.findAll().getFirst().getTotalGeneral()
        );
        assertDecimal(
                "200.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );

        mockMvc.perform(
                        get("/api/reports/cash")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("from", LocalDate.now().toString())
                                .param("to", LocalDate.now().toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.ingresosVentas")
                        .value(100.0))
                .andExpect(jsonPath("$.resumen.ventasFacturadas")
                        .value(300.0))
                .andExpect(jsonPath("$.ventasPorMedio.cuentaCorriente")
                        .value(200.0))
                .andExpect(jsonPath("$.ventas[0].ingresoCaja")
                        .value(100.0));

        long saleId = numero(
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.id"
                )
        );

        mockMvc.perform(
                        post("/api/sales/{id}/cancel", saleId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk());

        assertDecimal(
                "0.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
    }

    @Test
    void historialDetalleYCancelacionRestauranStockYCaja()
            throws Exception {
        long categoriaId = crearCategoria("Historial API");
        long productoId = crearProducto(categoriaId, "API-HIST");
        long clienteId = crearCliente("Cliente Historial", "DOC-HIST");

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\": 0}")
                )
                .andExpect(status().isCreated());

        MvcResult saleResult = mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "clienteId": %d,
                                          "formaPago": "EFECTIVO",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 2.000}
                                          ],
                                          "pagos": []
                                        }
                                        """).formatted(clienteId, productoId))
                )
                .andExpect(status().isCreated())
                .andReturn();

        long saleId = numero(
                JsonPath.read(
                        saleResult.getResponse().getContentAsString(),
                        "$.id"
                )
        );

        mockMvc.perform(
                        get("/api/sales/history")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("number", String.valueOf(saleId))
                                .param("client", "Historial")
                                .param("status", "CONFIRMADA")
                                .param("payment", "EFECTIVO")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.ventas[0].id").value(saleId))
                .andExpect(jsonPath("$.ventas[0].cliente")
                        .value("Cliente Historial"));

        mockMvc.perform(
                        get("/api/sales/{id}", saleId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.detalles[0].codigo")
                        .value("API-HIST"))
                .andExpect(jsonPath("$.pagos[0].monto").value(300.0));

        mockMvc.perform(
                        post("/api/sales/{id}/cancel", saleId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertDecimal(
                "10.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
        assertDecimal(
                "0.00",
                cajaRepository.findAll().getFirst().getTotalGeneral()
        );
    }

    @Test
    void altaRapidaValidaStockPreciosYLimitaLaBusquedaPorNombre()
            throws Exception {
        long categoriaId = crearCategoria("Busqueda rapida API");

        mockMvc.perform(
                        post("/api/sales/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "codigo": "API-STOCK-CERO",
                                          "nombre": "Producto sin stock",
                                          "precioCompra": 100.00,
                                          "precioVenta": 150.00,
                                          "stockActual": 0.000,
                                          "stockMinimo": 2.000,
                                          "unidadMedida": "UNIDAD",
                                          "categoriaId": %d
                                        }
                                        """).formatted(categoriaId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "mayor que cero"
                        )
                ));

        mockMvc.perform(
                        post("/api/sales/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "codigo": "API-MARGEN-NEG",
                                          "nombre": "Producto con margen negativo",
                                          "precioCompra": 200.00,
                                          "precioVenta": 150.00,
                                          "stockActual": 5.000,
                                          "stockMinimo": 2.000,
                                          "unidadMedida": "UNIDAD",
                                          "categoriaId": %d
                                        }
                                        """).formatted(categoriaId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "no puede ser menor"
                        )
                ));

        for (int number = 1; number <= 7; number++) {
            crearProducto(
                    categoriaId,
                    "API-LIM-" + number
            );
        }

        mockMvc.perform(
                        get("/api/sales/setup")
                                .with(user("admin").roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos.length()").value(6));

        mockMvc.perform(
                        get("/api/sales/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("query", "Producto API-LIM-7")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre")
                        .value("Producto API-LIM-7"));
    }

    @Test
    void vendedorNoPuedeCrearProductosNiAbrirCaja()
            throws Exception {
        mockMvc.perform(
                        post("/api/sales/products")
                                .with(user("vendedor").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("vendedor").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\": 0}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/sales/99/cancel")
                                .with(user("vendedor").roles("VENDEDOR"))
                                .with(csrf())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void vendedorNoRegistraVentasConLaCajaCerrada()
            throws Exception {
        long categoriaId = crearCategoria("Caja cerrada API");
        long productoId = crearProducto(categoriaId, "API-SIN-CAJA");

        mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "formaPago": "EFECTIVO",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 1}
                                          ],
                                          "pagos": []
                                        }
                                        """).formatted(productoId))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "Debe abrir la caja"
                        )
                ));

        assertEquals(0, ventaRepository.count());
        assertDecimal(
                "10.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void vendedorAbreCajaRegistraVentaYCierraCaja()
            throws Exception {
        long categoriaId = crearCategoria("Turno vendedor API");
        long productoId = crearProducto(categoriaId, "API-TURNO-VEND");

        mockMvc.perform(
                        post("/api/cash/open")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\":100}")
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/sales")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "formaPago": "EFECTIVO",
                                          "detalles": [
                                            {"productoId": %d, "cantidad": 1}
                                          ],
                                          "pagos": []
                                        }
                                        """).formatted(productoId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(150.0));

        mockMvc.perform(
                        post("/api/cash/close")
                                .with(user("admin").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"efectivoContado\":250}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADA"));

        assertEquals(1, ventaRepository.count());
        assertDecimal(
                "9.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
    }

    private long crearCategoria(String nombre) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/sales/categories")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {"nombre": "%s"}
                                        """).formatted(nombre))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return numero(
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.id"
                )
        );
    }

    private long crearProducto(
            long categoriaId,
            String codigo
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/sales/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "codigo": "%s",
                                          "nombre": "Producto %s",
                                          "precioCompra": 100.00,
                                          "precioVenta": 150.00,
                                          "stockActual": 10.000,
                                          "stockMinimo": 2.000,
                                          "unidadMedida": "UNIDAD",
                                          "categoriaId": %d
                                        }
                                        """).formatted(
                                        codigo,
                                        codigo,
                                        categoriaId
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return numero(
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.id"
                )
        );
    }

    private long crearCliente(
            String nombre,
            String documento
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/sales/clients")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "nombre": "%s",
                                          "documento": "%s"
                                        }
                                        """).formatted(nombre, documento))
                )
                .andExpect(status().isCreated())
                .andReturn();

        return numero(
                JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.id"
                )
        );
    }

    private long numero(Object valor) {
        return ((Number) valor).longValue();
    }

    private void assertDecimal(
            String esperado,
            BigDecimal actual
    ) {
        assertEquals(
                0,
                new BigDecimal(esperado).compareTo(actual)
        );
    }
}
