package com.LaFerreteria.LaFerreteria.printing;

import org.springframework.stereotype.Component;
import javax.print.PrintService;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import java.awt.print.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class WindowsTicketPrinter {
    public List<String> printers() {
        return Arrays.stream(PrinterJob.lookupPrintServices()).map(PrintService::getName)
                .filter(this::physical).sorted().toList();
    }
    private boolean physical(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return !n.contains("pdf") && !n.contains("onenote") && !n.contains("xps") && !n.contains("fax");
    }
    public void print(String name, TicketData data) throws PrinterException {
        PrintService service = Arrays.stream(PrinterJob.lookupPrintServices())
                .filter(p -> p.getName().equals(name) && physical(p.getName())).findFirst()
                .orElseThrow(() -> new PrinterException("La impresora seleccionada no esta instalada."));
        if (PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS.equals(service.getAttribute(PrinterIsAcceptingJobs.class)))
            throw new PrinterException("La impresora no acepta trabajos. Revise la cola de Windows.");
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(service);
        job.setCopies(1);
        job.setJobName("LaFerreteria - Ticket " + data.id());
        var layout = new TicketLayout(data);
        var paper = new Paper();
        double height = Math.min(300 * TicketLayout.MM, Math.max(80 * TicketLayout.MM, layout.height() + 10 * TicketLayout.MM));
        paper.setSize(58 * TicketLayout.MM, height);
        paper.setImageableArea(5 * TicketLayout.MM, 3 * TicketLayout.MM,
                TicketLayout.CONTENT_WIDTH, height - 8 * TicketLayout.MM);
        var requested = new PageFormat();
        requested.setPaper(paper);
        var format = job.validatePage(requested);
        if (Math.abs(format.getWidth() - 58 * TicketLayout.MM) > 3 * TicketLayout.MM)
            throw new PrinterException("El controlador no acepta 58 mm. Configure el papel de la NexusPOS en Windows.");
        job.setPrintable(layout, format);
        job.print();
    }
}
