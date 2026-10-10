package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.printing.WindowsTicketPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PrintingApiTests {
    @TempDir static Path directory;
    @Autowired MockMvc mvc;
    @MockitoBean WindowsTicketPrinter printer;
    @DynamicPropertySource static void props(DynamicPropertyRegistry registry) {
        registry.add("app.impresion.directorio", () -> directory.toString());
    }
    @BeforeEach void setup() { when(printer.printers()).thenReturn(List.of("NexusPOS")); }

    @Test void requiereLogin() throws Exception {
        mvc.perform(get("/api/printing/settings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/printing/sample")).andExpect(status().isUnauthorized());
    }
    @Test void vendedorPuedeConsultarPeroNoConfigurarNiProbar() throws Exception {
        mvc.perform(get("/api/printing/settings").with(user("v").roles("VENDEDOR"))).andExpect(status().isOk());
        mvc.perform(put("/api/printing/settings").with(user("v").roles("VENDEDOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"impresora\":\"NexusPOS\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/printing/test").with(user("v").roles("VENDEDOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"solicitudId\":\"" + UUID.randomUUID() + "\"}")).andExpect(status().isForbidden());
    }
    @Test void configuracionRequiereCsrf() throws Exception {
        mvc.perform(put("/api/printing/settings").with(user("admin").roles("ADMINISTRADOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"impresora\":\"NexusPOS\"}")).andExpect(status().isForbidden());
    }
    @Test void noPermiteImprimirVentaInexistente() throws Exception {
        mvc.perform(post("/api/printing/sales/999999").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"solicitudId\":\"" + UUID.randomUUID() + "\"}")).andExpect(status().isBadRequest());
        verify(printer, never()).print(anyString(), any());
    }
    @Test void identificadorDeImpresionEsObligatorio() throws Exception {
        mvc.perform(post("/api/printing/test").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/printing/test").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"solicitudId\":\"../otro\"}")).andExpect(status().isBadRequest());
    }
    @Test void vistaPreviaEsUnaImagenYNoEnviaImpresiones() throws Exception {
        mvc.perform(get("/api/printing/sample").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        verify(printer, never()).print(anyString(), any());
    }
    @Test void pruebaImprimeSoloUnaVezConElMismoId() throws Exception {
        mvc.perform(put("/api/printing/settings").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"impresora\":\"NexusPOS\"}")).andExpect(status().isOk());
        String body = "{\"solicitudId\":\"" + UUID.randomUUID() + "\"}";
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/printing/test").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENVIADO"));
        verify(printer, times(1)).print(eq("NexusPOS"), any());
    }
}
