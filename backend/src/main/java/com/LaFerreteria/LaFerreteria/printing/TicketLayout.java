package com.LaFerreteria.LaFerreteria.printing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** El mismo trazado se usa en la vista previa y en el controlador de Windows. */
public final class TicketLayout implements Printable {
    public static final double MM = 72.0 / 25.4;
    public static final double CONTENT_WIDTH = 48 * MM;
    private final TicketData data;
    public TicketLayout(TicketData data) { this.data = data; }

    public record Line(String text, Font font, boolean centered, boolean right, boolean rule) {
        double height() { return rule ? 10 : font.getSize2D() + 4; }
    }

    public List<Line> lines(double width) {
        var result = new ArrayList<Line>();
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            text(result, g, width, "LaFerreteria", 16, true, true, false);
            text(result, g, width, "Domicilio del comercio", 9, false, true, false);
            text(result, g, width, "Localidad", 9, false, true, false);
            text(result, g, width, "Tel.: configurar", 9, false, true, false);
            rule(result);
            text(result, g, width, "NOTA DE PEDIDO", 11, true, true, false);
            if (data.id() == 0) text(result, g, width, "PRUEBA", 8, false, true, false);
            if (data.cancelada()) text(result, g, width, "VENTA CANCELADA", 12, true, true, false);
            text(result, g, width, "N. " + String.format("%08d", data.id()), 10, true, true, false);
            text(result, g, width, data.fecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), 8, false, true, false);
            rule(result);
            text(result, g, width, "Cliente: " + data.cliente(), 9, true, false, false);
            if (data.documento() != null && !data.documento().isBlank())
                text(result, g, width, "DNI / CUIT: " + data.documento(), 8, false, false, false);
            if (data.vendedor() != null) text(result, g, width, "Atendio: " + data.vendedor(), 8, false, false, false);
            if (data.cajaId() != null) text(result, g, width, "Caja: " + data.cajaId(), 8, false, false, false);
            rule(result);
            for (var item : data.items()) {
                text(result, g, width, item.nombre(), 9, true, false, false);
                text(result, g, width, item.cantidad().stripTrailingZeros().toPlainString().replace('.', ',')
                        + " x " + money(item.precio()), 8, false, false, false);
                text(result, g, width, money(item.subtotal()), 10, true, false, true);
            }
            rule(result);
            text(result, g, width, "TOTAL", 10, true, false, false);
            text(result, g, width, money(data.total()), 16, true, false, true);
            rule(result);
            for (var payment : data.pagos()) {
                text(result, g, width, payment.nombre(), 8, false, false, false);
                text(result, g, width, money(payment.monto()), 9, true, false, true);
            }
            return List.copyOf(result);
        } finally { g.dispose(); }
    }

    private static String money(java.math.BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(value);
    }
    private static void rule(List<Line> lines) { lines.add(new Line("", null, false, false, true)); }

    private static void text(List<Line> lines, Graphics2D g, double width, String value,
                             int size, boolean bold, boolean centered, boolean right) {
        var font = new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size);
        var metrics = g.getFontMetrics(font);
        String remaining = value.replaceAll("\\s+", " ").trim();
        while (!remaining.isEmpty()) {
            int end = remaining.length();
            while (end > 1 && metrics.stringWidth(remaining.substring(0, end)) > width) end--;
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
            }
            lines.add(new Line(remaining.substring(0, end), font, centered, right, false));
            remaining = remaining.substring(end).stripLeading();
        }
    }

    public double height() { return lines(CONTENT_WIDTH).stream().mapToDouble(Line::height).sum() + 20; }
    public List<List<Line>> pages(double width, double height) {
        var pages = new ArrayList<List<Line>>();
        var page = new ArrayList<Line>();
        double used = 0;
        for (var line : lines(width)) {
            if (!page.isEmpty() && used + line.height() > height) {
                pages.add(List.copyOf(page));
                page.clear();
                used = 0;
            }
            page.add(line);
            used += line.height();
        }
        if (!page.isEmpty()) pages.add(List.copyOf(page));
        return pages;
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int pageIndex) throws PrinterException {
        double width = Math.min(CONTENT_WIDTH, format.getImageableWidth());
        if (width < 35 * MM || format.getImageableHeight() < 25 * MM)
            throw new PrinterException("Area imprimible demasiado pequena. Configure el papel de 58 mm.");
        var pages = pages(width, format.getImageableHeight());
        if (pageIndex < 0 || pageIndex >= pages.size()) return NO_SUCH_PAGE;
        var g = (Graphics2D) graphics.create();
        try {
            g.translate(format.getImageableX() + (format.getImageableWidth() - width) / 2, format.getImageableY());
            draw(g, pages.get(pageIndex), width);
        } finally { g.dispose(); }
        return PAGE_EXISTS;
    }

    public BufferedImage preview(int page) {
        var pages = pages(CONTENT_WIDTH, 280 * MM);
        if (page < 0 || page >= pages.size()) throw new IllegalArgumentException("Pagina de ticket inexistente");
        var lines = pages.get(page);
        double height = lines.stream().mapToDouble(Line::height).sum() + 24;
        var image = new BufferedImage((int) Math.ceil(58 * MM * 3), (int) Math.ceil(height * 3), BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.scale(3, 3);
            g.translate(5 * MM, 10);
            draw(g, lines, CONTENT_WIDTH);
        } finally { g.dispose(); }
        return image;
    }

    private void draw(Graphics2D g, List<Line> lines, double width) {
        g.setColor(Color.BLACK);
        double y = 0;
        for (var line : lines) {
            if (line.rule()) {
                g.setStroke(new BasicStroke(0.5f));
                g.draw(new java.awt.geom.Line2D.Double(0, y + 5, width, y + 5));
            } else {
                g.setFont(line.font());
                double textWidth = g.getFontMetrics().stringWidth(line.text());
                double x = line.centered() ? (width - textWidth) / 2 : line.right() ? width - textWidth : 0;
                g.drawString(line.text(), (float) x, (float) (y + line.font().getSize2D()));
            }
            y += line.height();
        }
    }
}
