package com.LaFerreteria.LaFerreteria;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadWebIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void usuarioAnonimoEsRedirigidoAlLogin() throws Exception {

        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void loginAdministradorAceptaClaveCorrectaYRechazaIncorrecta()
            throws Exception {

        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("nombreUsuario", "admin")
                                .param("contrasena", "Test123*")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("nombreUsuario", "admin")
                                .param("contrasena", "incorrecta")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void vendedorPuedeConsultarProductosYRegistrarVentas()
            throws Exception {

        mockMvc.perform(get("/productos").with(user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La Ferretería")))
                .andExpect(content().string(containsString("Cerrar sesión")))
                .andExpect(content().string(containsString("Inicio")))
                .andExpect(content().string(containsString("Vender")))
                .andExpect(content().string(not(containsString("Copias de seguridad"))));

        mockMvc.perform(get("/ventas/nueva").with(user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo cliente")))
                .andExpect(content().string(not(containsString("Nuevo producto"))));
    }

    @Test
    void vendedorNoPuedeAdministrarCajaBackupsNiCancelarVentas()
            throws Exception {

        mockMvc.perform(get("/caja").with(user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/copias-seguridad").with(user("vendedor").roles("VENDEDOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/ventas/cancelar/1")
                                .with(user("vendedor").roles("VENDEDOR"))
                                .with(csrf())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorAccedeAModulosProtegidos() throws Exception {

        mockMvc.perform(get("/caja").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/copias-seguridad").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/usuarios").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La Ferretería")))
                .andExpect(content().string(containsString("Cerrar sesión")))
                .andExpect(content().string(containsString("Vender")))
                .andExpect(content().string(containsString("Caja")))
                .andExpect(content().string(containsString("Nueva compra")))
                .andExpect(content().string(containsString("Proveedores")))
                .andExpect(content().string(containsString("Reporte de caja")))
                .andExpect(content().string(containsString("Copias de seguridad")));

        mockMvc.perform(get("/ventas/nueva").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo cliente")))
                .andExpect(content().string(containsString("Nuevo producto")));
    }

    @Test
    void operacionesPostSinTokenCsrfSonRechazadas() throws Exception {

        mockMvc.perform(post("/caja/abrir")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/copias-seguridad/crear")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }
}
