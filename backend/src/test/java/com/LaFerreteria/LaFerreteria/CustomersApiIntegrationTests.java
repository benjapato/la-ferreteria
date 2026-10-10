package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Cliente;
import com.LaFerreteria.LaFerreteria.entity.Usuario;
import com.LaFerreteria.LaFerreteria.enums.RolUsuario;
import com.LaFerreteria.LaFerreteria.repository.CajaRepository;
import com.LaFerreteria.LaFerreteria.repository.ClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoCuentaCorrienteRepository;
import com.LaFerreteria.LaFerreteria.repository.PagoClienteRepository;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomersApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CajaRepository cajaRepository;

    @Autowired
    private PagoClienteRepository pagoClienteRepository;

    @Autowired
    private MovimientoCuentaCorrienteRepository movimientoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void crearUsuariosAuditables() {
        guardarUsuario("Administrador", "admin", RolUsuario.ADMINISTRADOR);
        guardarUsuario("Cajero", "cajero", RolUsuario.VENDEDOR);
    }

    @Test
    void administraYFiltraClientesSinModificarSaldoDesdeEdicion()
            throws Exception {
        long clienteId = crearCliente(
                "Cliente Ferreteria",
                "20-12345678-9"
        );
        fijarSaldo(clienteId, "850.00");

        mockMvc.perform(
                        put("/api/customers/{id}", clienteId)
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "nombre": "Cliente Actualizado",
                                          "documento": "20-12345678-9",
                                          "telefono": "1122334455",
                                          "email": "cliente@test.local",
                                          "direccion": "Calle 10"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cliente Actualizado"))
                .andExpect(jsonPath("$.saldo").value(850.0))
                .andExpect(jsonPath("$.estadoSaldo").value("DEUDA"));

        mockMvc.perform(
                        get("/api/customers")
                                .with(user("cajero").roles("VENDEDOR"))
                                .param("query", "Actualizado")
                                .param("status", "ACTIVOS")
                                .param("balance", "DEUDA")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.metricas.conDeuda").value(1))
                .andExpect(jsonPath("$.metricas.deudaTotal").value(850.0));

        mockMvc.perform(
                        post("/api/customers/{id}/status", clienteId)
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        assertDecimal(
                "850.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
    }

    @Test
    void pagoYAnulacionActualizanSaldoCajaYMovimientos()
            throws Exception {
        long clienteId = crearCliente("Cliente con deuda", "D-100");
        fijarSaldo(clienteId, "1000.00");

        mockMvc.perform(
                        post("/api/sales/cash/open")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"montoInicial\": 100.00}")
                )
                .andExpect(status().isCreated());

        MvcResult pagoCreado = mockMvc.perform(
                        post("/api/customers/{id}/payments", clienteId)
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "monto": 400.00,
                                          "medioPago": "EFECTIVO",
                                          "observacion": "Pago parcial"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoAnterior").value(1000.0))
                .andExpect(jsonPath("$.saldoPosterior").value(600.0))
                .andExpect(jsonPath("$.anulado").value(false))
                .andReturn();

        Number pagoId = JsonPath.read(
                pagoCreado.getResponse().getContentAsString(),
                "$.id"
        );

        assertDecimal(
                "600.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertDecimal(
                "400.00",
                cajaRepository.findAll().getFirst()
                        .getTotalPagosClientes()
        );
        assertEquals(1, movimientoRepository.count());

        mockMvc.perform(
                        get("/api/customers/{id}/movements", clienteId)
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.movimientos[0].tipo").value("PAGO_CLIENTE"))
                .andExpect(jsonPath("$.movimientos[0].pago.id").value(pagoId));

        mockMvc.perform(
                        post(
                                "/api/customers/{clienteId}/payments/{pagoId}/cancel",
                                clienteId,
                                pagoId.longValue()
                        )
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post(
                                "/api/customers/{clienteId}/payments/{pagoId}/cancel",
                                clienteId,
                                pagoId.longValue()
                        )
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"motivo\":\"\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anulado").value(true))
                .andExpect(jsonPath("$.motivoAnulacion").value("Sin motivo"));

        assertDecimal(
                "1000.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertDecimal(
                "0.00",
                cajaRepository.findAll().getFirst()
                        .getTotalPagosClientes()
        );
        assertEquals(2, movimientoRepository.count());
        assertEquals(true, pagoClienteRepository.findById(
                pagoId.longValue()
        ).orElseThrow().getAnulado());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rechazaPagoSinCajaAbiertaSinCambiarSaldo() throws Exception {
        long clienteId = crearCliente("Cliente sin caja", "D-200");
        fijarSaldo(clienteId, "500.00");

        mockMvc.perform(
                        post("/api/customers/{id}/payments", clienteId)
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "monto": 100.00,
                                          "medioPago": "TRANSFERENCIA"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "abrir la caja"
                        )
                ));

        assertDecimal(
                "500.00",
                clienteRepository.findById(clienteId)
                        .orElseThrow()
                        .getSaldoCuentaCorriente()
        );
        assertEquals(0, pagoClienteRepository.count());
        assertEquals(0, movimientoRepository.count());
        clienteRepository.deleteById(clienteId);
    }

    private long crearCliente(String nombre, String documento)
            throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/api/customers")
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(("""
                                        {
                                          "nombre": "%s",
                                          "documento": "%s",
                                          "telefono": "",
                                          "email": "",
                                          "direccion": ""
                                        }
                                        """).formatted(nombre, documento))
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private void fijarSaldo(long clienteId, String saldo) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow();
        cliente.setSaldoCuentaCorriente(new BigDecimal(saldo));
        clienteRepository.saveAndFlush(cliente);
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }

    private void guardarUsuario(
            String nombre,
            String nombreUsuario,
            RolUsuario rol
    ) {
        if (usuarioRepository.existsByNombreUsuarioIgnoreCase(nombreUsuario)) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(nombre);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setContrasena("test");
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
    }
}
