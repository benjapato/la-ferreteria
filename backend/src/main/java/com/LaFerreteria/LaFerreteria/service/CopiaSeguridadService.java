package com.LaFerreteria.LaFerreteria.service;

import com.LaFerreteria.LaFerreteria.dto.CopiaSeguridadDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class CopiaSeguridadService {

    private static final DateTimeFormatter
            FORMATO_NOMBRE_ARCHIVO =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd_HH-mm-ss"
            );

    private final String datasourceUrl;
    private final String datasourceUsuario;
    private final String datasourceContrasena;
    private final String rutaPgDump;
    private final Path directorioCopias;

    public CopiaSeguridadService(
            @Value("${spring.datasource.url}")
            String datasourceUrl,

            @Value("${spring.datasource.username}")
            String datasourceUsuario,

            @Value("${spring.datasource.password}")
            String datasourceContrasena,

            @Value("${app.copias-seguridad.pg-dump:pg_dump}")
            String rutaPgDump,

            @Value("${app.copias-seguridad.directorio:backups}")
            String directorioCopias
    ) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsuario = datasourceUsuario;
        this.datasourceContrasena = datasourceContrasena;
        this.rutaPgDump = rutaPgDump;
        this.directorioCopias = Paths.get(
                directorioCopias
        ).toAbsolutePath().normalize();
    }

    public CopiaSeguridadDTO crearCopia() {

        DatosConexion datosConexion =
                obtenerDatosConexion();

        crearDirectorioSiNoExiste();

        String nombreArchivo =
                "LaFerreteria_"
                        + LocalDateTime.now().format(
                        FORMATO_NOMBRE_ARCHIVO
                )
                        + ".backup";

        Path archivoDestino = directorioCopias
                .resolve(nombreArchivo)
                .normalize();

        validarRutaDentroDelDirectorio(
                archivoDestino
        );

        List<String> comando = new ArrayList<>();

        comando.add(obtenerEjecutablePgDump());
        comando.add("--host");
        comando.add(datosConexion.host());
        comando.add("--port");
        comando.add(String.valueOf(datosConexion.port()));
        comando.add("--username");
        comando.add(datasourceUsuario);
        comando.add("--format=custom");
        comando.add("--blobs");
        comando.add("--no-owner");
        comando.add("--file");
        comando.add(archivoDestino.toString());
        comando.add(datosConexion.baseDeDatos());

        ProcessBuilder processBuilder =
                new ProcessBuilder(comando);

        processBuilder.redirectErrorStream(true);

        processBuilder.environment().put(
                "PGPASSWORD",
                datasourceContrasena
        );

        StringBuilder salidaProceso =
                new StringBuilder();

        try {

            Process proceso = processBuilder.start();

            Thread lectorSalida = new Thread(() -> {

                try (
                        BufferedReader lector =
                                new BufferedReader(
                                        new InputStreamReader(
                                                proceso.getInputStream(),
                                                StandardCharsets.UTF_8
                                        )
                                )
                ) {

                    String linea;

                    while ((linea = lector.readLine()) != null) {
                        salidaProceso.append(linea)
                                .append(System.lineSeparator());
                    }

                } catch (IOException ignored) {
                    // El código de salida del proceso informará el error.
                }
            });

            lectorSalida.start();

            boolean termino = proceso.waitFor(
                    5,
                    TimeUnit.MINUTES
            );

            if (!termino) {
                proceso.destroyForcibly();
                lectorSalida.join(2000);
                borrarArchivoSilenciosamente(
                        archivoDestino
                );

                throw new IllegalArgumentException(
                        "La copia de seguridad tardó demasiado y fue cancelada"
                );
            }

            lectorSalida.join(2000);

            int codigoSalida = proceso.exitValue();

            if (codigoSalida != 0) {
                borrarArchivoSilenciosamente(
                        archivoDestino
                );

                String detalle = salidaProceso
                        .toString()
                        .trim();

                if (detalle.isBlank()) {
                    detalle = "pg_dump terminó con el código "
                            + codigoSalida;
                }

                throw new IllegalArgumentException(
                        "No se pudo crear la copia de seguridad. "
                                + limitarMensaje(detalle)
                );
            }

            if (!Files.exists(archivoDestino)
                    || Files.size(archivoDestino) == 0) {

                borrarArchivoSilenciosamente(
                        archivoDestino
                );

                throw new IllegalArgumentException(
                        "PostgreSQL no generó correctamente el archivo de copia"
                );
            }

            return convertirADTO(
                    archivoDestino
            );

        } catch (IOException excepcion) {

            borrarArchivoSilenciosamente(
                    archivoDestino
            );

            throw new IllegalArgumentException(
                    "No se pudo ejecutar pg_dump. Revisá la ruta configurada: "
                            + rutaPgDump,
                    excepcion
            );

        } catch (InterruptedException excepcion) {

            Thread.currentThread().interrupt();

            borrarArchivoSilenciosamente(
                    archivoDestino
            );

            throw new IllegalArgumentException(
                    "La creación de la copia fue interrumpida",
                    excepcion
            );
        }
    }

    public List<CopiaSeguridadDTO> listarCopias() {

        crearDirectorioSiNoExiste();

        try (
                Stream<Path> archivos =
                        Files.list(directorioCopias)
        ) {

            return archivos
                    .filter(Files::isRegularFile)
                    .filter(this::esArchivoDeCopia)
                    .map(this::convertirADTO)
                    .sorted(
                            Comparator.comparing(
                                    CopiaSeguridadDTO::fechaModificacion
                            ).reversed()
                    )
                    .toList();

        } catch (IOException excepcion) {

            throw new IllegalArgumentException(
                    "No se pudo leer la carpeta de copias de seguridad",
                    excepcion
            );
        }
    }

    public Resource obtenerArchivo(
            String nombreArchivo
    ) {

        Path archivo = resolverArchivoSeguro(
                nombreArchivo
        );

        if (!Files.exists(archivo)
                || !Files.isRegularFile(archivo)) {

            throw new IllegalArgumentException(
                    "La copia de seguridad no existe"
            );
        }

        return new FileSystemResource(archivo);
    }

    public long obtenerTamanio(
            String nombreArchivo
    ) {

        Path archivo = resolverArchivoSeguro(
                nombreArchivo
        );

        try {
            return Files.size(archivo);

        } catch (IOException excepcion) {
            throw new IllegalArgumentException(
                    "No se pudo leer la copia de seguridad",
                    excepcion
            );
        }
    }

    public void eliminarCopia(
            String nombreArchivo
    ) {

        Path archivo = resolverArchivoSeguro(
                nombreArchivo
        );

        try {

            boolean eliminado =
                    Files.deleteIfExists(archivo);

            if (!eliminado) {
                throw new IllegalArgumentException(
                        "La copia de seguridad no existe"
                );
            }

        } catch (IOException excepcion) {

            throw new IllegalArgumentException(
                    "No se pudo eliminar la copia de seguridad",
                    excepcion
            );
        }
    }

    public Path obtenerDirectorioCopias() {
        crearDirectorioSiNoExiste();
        return directorioCopias;
    }

    private DatosConexion obtenerDatosConexion() {

        String url = datasourceUrl;

        if (url == null
                || !url.startsWith(
                "jdbc:postgresql://"
        )) {

            throw new IllegalArgumentException(
                    "La URL de PostgreSQL no tiene un formato válido"
            );
        }

        try {

            URI uri = URI.create(
                    url.substring("jdbc:".length())
            );

            String host = uri.getHost();
            int port = uri.getPort() == -1
                    ? 5432
                    : uri.getPort();

            String path = uri.getPath();

            if (host == null
                    || path == null
                    || path.length() <= 1) {

                throw new IllegalArgumentException(
                        "No se pudo obtener la base de datos desde spring.datasource.url"
                );
            }

            String baseDeDatos =
                    path.substring(1);

            return new DatosConexion(
                    host,
                    port,
                    baseDeDatos
            );

        } catch (IllegalArgumentException excepcion) {

            throw new IllegalArgumentException(
                    "No se pudo interpretar la conexión de PostgreSQL: "
                            + datasourceUrl,
                    excepcion
            );
        }
    }

    private String obtenerEjecutablePgDump() {

        if (rutaPgDump == null
                || rutaPgDump.isBlank()) {
            return "pg_dump";
        }

        String ruta = rutaPgDump.trim();

        if (ruta.equalsIgnoreCase("pg_dump")
                || ruta.equalsIgnoreCase("pg_dump.exe")) {
            return ruta;
        }

        Path ejecutable = Paths.get(ruta)
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(ejecutable)
                || !Files.isRegularFile(ejecutable)) {

            throw new IllegalArgumentException(
                    "No se encontró pg_dump en: "
                            + ejecutable
            );
        }

        return ejecutable.toString();
    }

    private void crearDirectorioSiNoExiste() {

        try {
            Files.createDirectories(
                    directorioCopias
            );

        } catch (IOException excepcion) {
            throw new IllegalArgumentException(
                    "No se pudo crear la carpeta de copias: "
                            + directorioCopias,
                    excepcion
            );
        }
    }

    private Path resolverArchivoSeguro(
            String nombreArchivo
    ) {

        if (nombreArchivo == null
                || nombreArchivo.isBlank()) {

            throw new IllegalArgumentException(
                    "Debe indicar una copia de seguridad"
            );
        }

        String nombreLimpio =
                Paths.get(nombreArchivo)
                        .getFileName()
                        .toString();

        if (!nombreLimpio.equals(nombreArchivo)
                || !nombreLimpio
                .toLowerCase(Locale.ROOT)
                .endsWith(".backup")) {

            throw new IllegalArgumentException(
                    "El nombre de la copia no es válido"
            );
        }

        Path archivo = directorioCopias
                .resolve(nombreLimpio)
                .normalize();

        validarRutaDentroDelDirectorio(
                archivo
        );

        return archivo;
    }

    private void validarRutaDentroDelDirectorio(
            Path archivo
    ) {

        if (!archivo.startsWith(
                directorioCopias
        )) {

            throw new IllegalArgumentException(
                    "La ruta de la copia no es válida"
            );
        }
    }

    private boolean esArchivoDeCopia(
            Path archivo
    ) {

        return archivo
                .getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".backup");
    }

    private CopiaSeguridadDTO convertirADTO(
            Path archivo
    ) {

        try {

            FileTime ultimaModificacion =
                    Files.getLastModifiedTime(
                            archivo
                    );

            LocalDateTime fecha =
                    LocalDateTime.ofInstant(
                            ultimaModificacion.toInstant(),
                            ZoneId.systemDefault()
                    );

            return new CopiaSeguridadDTO(
                    archivo.getFileName().toString(),
                    Files.size(archivo),
                    fecha
            );

        } catch (IOException excepcion) {

            throw new IllegalArgumentException(
                    "No se pudo leer el archivo: "
                            + archivo.getFileName(),
                    excepcion
            );
        }
    }

    private void borrarArchivoSilenciosamente(
            Path archivo
    ) {

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ignored) {
            // No reemplazamos el error original.
        }
    }

    private String limitarMensaje(
            String mensaje
    ) {

        String limpio = mensaje
                .replaceAll("\\s+", " ")
                .trim();

        int maximo = 700;

        if (limpio.length() <= maximo) {
            return limpio;
        }

        return limpio.substring(0, maximo)
                + "...";
    }

    private record DatosConexion(
            String host,
            int port,
            String baseDeDatos
    ) {
    }
}
