package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.service.CopiaSeguridadService;
import com.LaFerreteria.LaFerreteria.entity.Venta;
import com.LaFerreteria.LaFerreteria.enums.FormaPago;
import com.LaFerreteria.LaFerreteria.repository.UsuarioRepository;
import com.LaFerreteria.LaFerreteria.repository.VentaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.copias-seguridad.directorio=build/test-backups-api",
        "app.copias-seguridad.automaticas.habilitadas=false"
})
@AutoConfigureMockMvc
@Transactional
class SystemReportsApiIntegrationTests {

    private static final String BACKUP_PRUEBA = "api-prueba.backup";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CopiaSeguridadService copiaSeguridadService;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @AfterEach
    void limpiarArchivoTemporal() throws Exception {
        Files.deleteIfExists(
                copiaSeguridadService.obtenerDirectorioCopias()
                        .resolve(BACKUP_PRUEBA)
        );
    }

    @Test
    void administradorConsultaLosTresReportesYValidaElPeriodo() throws Exception {
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.of(2040, 1, 15, 10, 30));
        venta.setNombreCliente("Cliente reporte API");
        venta.setFormaPago(FormaPago.EFECTIVO);
        venta.setTotal(new BigDecimal("150.00"));
        venta.setCostoTotal(new BigDecimal("90.00"));
        venta.setGananciaTotal(new BigDecimal("60.00"));
        venta.setUsuario(usuarioRepository
                .findByNombreUsuarioIgnoreCase("admin")
                .orElse(null));
        ventaRepository.save(venta);

        mockMvc.perform(get("/api/reports/sales")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .param("from", "2040-01-15")
                        .param("to", "2040-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo.desde").value("2040-01-15"))
                .andExpect(jsonPath("$.metricas.cantidad").value(1))
                .andExpect(jsonPath("$.metricas.ganancia").value(60.0))
                .andExpect(jsonPath("$.ventas[0].cliente").value("Cliente reporte API"));

        mockMvc.perform(get("/api/reports/products")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .param("from", "2040-01-15")
                        .param("to", "2040-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas.productos").isNumber())
                .andExpect(jsonPath("$.productos").isArray());

        mockMvc.perform(get("/api/reports/cash")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .param("from", "2040-01-15")
                        .param("to", "2040-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.totalIngresado").value(150.0))
                .andExpect(jsonPath("$.cajas").isArray())
                .andExpect(jsonPath("$.ventas[0].ingresoCaja").value(150.0))
                .andExpect(jsonPath("$.pagos").isArray());

        mockMvc.perform(get("/api/reports/sales")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .param("from", "2026-07-31")
                        .param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void creaEditaYDesactivaUsuarioSinExponerContrasena() throws Exception {
        String usuario = "api.sistema.2026";

        mockMvc.perform(post("/api/users")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreCompleto": "Usuario API Sistema",
                                  "nombreUsuario": "api.sistema.2026",
                                  "contrasena": "Prueba123*",
                                  "rol": "VENDEDOR",
                                  "activo": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value(usuario))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());

        Number id = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/users")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("search", usuario))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.usuarios.length()").value(1))
                        .andReturn().getResponse().getContentAsString(),
                "$.usuarios[0].id"
        );

        mockMvc.perform(put("/api/users/{id}", id.longValue())
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombreCompleto": "Usuario API Editado",
                                  "nombreUsuario": "api.sistema.2026",
                                  "contrasena": "",
                                  "rol": "ADMINISTRADOR",
                                  "activo": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Usuario API Editado"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));

        mockMvc.perform(post("/api/users/{id}/toggle", id.longValue())
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void listaDescargaYEliminaUnBackupSeguro() throws Exception {
        Path archivo = copiaSeguridadService.obtenerDirectorioCopias()
                .resolve(BACKUP_PRUEBA);
        Files.write(archivo, new byte[]{1, 2, 3, 4});

        mockMvc.perform(get("/api/backups")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.automaticasHabilitadas").value(false))
                .andExpect(jsonPath("$.copias[0].nombre").value(BACKUP_PRUEBA));

        mockMvc.perform(get("/api/backups/{nombre}/download", BACKUP_PRUEBA)
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString(BACKUP_PRUEBA)
                ))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));

        mockMvc.perform(delete("/api/backups/{nombre}", BACKUP_PRUEBA)
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void vendedorNoAccedeAReportesUsuariosNiBackups() throws Exception {
        mockMvc.perform(get("/api/reports/sales")
                        .with(user("cajero").roles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users")
                        .with(user("cajero").roles("VENDEDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/backups")
                        .with(user("cajero").roles("VENDEDOR")))
                .andExpect(status().isForbidden());
    }
}
