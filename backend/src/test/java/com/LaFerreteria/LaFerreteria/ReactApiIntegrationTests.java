package com.LaFerreteria.LaFerreteria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReactApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginApiAceptaCredencialesCorrectas() throws Exception {
        mockMvc.perform(
                        post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "nombreUsuario": "admin",
                                          "contrasena": "Test123*"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("admin"))
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void usuarioAnonimoNoAccedeAlDashboardApi()
            throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorRecibeDashboardEstructurado()
            throws Exception {
        mockMvc.perform(
                        get("/api/dashboard")
                                .with(user("admin")
                                        .roles("ADMINISTRADOR"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricas.cantidadVentas").exists())
                .andExpect(jsonPath("$.productosStockBajo").isArray())
                .andExpect(jsonPath("$.ventasRecientes").isArray());
    }
}
