package com.LaFerreteria.LaFerreteria;

import com.LaFerreteria.LaFerreteria.service.CopiaSeguridadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopiaSeguridadServiceTests {

    @TempDir
    Path directorioTemporal;

    @Test
    void listaDescargaMideYEliminaCopiasValidas() throws Exception {

        Path copia = directorioTemporal.resolve("prueba.backup");
        Files.writeString(copia, "contenido de prueba");
        Files.writeString(
                directorioTemporal.resolve("ignorar.txt"),
                "no es una copia"
        );

        CopiaSeguridadService servicio = crearServicio(
                "jdbc:postgresql://localhost:5432/prueba"
        );

        assertEquals(1, servicio.listarCopias().size());
        assertEquals(
                "prueba.backup",
                servicio.listarCopias().getFirst().nombre()
        );
        assertEquals(Files.size(copia), servicio.obtenerTamanio("prueba.backup"));
        assertTrue(servicio.obtenerArchivo("prueba.backup").exists());

        servicio.eliminarCopia("prueba.backup");

        assertTrue(servicio.listarCopias().isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.eliminarCopia("prueba.backup")
        );
    }

    @Test
    void rechazaNombresQueIntentanSalirDelDirectorio() {

        CopiaSeguridadService servicio = crearServicio(
                "jdbc:postgresql://localhost:5432/prueba"
        );

        for (String nombre : new String[]{
                "../secreto.backup",
                "..\\secreto.backup",
                "archivo.txt",
                "",
                "carpeta/archivo.backup"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> servicio.obtenerArchivo(nombre)
            );
        }
    }

    @Test
    void noIntentaCrearBackupCuandoLaUrlNoEsPostgresql() {

        CopiaSeguridadService servicio = crearServicio(
                "jdbc:h2:mem:prueba"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                servicio::crearCopia
        );

        assertTrue(error.getMessage().contains("PostgreSQL"));
    }

    private CopiaSeguridadService crearServicio(String datasourceUrl) {

        return new CopiaSeguridadService(
                datasourceUrl,
                "usuario",
                "contrasena",
                "pg_dump",
                directorioTemporal.toString()
        );
    }
}
