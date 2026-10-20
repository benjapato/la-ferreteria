package com.LaFerreteria.LaFerreteria.printing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketPrintingTests {
    @TempDir Path directory;
    WindowsTicketPrinter printer;
    TicketPrintingService service;

    @BeforeEach void setup() {
        printer = mock(WindowsTicketPrinter.class);
        when(printer.printers()).thenReturn(List.of("NexusPOS Z-NX58 U"));
        service = new TicketPrintingService(printer, directory.toString());
    }

    static TicketData sample(int count) {
        return new TicketData(12L, LocalDateTime.of(2026, 9, 23, 18, 30),
                "Consumidor final", null, "Administrador", 2L, false,
                new BigDecimal("8500"), "Retirar en mostrador.",
                java.util.stream.IntStream.range(0, count).mapToObj(i -> new TicketData.Item("A" + i,
                        "Martillo de carpintero con mango reforzado", new BigDecimal("2"), new BigDecimal("4250"), new BigDecimal("8500"))).toList(),
                List.of(new TicketData.Payment("Efectivo", new BigDecimal("9000")),
                        new TicketData.Payment("Saldo a favor", new BigDecimal("-500"))));
    }

    @Test void noEligeLaImpresoraPredeterminada() throws Exception {
        assertEquals("NO_ENVIADO", service.print(sample(1), UUID.randomUUID()).estado());
        verify(printer, never()).print(anyString(), any());
    }
    @Test void guardaConfiguracionYSeConservaAlReiniciar() {
        service.configure("NexusPOS Z-NX58 U");
        assertTrue(new TicketPrintingService(printer, directory.toString()).settings().disponible());
    }
    @Test void rechazaUnaImpresoraNoInstalada() {
        assertThrows(IllegalArgumentException.class, () -> service.configure("Microsoft Print to PDF"));
    }
    @Test void quitarConfiguracionNoImprime() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        service.configure("");
        assertEquals("NO_ENVIADO", service.print(sample(1), UUID.randomUUID()).estado());
        verify(printer, never()).print(anyString(), any());
    }
    @Test void impresoraDesconectadaNoCambiaDestino() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        when(printer.printers()).thenReturn(List.of("Otra"));
        assertEquals("NO_ENVIADO", service.print(sample(1), UUID.randomUUID()).estado());
        verify(printer, never()).print(anyString(), any());
    }
    @Test void unReintentoNoDuplicaNiTrasReiniciar() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        UUID id = UUID.randomUUID();
        assertEquals("ENVIADO", service.print(sample(1), id).estado());
        assertEquals("ENVIADO", service.print(sample(1), id).estado());
        assertEquals("ENVIADO", new TicketPrintingService(printer, directory.toString()).print(sample(1), id).estado());
        verify(printer, times(1)).print(eq("NexusPOS Z-NX58 U"), any());
    }
    @Test void permiteReimpresionExplicita() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        service.print(sample(1), UUID.randomUUID());
        service.print(sample(1), UUID.randomUUID());
        verify(printer, times(2)).print(anyString(), any());
    }
    @Test void errorDeImpresoraNoSeConfundeConVentaFallida() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        doThrow(new PrinterException("Sin papel")).when(printer).print(anyString(), any());
        UUID id = UUID.randomUUID();
        assertEquals("REVISAR", service.print(sample(1), id).estado());
        assertEquals("REVISAR", service.print(sample(1), id).estado());
        verify(printer, times(1)).print(anyString(), any());
    }
    @Test void mismoIdNoSePuedeUsarParaOtraVenta() {
        service.configure("NexusPOS Z-NX58 U");
        UUID id = UUID.randomUUID();
        var data = sample(1);
        service.print(data, id);
        var other = new TicketData(99L, data.fecha(), data.cliente(), null, null, null, false, data.total(), null, data.items(), data.pagos());
        assertThrows(IllegalArgumentException.class, () -> service.print(other, id));
    }
    @Test void solicitudesSimultaneasImprimenUnaVez() throws Exception {
        service.configure("NexusPOS Z-NX58 U");
        var id = UUID.randomUUID();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> service.print(sample(1), id));
            var second = pool.submit(() -> service.print(sample(1), id));
            assertEquals("ENVIADO", first.get(5, TimeUnit.SECONDS).estado());
            assertEquals("ENVIADO", second.get(5, TimeUnit.SECONDS).estado());
        }
        verify(printer, times(1)).print(anyString(), any());
    }
    @Test void todoElTextoCabeEnElAncho() {
        var g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            for (var line : new TicketLayout(sample(1)).lines(TicketLayout.CONTENT_WIDTH)) {
                if (!line.rule()) assertTrue(g.getFontMetrics(line.font()).stringWidth(line.text()) <= TicketLayout.CONTENT_WIDTH, line.text());
            }
        } finally { g.dispose(); }
    }
    @Test void encabezadoIncluyeDomicilioContactoYNotaDePedido() {
        var text = new TicketLayout(sample(1)).lines(TicketLayout.CONTENT_WIDTH).stream()
                .filter(line -> !line.rule()).map(TicketLayout.Line::text).toList();
        assertEquals(List.of("LaFerreteria", "Domicilio del comercio", "Localidad", "Tel.: configurar"), text.subList(0, 4));
        assertTrue(text.contains("NOTA DE PEDIDO"));
        assertFalse(text.contains("COMPROBANTE DE VENTA"));
    }
    @Test void terminaEnLosPagosSinObservacionesNiPieAnterior() {
        var text = new TicketLayout(sample(1)).lines(TicketLayout.CONTENT_WIDTH).stream()
                .filter(line -> !line.rule()).map(TicketLayout.Line::text).toList();
        String content = String.join(" ", text);
        assertFalse(content.toLowerCase().contains("fiscal"));
        assertFalse(content.contains("Retirar en mostrador"));
        assertFalse(content.contains("Gracias por su compra"));
        assertFalse(content.contains("Conserve este comprobante"));
        assertEquals("Saldo a favor", text.get(text.size() - 2));
        assertTrue(text.getLast().contains("500,00"));
    }
    @Test void ticketLargoSePaginaSinPerderRenglones() {
        var layout = new TicketLayout(sample(120));
        var pages = layout.pages(TicketLayout.CONTENT_WIDTH, 180);
        assertTrue(pages.size() > 3);
        assertEquals(layout.lines(TicketLayout.CONTENT_WIDTH), pages.stream().flatMap(List::stream).toList());
    }
    @Test void renderizaVistaPreviaNoBlanca() throws Exception {
        var image = new TicketLayout(sample(1)).preview(0);
        int ink = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if ((image.getRGB(x, y) & 0xffffff) != 0xffffff) ink++;
        assertTrue(ink > 2000);
        Path report = Path.of("build/reports/thermal/sample-ticket.png");
        Files.createDirectories(report.getParent());
        ImageIO.write(image, "png", report.toFile());
    }
    @Test void rechazaPaginaDeVistaPreviaInexistente() {
        assertThrows(IllegalArgumentException.class, () -> new TicketLayout(sample(1)).preview(100));
    }
    @Test void paginasImprimiblesNoPierdenContenido() throws Exception {
        var layout = new TicketLayout(sample(30));
        var paper = new Paper();
        paper.setSize(58 * TicketLayout.MM, 220);
        paper.setImageableArea(0, 0, TicketLayout.CONTENT_WIDTH, 200);
        var format = new PageFormat();
        format.setPaper(paper);
        var graphics = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB).createGraphics();
        try {
            int count = layout.pages(TicketLayout.CONTENT_WIDTH, 200).size();
            for (int i = 0; i < count; i++) assertEquals(Printable.PAGE_EXISTS, layout.print(graphics, format, i));
            assertEquals(Printable.NO_SUCH_PAGE, layout.print(graphics, format, count));
        } finally { graphics.dispose(); }
    }
}
