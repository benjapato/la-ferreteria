package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasItem;
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
class UnidadesApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired ProveedorRepository proveedores;

    @ParameterizedTest
    @CsvSource({"METRO,1.200,1200.00,10.300", "KILOGRAMO,0.750,750.00,10.750", "LITRO,0.001,1.00,11.499", "BOLSA,2,2000.00,9.500"})
    void compraYVentaConservanCantidadImportesYStock(String unidad, String cantidad, String total, String stockFinal) throws Exception {
        long producto = crearProducto(unidad, "10.000", "1000.00");
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre("Proveedor unidades");
        proveedor.setCuit("30-12345678-9");
        proveedor.setActivo(true);
        proveedor = proveedores.save(proveedor);
        long compra = id(postJson("/api/purchases", """
                {"proveedorId":%d,"detalles":[{"productoId":%d,"cantidad":1.500,"costoUnitario":800.00}]}
                """.formatted(proveedor.getId(), producto))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(1200.0)));
        mvc.perform(post("/api/purchases/{id}/confirm", compra).with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isOk());
        assertDecimal("11.500", productos.findById(producto).orElseThrow().getStockActual());
        abrirCaja();
        postJson("/api/sales", venta(producto, cantidad))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(Double.parseDouble(total)))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(Double.parseDouble(cantidad)))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(Double.parseDouble(total)));
        assertDecimal(stockFinal, productos.findById(producto).orElseThrow().getStockActual());
    }

    @Test
    void vendeRemanenteMenorAUnoYRechazaExcesoSinCambiarStock() throws Exception {
        long producto = crearProducto("METRO", "0.350", "1000.00");
        abrirCaja();
        postJson("/api/sales", venta(producto, "0.351")).andExpect(status().isBadRequest());
        assertDecimal("0.350", productos.findById(producto).orElseThrow().getStockActual());
        postJson("/api/sales", venta(producto, "0.350"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(350.0));
        assertDecimal("0.000", productos.findById(producto).orElseThrow().getStockActual());
    }

    @Test
    void noRedondeaCantidadesConMasDeTresDecimalesSilenciosamente() throws Exception {
        long producto = crearProducto("KILOGRAMO", "10.000", "1000.00");
        abrirCaja();
        postJson("/api/sales", venta(producto, "1.2345")).andExpect(status().isBadRequest());
        assertDecimal("10.000", productos.findById(producto).orElseThrow().getStockActual());
    }

    @Test
    void subtotalFraccionarioSeRedondeaACentavos() throws Exception {
        long producto = crearProducto("METRO", "10.000", "1.00");
        abrirCaja();
        postJson("/api/sales", venta(producto, "0.335"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(0.34));
        assertDecimal("9.665", productos.findById(producto).orElseThrow().getStockActual());
    }

    @Test
    void bolsaDisponibleEnCatalogoYCreacionRapida() throws Exception {
        for (String endpoint : new String[]{"/api/catalog/setup", "/api/sales/setup"}) {
            mvc.perform(get(endpoint).with(user("admin").roles("ADMINISTRADOR")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.unidadesMedida", hasItem("BOLSA")));
        }
        long producto = crearProducto("BOLSA", "5", "1000.00");
        assertEquals("BOLSA", productos.findById(producto).orElseThrow().getUnidadMedida().name());
    }

    private long crearProducto(String unidad, String stock, String precio) throws Exception {
        Categoria categoria = new Categoria();
        categoria.setNombre("Unidades " + unidad);
        categoria.setActiva(true);
        categoria = categorias.save(categoria);
        return id(postJson("/api/catalog/products", """
                {"codigo":"U-%s","nombre":"Producto %s","precioCompra":0.50,"precioVenta":%s,
                 "stockActual":%s,"stockMinimo":0,"unidadMedida":"%s","categoriaId":%d}
                """.formatted(unidad, unidad, precio, stock, unidad, categoria.getId()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.unidadMedida").value(unidad)));
    }

    private void abrirCaja() throws Exception {
        postJson("/api/sales/cash/open", "{\"montoInicial\":0}").andExpect(status().isCreated());
    }

    private String venta(long producto, String cantidad) {
        return """
                {"formaPago":"EFECTIVO","detalles":[{"productoId":%d,"cantidad":%s}],"pagos":[]}
                """.formatted(producto, cantidad);
    }

    private ResultActions postJson(String endpoint, String json) throws Exception {
        return mvc.perform(post(endpoint).with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long id(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
