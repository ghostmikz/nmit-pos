package controller;

import client.SocketClient;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import i18n.I18n;
import model.Response;
import model.Sale;
import model.User;
import view.panels.ReportsPanel;

import javax.swing.*;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
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
            view.showToast(I18n.t("report.empty"));
            return;
        }
        new SwingWorker<File, Void>() {
            @Override protected File doInBackground() throws Exception {
                String ts   = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String home = System.getProperty("user.home");
                File out    = new File(home + File.separator + "Desktop", "Sales_Report_" + ts + ".pdf");

                BaseFont bfReg  = unicodeBaseFont("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
                BaseFont bfBold = unicodeBaseFont("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf");
                Font titleFont = bfBold  != null ? new Font(bfBold,  16) : new Font(Font.HELVETICA, 16, Font.BOLD);
                Font subFont   = bfReg   != null ? new Font(bfReg,   10) : new Font(Font.HELVETICA, 10, Font.NORMAL);
                Font hFont     = bfBold  != null ? new Font(bfBold,   9) : new Font(Font.HELVETICA,  9, Font.BOLD);
                Font rFont     = bfReg   != null ? new Font(bfReg,    8) : new Font(Font.HELVETICA,  8, Font.NORMAL);
                Font sumFont   = bfBold  != null ? new Font(bfBold,  11) : new Font(Font.HELVETICA, 11, Font.BOLD);

                Document doc = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(doc, new FileOutputStream(out));
                doc.open();

                Paragraph title = new Paragraph(I18n.t("report.title"), titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                doc.add(title);

                Paragraph sub = new Paragraph(I18n.t("report.pdf.generated") + " " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), subFont);
                sub.setAlignment(Element.ALIGN_CENTER);
                doc.add(sub);
                doc.add(Chunk.NEWLINE);

                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.6f, 1.6f, 1.1f, 1.3f, 1.2f, 1.2f, 1.0f});

                addCell(table, "#",                          hFont, true);
                addCell(table, I18n.t("report.receipt"),    hFont, true);
                addCell(table, I18n.t("report.date"),       hFont, true);
                addCell(table, I18n.t("report.cashier"),    hFont, true);
                addCell(table, I18n.t("report.payment"),    hFont, true);
                addCell(table, I18n.t("report.total"),      hFont, true);
                addCell(table, I18n.t("report.status"),     hFont, true);

                BigDecimal grandTotal = BigDecimal.ZERO;
                int idx = 1;
                for (Sale s : sales) {
                    addCell(table, String.valueOf(idx++),                                      rFont, false);
                    addCell(table, safe(s.getReceiptNumber()),                                 rFont, false);
                    addCell(table, shortDate(s.getCreatedAt()),                               rFont, false);
                    addCell(table, safe(s.getCashierName()),                                   rFont, false);
                    addCell(table, safe(s.getPaymentMethod()),                                 rFont, false);
                    addCell(table, "₮" + fmt(s.getTotal()),                              rFont, false);
                    addCell(table, s.isRefunded()
                            ? I18n.t("report.status.refunded") : I18n.t("report.status.sold"), rFont, false);
                    if (s.getTotal() != null) grandTotal = grandTotal.add(s.getTotal());
                }
                doc.add(table);

                doc.add(Chunk.NEWLINE);
                Paragraph summary = new Paragraph(
                        MessageFormat.format(I18n.t("report.summary"),
                                sales.size(), "₮" + fmt(grandTotal)), sumFont);
                summary.setAlignment(Element.ALIGN_RIGHT);
                doc.add(summary);

                doc.close();
                return out;
            }

            @Override protected void done() {
                try {
                    File f = get();
                    view.showToast(I18n.t("report.pdf.saved"));
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f);
                } catch (Exception ex) {
                    view.showToast("Export failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private BaseFont unicodeBaseFont(String path) {
        try {
            return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            return null;
        }
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
