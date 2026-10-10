package com.LaFerreteria.LaFerreteria.controller.api;

import com.LaFerreteria.LaFerreteria.printing.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@RestController
@RequestMapping("/api/printing")
public class PrintingApiController {
    private final TicketPrintingService printing;
    private final TicketDataService tickets;
    public PrintingApiController(TicketPrintingService printing, TicketDataService tickets) {
        this.printing = printing;
        this.tickets = tickets;
    }
    @GetMapping("/settings")
    public TicketPrintingService.Settings settings() { return printing.settings(); }
    @PutMapping("/settings")
    public TicketPrintingService.Settings configure(@Valid @RequestBody SettingsRequest request) {
        return printing.configure(request.impresora());
    }
    @PostMapping("/sales/{id}")
    public TicketPrintingService.Result print(@PathVariable Long id, @Valid @RequestBody PrintRequest request) {
        return printing.print(tickets.obtener(id), request.solicitudId());
    }
    @PostMapping("/test")
    public TicketPrintingService.Result test(@Valid @RequestBody PrintRequest request) {
        return printing.print(sample(), request.solicitudId());
    }
    @GetMapping("/sales/{id}/preview")
    public PreviewInfo previewInfo(@PathVariable Long id) {
        return new PreviewInfo(new TicketLayout(tickets.obtener(id))
                .pages(TicketLayout.CONTENT_WIDTH, 280 * TicketLayout.MM).size());
    }
    @GetMapping(value = "/sales/{id}/preview/{page}", produces = "image/png")
    public byte[] preview(@PathVariable Long id, @PathVariable int page) throws IOException {
        return png(tickets.obtener(id), page);
    }
    @GetMapping(value = "/sample", produces = "image/png")
    public byte[] samplePreview() throws IOException { return png(sample(), 0); }
    private byte[] png(TicketData data, int page) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new TicketLayout(data).preview(page), "png", output);
        return output.toByteArray();
    }
    private TicketData sample() {
        return new TicketData(0L, LocalDateTime.now(), "Consumidor final", null, null, null, false,
                new BigDecimal("8500"), "Prueba de impresion. Sin efecto en caja ni stock.",
                List.of(new TicketData.Item("PRUEBA", "Martillo de carpintero", BigDecimal.ONE, new BigDecimal("8500"), new BigDecimal("8500"))),
                List.of(new TicketData.Payment("Efectivo", new BigDecimal("8500"))));
    }
    public record SettingsRequest(@Size(max = 250) String impresora) {}
    public record PrintRequest(@NotNull UUID solicitudId) {}
    public record PreviewInfo(int paginas) {}
}
