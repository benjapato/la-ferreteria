package com.LaFerreteria.LaFerreteria.config;

import com.LaFerreteria.LaFerreteria.enums.UnidadMedida;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UnidadMedidaInicializador implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final String tabla;

    public UnidadMedidaInicializador(JdbcTemplate jdbc,
            @Value("${spring.jpa.properties.hibernate.default_schema:}") String schema) {
        this.jdbc = jdbc;
        this.tabla = schema.isBlank() ? "productos" : quote(schema) + ".productos";
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String database = jdbc.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        if (!"PostgreSQL".equals(database)) return;

        List<String> restricciones = jdbc.queryForList("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conrelid = to_regclass(?) AND contype = 'c'
                  AND conname = 'productos_unidad_medida_check'
                """, String.class, tabla);
        if (restricciones.isEmpty() || restricciones.getFirst().contains("'BOLSA'")) return;

        // Hibernate update does not extend the enum CHECK on existing PostgreSQL tables.
        String valores = Arrays.stream(UnidadMedida.values())
                .map(unidad -> "'" + unidad.name() + "'")
                .collect(Collectors.joining(", "));
        jdbc.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT productos_unidad_medida_check, "
                + "ADD CONSTRAINT productos_unidad_medida_check CHECK (unidad_medida IN (" + valores + "))");
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
