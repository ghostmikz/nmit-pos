package controller;

import client.SocketClient;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import model.Response;
import model.Sale;
import model.User;
import view.panels.ReportsPanel;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsController {

    private final ReportsPanel view;
    private final User         currentUser;

    public ReportsController(ReportsPanel view, User currentUser) {
        this.view        = view;
        this.currentUser = currentUser;
        view.setReportLoader(this::loadReport);
        view.setPdfExporter(this::exportPdf);
        LocalDate now = LocalDate.now();
        loadReport(now.withDayOfMonth(1).toString(), now.toString());
    }

    @SuppressWarnings("unchecked")
    private void loadReport(String startDate, String endDate) {
        new SwingWorker<List<Sale>, Void>() {
            @Override protected List<Sale> doInBackground() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("startDate", startDate);
                data.put("endDate",   endDate);
                Response resp = SocketClient.getInstance().send("GET_REPORT", currentUser.getToken(), data);
                if (!"OK".equals(resp.getStatus()))
                    throw new Exception(resp.getMessage() != null ? resp.getMessage() : "Error");
                return (List<Sale>) resp.getData();
            }

            @Override protected void done() {
                try {
                    view.setSaleData(get());
                } catch (Exception ex) {
                    view.showError("Failed to load: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void exportPdf(List<Sale> sales) {
        if (sales == null || sales.isEmpty()) {
            view.showToast("No data to export");
            return;
        }
        new SwingWorker<File, Void>() {
            @Override protected File doInBackground() throws Exception {
                String ts   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String home = System.getProperty("user.home");
                File out    = new File(home + File.separator + "Desktop", "Sales_Report_" + ts + ".pdf");

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();

                Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
                Paragraph title = new Paragraph("Sales Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                doc.add(title);

                Font subFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
                Paragraph sub = new Paragraph("Generated: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), subFont);
                sub.setAlignment(Element.ALIGN_CENTER);
                doc.add(sub);
                doc.add(Chunk.NEWLINE);

                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.6f, 1.6f, 1.1f, 1.3f, 1.2f, 1.2f, 1.0f});

                Font hFont = new Font(Font.HELVETICA, 9, Font.BOLD);
                addCell(table, "#",           hFont, true);
                addCell(table, "Receipt No.", hFont, true);
                addCell(table, "Date",        hFont, true);
                addCell(table, "Cashier",     hFont, true);
                addCell(table, "Payment",     hFont, true);
                addCell(table, "Total",       hFont, true);
                addCell(table, "Status",      hFont, true);

                Font rFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
                BigDecimal grandTotal = BigDecimal.ZERO;
                int idx = 1;
                for (Sale s : sales) {
                    addCell(table, String.valueOf(idx++),                 rFont, false);
                    addCell(table, safe(s.getReceiptNumber()),            rFont, false);
                    addCell(table, shortDate(s.getCreatedAt()),           rFont, false);
                    addCell(table, safe(s.getCashierName()),              rFont, false);
                    addCell(table, safe(s.getPaymentMethod()),            rFont, false);
                    addCell(table, "₮" + fmt(s.getTotal()),              rFont, false);
                    addCell(table, s.isRefunded() ? "Refunded" : "Sold", rFont, false);
                    if (s.getTotal() != null) grandTotal = grandTotal.add(s.getTotal());
                }
                doc.add(table);

                doc.add(Chunk.NEWLINE);
                Font sumFont = new Font(Font.HELVETICA, 11, Font.BOLD);
                Paragraph summary = new Paragraph(
                        sales.size() + " sales   Grand Total: ₮" + fmt(grandTotal), sumFont);
                summary.setAlignment(Element.ALIGN_RIGHT);
                doc.add(summary);

                doc.close();
                return out;
            }

            @Override protected void done() {
                try {
                    File f = get();
                    view.showToast("PDF saved: " + f.getName());
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
                } catch (Exception ex) {
                    view.showToast("Export failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void addCell(PdfPTable table, String text, Font font, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (header) {
            cell.setBackgroundColor(new java.awt.Color(0x7a1a1a));
            cell.getPhrase().getFont().setColor(java.awt.Color.WHITE);
        }
        cell.setPadding(5);
        cell.setBorderColor(new java.awt.Color(0xE2E8F0));
        table.addCell(cell);
    }

    private String safe(String s)      { return s != null ? s : "—"; }
    private String shortDate(String s) { return s != null && s.length() >= 10 ? s.substring(0, 10) : "—"; }
    private String fmt(BigDecimal val) { return val != null ? String.format("%,.0f", val) : "0"; }
}
