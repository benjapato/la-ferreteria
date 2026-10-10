package com.LaFerreteria.LaFerreteria.printing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@Service
public class TicketPrintingService {
    private final WindowsTicketPrinter printer;
    private final Path directory;
    public TicketPrintingService(WindowsTicketPrinter printer,
            @Value("${app.impresion.directorio:../.runtime/impresion}") String directory) {
        this.printer = printer;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public record Settings(String impresora, List<String> impresoras, boolean disponible) {}
    public record Result(String estado, String mensaje) {}

    public synchronized Settings settings() {
        String name = read(directory.resolve("config.properties")).getProperty("impresora", "");
        var available = printer.printers();
        return new Settings(name, available, !name.isBlank() && available.contains(name));
    }
    public synchronized Settings configure(String name) {
        String selected = name == null ? "" : name.trim();
        if (!selected.isEmpty() && !printer.printers().contains(selected))
            throw new IllegalArgumentException("Seleccione una impresora instalada en esta computadora.");
        var values = new Properties();
        values.setProperty("impresora", selected);
        write(directory.resolve("config.properties"), values);
        return settings();
    }
    public synchronized Result print(TicketData data, UUID requestId) {
        Path journal = directory.resolve("trabajos").resolve(requestId + ".properties");
        Properties previous = read(journal);
        if (!previous.isEmpty()) {
            if (!previous.getProperty("venta").equals(data.id().toString()))
                throw new IllegalArgumentException("La solicitud corresponde a otro comprobante.");
            return result(previous.getProperty("estado"));
        }
        var config = settings();
        if (!config.disponible())
            return new Result("NO_ENVIADO", "La venta esta guardada. Configure la impresora de tickets para imprimir.");
        var job = new Properties();
        job.setProperty("venta", data.id().toString());
        job.setProperty("impresora", config.impresora());
        job.setProperty("estado", "REVISAR");
        // Registrar antes del envio evita repetir un ticket tras un corte de conexion o reinicio.
        write(journal, job);
        try {
            printer.print(config.impresora(), data);
            job.setProperty("estado", "ENVIADO");
            write(journal, job);
            return result("ENVIADO");
        } catch (Exception exception) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("No se pudo confirmar el ticket {}", data.id(), exception);
            return result("REVISAR");
        }
    }
    private Result result(String state) {
        return "ENVIADO".equals(state)
                ? new Result("ENVIADO", "Ticket enviado a la cola de impresion de Windows.")
                : new Result("REVISAR", "No se pudo confirmar la impresion. Revise el papel, la conexion, el formato de 58 mm y la cola de Windows antes de reimprimir.");
    }
    private Properties read(Path file) {
        var values = new Properties();
        if (!Files.exists(file)) return values;
        try (var input = Files.newInputStream(file)) { values.load(input); return values; }
        catch (IOException e) { throw new IllegalArgumentException("No se pudo leer la configuracion de impresion.", e); }
    }
    private void write(Path file, Properties values) {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "impresion-", ".tmp");
            try (var output = Files.newOutputStream(temporary)) { values.store(output, "LaFerreteria"); }
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo guardar la configuracion de impresion.", e);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }
}
