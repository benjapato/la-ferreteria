package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.repository.MovimientoStockRepository;
import com.LaFerreteria.LaFerreteria.repository.ProductoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
class CatalogApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Test
    void administraProductosSinModificarStockFueraDeUnAjuste()
            throws Exception {
        long categoriaId = crearCategoria("Herramientas manuales");
        long productoId = crearProducto(
                categoriaId,
                "CAT-001",
                "Martillo carpintero",
                "5.000",
                "5.000"
        );

        mockMvc.perform(
                        get("/api/catalog/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("query", "martillo")
                                .param("category", String.valueOf(categoriaId))
                                .param("status", "ACTIVOS")
                                .param("stock", "BAJO")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.productos[0].codigo").value("CAT-001"))
                .andExpect(jsonPath("$.productos[0].estadoStock").value("BAJO"));

        mockMvc.perform(
                        put("/api/catalog/products/{id}", productoId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson(
                                        categoriaId,
                                        "CAT-001",
                                        "Martillo reforzado",
                                        "999.000",
                                        "5.000",
                                        "210.00"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Martillo reforzado"))
                .andExpect(jsonPath("$.precioVenta").value(210.0))
                .andExpect(jsonPath("$.stockActual").value(5.0));

        mockMvc.perform(
                        post("/api/catalog/products/{id}/stock-adjustments", productoId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tipo": "AJUSTE_ENTRADA",
                                          "cantidad": 2.500,
                                          "observacion": "Conteo de deposito"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(7.5))
                .andExpect(jsonPath("$.estadoStock").value("NORMAL"));

        assertEquals(1, movimientoStockRepository.count());
        assertDecimal(
                "7.500",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );

        mockMvc.perform(
                        post("/api/catalog/products/{id}/status", productoId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(
                        get("/api/catalog/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .param("status", "INACTIVOS")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void rechazaAjusteQueDejariaStockNegativo() throws Exception {
        long categoriaId = crearCategoria("Ajustes controlados");
        long productoId = crearProducto(
                categoriaId,
                "CAT-NEG",
                "Producto controlado",
                "2.000",
                "1.000"
        );

        mockMvc.perform(
                        post("/api/catalog/products/{id}/stock-adjustments", productoId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "tipo": "AJUSTE_SALIDA",
                                          "cantidad": 3.000
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "stock negativo"
                        )
                ));

        assertEquals(0, movimientoStockRepository.count());
        assertDecimal(
                "2.000",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getStockActual()
        );
    }

    @Test
    void actualizaElPrecioDeVentaDeTodosLosProductosPorPorcentaje()
            throws Exception {
        long categoriaId = crearCategoria("Actualizacion de precios");
        long primerProductoId = crearProducto(
                categoriaId,
                "PRECIO-001",
                "Primer producto",
                "5.000",
                "2.000"
        );
        long segundoProductoId = crearProducto(
                categoriaId,
                "PRECIO-002",
                "Segundo producto",
                "3.000",
                "1.000"
        );

        mockMvc.perform(
                        post("/api/catalog/products/prices/percentage")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "porcentaje": 10,
                                          "operacion": "AUMENTAR"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productosActualizados").value(2))
                .andExpect(jsonPath("$.porcentaje").value(10))
                .andExpect(jsonPath("$.operacion").value("AUMENTAR"));

        assertDecimal(
                "165.00",
                productoRepository.findById(primerProductoId)
                        .orElseThrow()
                        .getPrecioVenta()
        );
        assertDecimal(
                "165.00",
                productoRepository.findById(segundoProductoId)
                        .orElseThrow()
                        .getPrecioVenta()
        );
    }

    @Test
    void alAumentarCorrigeUnPrecioAntiguoQueEstabaBajoElCosto()
            throws Exception {
        long categoriaId = crearCategoria("Correccion de precios");
        long productoId = crearProducto(
                categoriaId,
                "PRECIO-ANTIGUO",
                "Producto con precio antiguo",
                "2.000",
                "1.000"
        );
        var producto = productoRepository.findById(productoId).orElseThrow();
        producto.setPrecioVenta(new java.math.BigDecimal("50.00"));
        productoRepository.saveAndFlush(producto);

        mockMvc.perform(
                        post("/api/catalog/products/prices/percentage")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "porcentaje": 10,
                                          "operacion": "AUMENTAR"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productosActualizados").value(1));

        assertDecimal(
                "100.00",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getPrecioVenta()
        );
    }

    @Test
    void rechazaLaActualizacionCompletaSiUnPrecioQuedaBajoElCosto()
            throws Exception {
        long categoriaId = crearCategoria("Precios protegidos");
        long productoId = crearProducto(
                categoriaId,
                "PRECIO-BAJO",
                "Producto protegido",
                "2.000",
                "1.000"
        );

        mockMvc.perform(
                        post("/api/catalog/products/prices/percentage")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "porcentaje": 40,
                                          "operacion": "DISMINUIR"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString(
                                "menor al de compra"
                        )
                ));

        assertDecimal(
                "150.00",
                productoRepository.findById(productoId)
                        .orElseThrow()
                        .getPrecioVenta()
        );
    }

    @Test
    void administraCategoriasYProveedores() throws Exception {
        long categoriaId = crearCategoria("Pintureria");

        mockMvc.perform(
                        put("/api/catalog/categories/{id}", categoriaId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "nombre": "Pinturas",
                                          "descripcion": "Pinturas y accesorios"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pinturas"));

        mockMvc.perform(
                        post("/api/catalog/categories/{id}/status", categoriaId)
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(false));

        MvcResult proveedorCreado = mockMvc.perform(
                        post("/api/catalog/providers")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "nombre": "Distribuidora Norte",
                                          "razonSocial": "Distribuidora Norte SA",
                                          "cuit": "30-12345678-9",
                                          "telefono": "+54 11 4555-7788 interno 12",
                                          "email": "ventas@norte.test",
                                          "direccion": "Ruta 1"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefono").value(
                        "+54 11 4555-7788 interno 12"
                ))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();

        Number proveedorId = JsonPath.read(
                proveedorCreado.getResponse().getContentAsString(),
                "$.id"
        );

        mockMvc.perform(
                        put("/api/catalog/providers/{id}", proveedorId.longValue())
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "nombre": "Distribuidora Norte",
                                          "razonSocial": "Distribuidora Norte SA",
                                          "cuit": "30-12345678-9",
                                          "telefono": "1199999999",
                                          "email": "pedidos@norte.test",
                                          "direccion": "Ruta 2"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("1199999999"));

        mockMvc.perform(
                        post("/api/catalog/providers/{id}/status", proveedorId.longValue())
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    void vendedorConsultaProductosPeroNoAdministraCatalogo()
            throws Exception {
        mockMvc.perform(
                        get("/api/catalog/products")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/catalog/categories")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/catalog/providers")
                                .with(user("cajero").roles("VENDEDOR"))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/catalog/categories")
                                .with(user("cajero").roles("VENDEDOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"No permitida\"}")
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
                                          "descripcion": "Categoria de prueba"
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
            String nombre,
            String stockActual,
            String stockMinimo
    ) throws Exception {
        MvcResult resultado = mockMvc.perform(
                        post("/api/catalog/products")
                                .with(user("admin").roles("ADMINISTRADOR"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(productoJson(
                                        categoriaId,
                                        codigo,
                                        nombre,
                                        stockActual,
                                        stockMinimo,
                                        "150.00"
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(
                resultado.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private String productoJson(
            long categoriaId,
            String codigo,
            String nombre,
            String stockActual,
            String stockMinimo,
            String precioVenta
    ) {
        return ("""
                {
                  "codigo": "%s",
                  "nombre": "%s",
                  "descripcion": "Descripcion de prueba",
                  "marca": "Marca API",
                  "precioCompra": 100.00,
                  "precioVenta": %s,
                  "stockActual": %s,
                  "stockMinimo": %s,
                  "unidadMedida": "UNIDAD",
                  "categoriaId": %d
                }
                """).formatted(
                codigo,
                nombre,
                precioVenta,
                stockActual,
                stockMinimo,
                categoriaId
        );
    }

    private void assertDecimal(String esperado, Object actual) {
        assertEquals(
                0,
                new java.math.BigDecimal(esperado)
                        .compareTo((java.math.BigDecimal) actual)
        );
    }
}
