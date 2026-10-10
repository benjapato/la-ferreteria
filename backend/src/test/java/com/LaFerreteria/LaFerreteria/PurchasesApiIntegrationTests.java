package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.entity.Categoria;
import com.LaFerreteria.LaFerreteria.entity.Producto;
import com.LaFerreteria.LaFerreteria.entity.Proveedor;
import com.LaFerreteria.LaFerreteria.enums.TipoMovimientoStock;
import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import com.LaFerreteria.LaFerreteria.repository.CategoriaRepository;
import com.LaFerreteria.LaFerreteria.repository.CompraRepository;
import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.LaFerreteria.LaFerreteria.repository.ProveedorRepository;
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
class PurchasesApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Test
    void creaFiltraYConfirmaCompraActualizandoStockYCosto() throws Exception {
        Producto producto = guardarProducto("COMP-API-001", "5.000", "80.00");
        Proveedor proveedor = guardarProveedor("Proveedor API", "30-12345678-9");

        mockMvc.perform(get("/api/purchases/setup")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedores[0].id").value(proveedor.getId()))
                .andExpect(jsonPath("$.productos[0].id").value(producto.getId()));

        long compraId = crearCompra(proveedor.getId(), producto.getId());

        assertDecimal("5.000", productoRepository.findById(producto.getId())
                .orElseThrow().getStockActual());
        assertEquals(0, movimientoStockRepository.count());

        mockMvc.perform(get("/api/purchases")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .param("number", String.valueOf(compraId))
                        .param("supplier", "Proveedor API")
                        .param("status", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.compras[0].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.metricas.pendientes").value(1));

        mockMvc.perform(get("/api/purchases/{id}", compraId)
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(360.0))
                .andExpect(jsonPath("$.detalles[0].cantidad").value(3.0));

        mockMvc.perform(post("/api/purchases/{id}/confirm", compraId)
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        Producto actualizado = productoRepository.findById(producto.getId())
                .orElseThrow();
        assertDecimal("8.000", actualizado.getStockActual());
        assertDecimal("120.00", actualizado.getPrecioCompra());
        assertEquals(1, movimientoStockRepository.count());
        assertEquals(
                TipoMovimientoStock.ENTRADA_COMPRA,
                movimientoStockRepository.findAll().getFirst().getTipo()
        );
    }

    @Test
    void cancelaCompraPendienteSinModificarStock() throws Exception {
        Producto producto = guardarProducto("COMP-API-002", "4.000", "75.00");
        Proveedor proveedor = guardarProveedor("Proveedor Cancelacion", "30-87654321-9");
        long compraId = crearCompra(proveedor.getId(), producto.getId());

        mockMvc.perform(post("/api/purchases/{id}/cancel", compraId)
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertDecimal("4.000", productoRepository.findById(producto.getId())
                .orElseThrow().getStockActual());
        assertEquals(0, movimientoStockRepository.count());

        mockMvc.perform(post("/api/purchases/{id}/confirm", compraId)
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vendedorNoPuedeConsultarNiRegistrarCompras() throws Exception {
        mockMvc.perform(get("/api/purchases")
                        .with(user("cajero").roles("VENDEDOR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/purchases")
                        .with(user("cajero").roles("VENDEDOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private long crearCompra(long proveedorId, long productoId) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/purchases")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "proveedorId": %d,
                                  "observaciones": "Reposicion semanal",
                                  "detalles": [{
                                    "productoId": %d,
                                    "cantidad": 3.000,
                                    "costoUnitario": 120.00
                                  }]
                                }
                                """.formatted(proveedorId, productoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(360.0))
                .andReturn();

        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Producto guardarProducto(
            String codigo,
            String stock,
            String costo
    ) {
        Categoria categoria = new Categoria();
        categoria.setNombre("Compras " + codigo);
        categoria.setDescripcion("Pruebas de compras");
        categoria.setActiva(true);
        categoria = categoriaRepository.save(categoria);

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre("Producto " + codigo);
        producto.setMarca("Marca test");
        producto.setPrecioCompra(new BigDecimal(costo));
        producto.setPrecioVenta(new BigDecimal("200.00"));
        producto.setStockActual(new BigDecimal(stock));
        producto.setStockMinimo(BigDecimal.ONE);
        producto.setUnidadMedida(UnidadMedida.UNIDAD);
        producto.setCategoria(categoria);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private Proveedor guardarProveedor(String nombre, String cuit) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(nombre);
        proveedor.setCuit(cuit);
        proveedor.setActivo(true);
        return proveedorRepository.save(proveedor);
    }

    private void assertDecimal(String esperado, BigDecimal actual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(actual));
    }
}
