package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.config.UnidadMedidaInicializador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = ".+")
class UnidadMedidaPostgresTests {
    @Test
    void actualizaSoloRestriccionDeUnidadEnSchemaAislado() {
        var source = new DriverManagerDataSource(System.getenv("TEST_POSTGRES_URL"),
                System.getenv("TEST_POSTGRES_USER"), System.getenv("TEST_POSTGRES_PASSWORD"));
        var jdbc = new JdbcTemplate(source);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        String schema = "qa_unidades_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        try {
            jdbc.execute("CREATE TABLE " + schema + ".productos (unidad_medida varchar(30) NOT NULL, "
                    + "stock int CHECK(stock >= 0), CONSTRAINT productos_unidad_medida_check "
                    + "CHECK(unidad_medida IN ('UNIDAD','METRO','KILOGRAMO','LITRO','CAJA','PAQUETE','ROLLO')))");
            jdbc.execute("INSERT INTO " + schema + ".productos VALUES ('METRO', 2)");
            var initializer = new UnidadMedidaInicializador(jdbc, schema);
            transaction.executeWithoutResult(status -> initializer.run(null));
            jdbc.execute("INSERT INTO " + schema + ".productos VALUES ('BOLSA', 3)");
            transaction.executeWithoutResult(status -> initializer.run(null));
            assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".productos", Integer.class));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.execute("INSERT INTO " + schema + ".productos VALUES ('INVALIDA', 1)"));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.execute("INSERT INTO " + schema + ".productos VALUES ('BOLSA', -1)"));
        } finally {
            // Only this randomly named test schema is removed, never the application's tables.
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
}
