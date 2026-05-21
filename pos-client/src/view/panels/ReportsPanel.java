package view.panels;

import i18n.I18n;
import i18n.LanguageListener;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportsPanel extends JPanel implements LanguageListener {

    // ── palette ──────────────────────────────────────────────────────────────
    private static final Color ACCENT   = new Color(0x7a1a1a);
    private static final Color BG       = new Color(0xF0F2F5);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color TXT      = new Color(0x1E293B);
    private static final Color MUTED    = new Color(0x64748B);
    private static final Color SEP      = new Color(0xF1F5F9);
    private static final Color GREEN    = new Color(0x16A34A);
    private static final Color GREEN_BG = new Color(0xDCFCE7);
    private static final Color RED_CLR  = new Color(0xDC2626);
    private static final Color RED_BG   = new Color(0xFEE2E2);
    private static final Color HOVER    = new Color(0xFDF9F9);
    private static final Color STRIPE   = new Color(0xFAFAFA);

    // ── column widths ────────────────────────────────────────────────────────
    // COL_REC and COL_CASH are minimum/preferred for flexible columns;
    // they grow to fill available width via BoxLayout X_AXIS.
    private static final int COL_IDX  = 40;
    private static final int COL_REC  = 140;  // flexible (preferred min)
    private static final int COL_DATE = 110;
    private static final int COL_CASH = 120;  // flexible (preferred min)
    private static final int COL_PAY  = 140;
    private static final int COL_TOT  = 140;
    private static final int COL_STS  = 110;
    private static final int ROW_H    = 50;
    private static final int HDR_H    = 36;
    private static final int ROW_PAD  = 14;

    @FunctionalInterface public interface ReportLoader { void load(String startDate, String endDate); }
    @FunctionalInterface public interface PdfExporter  { void export(List<Sale> sales); }

    // ── state ─────────────────────────────────────────────────────────────────
    private final User           user;
    private ReportLoader         reportLoader;
    private PdfExporter          pdfExporter;
    private List<Sale>           currentSales = new ArrayList<>();

    // ── controls ──────────────────────────────────────────────────────────────
    private JFormattedTextField fromField, toField;
    private JPanel              saleListPanel;
    private JLabel              summaryLabel;
    private JLabel              lblTitle;
    private JButton             exportBtn, applyBtn, resetBtn;
    private JLabel              toastLabel;
    private javax.swing.Timer   toastTimer;

    public ReportsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
    }

    public void setReportLoader(ReportLoader loader) { this.reportLoader = loader; }
    public void setPdfExporter(PdfExporter exporter) { this.pdfExporter = exporter; }

    // ── build ─────────────────────────────────────────────────────────────────
    private void build() {
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new EmptyBorder(18, 28, 18, 24));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD_BG);
        lblTitle = new JLabel(I18n.t("report.title"));
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 22));
        lblTitle.setForeground(TXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(lblTitle);

        exportBtn = accentBtn(I18n.t("report.export.pdf"));
        exportBtn.addActionListener(e -> { if (pdfExporter != null) pdfExporter.export(currentSales); });

        bar.add(left,      BorderLayout.WEST);
        bar.add(exportBtn, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BG);
        wrapper.add(bar,         BorderLayout.CENTER);
        wrapper.add(separator(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildContent() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(16, 24, 0, 24));
        outer.add(buildFilterBar(), BorderLayout.NORTH);
        outer.add(buildTable(),     BorderLayout.CENTER);
        return outer;
    }

    // ── filter bar ────────────────────────────────────────────────────────────
    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 64));

        JLabel fromLbl = filterLabel(I18n.t("report.from"));
        JLabel toLbl   = filterLabel(I18n.t("report.to"));
        fromField = dateField();
        toField   = dateField();

        LocalDate today    = LocalDate.now();
        LocalDate firstOfM = today.withDayOfMonth(1);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        fromField.setText(firstOfM.format(f));
        toField.setText(today.format(f));

        applyBtn = ghostBtn(I18n.t("report.apply"));
        applyBtn.addActionListener(e -> applyFilter());
        resetBtn = ghostBtn(I18n.t("report.reset"));
        resetBtn.addActionListener(e -> { fromField.setText(firstOfM.format(f)); toField.setText(today.format(f)); applyFilter(); });

        int x = 16;
        fromLbl.setBounds(x, 12, 60, 16);   x += 64;
        fromField.setBounds(x, 8, 130, 32); x += 138;
        toLbl.setBounds(x, 12, 40, 16);      x += 44;
        toField.setBounds(x, 8, 130, 32);   x += 138;
        applyBtn.setBounds(x, 10, 80, 30);  x += 88;
        resetBtn.setBounds(x, 10, 80, 30);

        bar.add(fromLbl); bar.add(fromField);
        bar.add(toLbl);   bar.add(toField);
        bar.add(applyBtn); bar.add(resetBtn);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(0, 0, 8, 0));
        wrapper.add(bar, BorderLayout.CENTER);
        return wrapper;
    }

    // ── table ─────────────────────────────────────────────────────────────────
    private JPanel buildTable() {
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.add(buildColumnHeader(), BorderLayout.NORTH);

        saleListPanel = new ScrollableList();
        saleListPanel.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(saleListPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(CARD_BG);
        view.MainFrame.modernScrollBar(scroll);
        tableCard.add(scroll, BorderLayout.CENTER);
        return tableCard;
    }

    private JPanel buildColumnHeader() {
        JPanel p = makeRowPanel(new Color(0xF8FAFC), HDR_H);
        p.add(hdrCell("#",                        COL_IDX,  COL_IDX));
        p.add(hdrCell(I18n.t("report.receipt"),   COL_REC,  Integer.MAX_VALUE));
        p.add(hdrCell(I18n.t("report.date"),       COL_DATE, COL_DATE));
        p.add(hdrCell(I18n.t("report.cashier"),   COL_CASH, Integer.MAX_VALUE));
        p.add(hdrCell(I18n.t("report.payment"),   COL_PAY,  COL_PAY));
        p.add(hdrCell(I18n.t("report.total"),      COL_TOT,  COL_TOT));
        p.add(hdrCell(I18n.t("report.status"),    COL_STS,  COL_STS));
        p.add(Box.createHorizontalStrut(ROW_PAD));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SEP));
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD_BG);
        footer.setBorder(new EmptyBorder(10, 28, 14, 28));

        toastLabel = new JLabel("", SwingConstants.LEFT);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLabel.setForeground(GREEN);
        toastLabel.setVisible(false);

        summaryLabel = new JLabel("0 sales · Total: ₮0");
        summaryLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        summaryLabel.setForeground(MUTED);

        footer.add(toastLabel,   BorderLayout.WEST);
        footer.add(summaryLabel, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BG);
        wrapper.add(separator(), BorderLayout.NORTH);
        wrapper.add(footer,      BorderLayout.CENTER);
        return wrapper;
    }

    // ── public setters ────────────────────────────────────────────────────────
    public void setSaleData(List<Sale> sales) {
        SwingUtilities.invokeLater(() -> {
            this.currentSales = sales != null ? sales : new ArrayList<>();
            saleListPanel.removeAll();

            if (currentSales.isEmpty()) {
                JLabel empty = new JLabel(I18n.t("report.empty"), SwingConstants.CENTER);
                empty.setFont(new Font("Dialog", Font.PLAIN, 14));
                empty.setForeground(MUTED);
                empty.setBorder(new EmptyBorder(40, 0, 40, 0));
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                saleListPanel.add(empty);
            } else {
                BigDecimal grandTotal = BigDecimal.ZERO;
                int idx = 0;
                for (Sale s : currentSales) {
                    saleListPanel.add(buildSaleRow(s, idx));
                    saleListPanel.add(rowSep());
                    if (s.getTotal() != null) grandTotal = grandTotal.add(s.getTotal());
                    idx++;
                }
                String summary = MessageFormat.format(I18n.t("report.summary"),
                        currentSales.size(), "₮" + formatMoney(grandTotal));
                summaryLabel.setText(summary);
            }

            saleListPanel.revalidate();
            saleListPanel.repaint();
        });
    }

    public void showError(String msg) {
        SwingUtilities.invokeLater(() -> {
            saleListPanel.removeAll();
            JLabel err = new JLabel(msg, SwingConstants.CENTER);
            err.setFont(new Font("Dialog", Font.PLAIN, 13));
            err.setForeground(RED_CLR);
            err.setBorder(new EmptyBorder(40, 0, 40, 0));
            err.setAlignmentX(Component.CENTER_ALIGNMENT);
            saleListPanel.add(err);
            saleListPanel.revalidate();
            saleListPanel.repaint();
        });
    }

    public void showToast(String msg) {
        toastLabel.setText(msg);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3000, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    public List<Sale> getCurrentSales() { return currentSales; }

    // ── row builder ───────────────────────────────────────────────────────────
    private JPanel buildSaleRow(Sale s, int rowIndex) {
        Color bg = rowIndex % 2 == 1 ? STRIPE : CARD_BG;

        JPanel row = makeRowPanel(bg, ROW_H);
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(HOVER); row.repaint(); }
            public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(bg);   row.repaint(); }
        });

        Font plain = new Font("Dialog", Font.PLAIN, 13);
        Font bold  = new Font("Dialog", Font.BOLD,  13);

        boolean refunded = s.isRefunded();
        JLabel badge = statusBadge(
            refunded ? I18n.t("report.status.refunded") : I18n.t("report.status.sold"),
            refunded ? RED_BG : GREEN_BG,
            refunded ? RED_CLR : GREEN
        );
        badge.setPreferredSize(new Dimension(COL_STS, ROW_H));
        badge.setMinimumSize(new Dimension(COL_STS, ROW_H));
        badge.setMaximumSize(new Dimension(COL_STS, ROW_H));

        row.add(dataCell(String.valueOf(rowIndex + 1),              COL_IDX, COL_IDX,           plain, MUTED,  SwingConstants.CENTER));
        row.add(dataCell(nullSafe(s.getReceiptNumber(), "—"),       COL_REC, Integer.MAX_VALUE,  bold,  TXT,    SwingConstants.LEFT));
        row.add(dataCell(shortDate(s.getCreatedAt()),               COL_DATE, COL_DATE,          plain, MUTED,  SwingConstants.LEFT));
        row.add(dataCell(nullSafe(s.getCashierName(), "—"),         COL_CASH, Integer.MAX_VALUE, plain, TXT,    SwingConstants.LEFT));
        row.add(dataCell(nullSafe(s.getPaymentMethod(), "—"),       COL_PAY,  COL_PAY,           plain, MUTED,  SwingConstants.LEFT));
        row.add(dataCell("₮" + formatMoney(s.getTotal()),           COL_TOT,  COL_TOT,           bold,  ACCENT, SwingConstants.LEFT));
        row.add(badge);
        row.add(Box.createHorizontalStrut(ROW_PAD));
        return row;
    }

    // ── row / cell helpers ────────────────────────────────────────────────────
    private static JPanel makeRowPanel(Color bg, int height) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setBackground(bg);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        p.setPreferredSize(new Dimension(0, height));
        p.add(Box.createHorizontalStrut(ROW_PAD));
        return p;
    }

    private static JLabel hdrCell(String text, int prefW, int maxW) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Dialog", Font.BOLD, 10));
        l.setForeground(MUTED);
        l.setHorizontalAlignment(SwingConstants.LEFT);
        l.setPreferredSize(new Dimension(prefW, HDR_H));
        l.setMinimumSize(new Dimension(Math.min(prefW, 30), HDR_H));
        l.setMaximumSize(new Dimension(maxW, HDR_H));
        return l;
    }

    private static JLabel dataCell(String text, int prefW, int maxW, Font font, Color fg, int align) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        l.setHorizontalAlignment(align);
        l.setPreferredSize(new Dimension(prefW, ROW_H));
        l.setMinimumSize(new Dimension(Math.min(prefW, 30), ROW_H));
        l.setMaximumSize(new Dimension(maxW, ROW_H));
        return l;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JLabel statusBadge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, (getHeight() - 22) / 2, getWidth(), 22, 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setForeground(fg);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setOpaque(false);
        return l;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setForeground(MUTED);
        return l;
    }

    private JFormattedTextField dateField() {
        JFormattedTextField f;
        try {
            MaskFormatter mask = new MaskFormatter("####-##-##");
            mask.setPlaceholderCharacter('0');
            f = new JFormattedTextField(mask);
        } catch (Exception e) { f = new JFormattedTextField(); }
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setForeground(TXT);
        f.setBackground(new Color(0xF8FAFC));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1, true),
            new EmptyBorder(4, 10, 4, 10)));
        return f;
    }

    private JButton accentBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT.darker() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(8, 18, 8, 18));
        return b;
    }

    private JButton ghostBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(0xE2E8F0) : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xCBD5E1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Dialog", Font.PLAIN, 13));
        b.setForeground(TXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4, 14, 4, 14));
        return b;
    }

    private JPanel rowSep() {
        JPanel s = new JPanel();
        s.setBackground(SEP);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(SEP);
        return sep;
    }

    private void applyFilter() {
        if (reportLoader != null) {
            String from = fromField.getText().trim();
            String to   = toField.getText().trim();
            reportLoader.load(
                from.equals("0000-00-00") || from.isBlank() ? null : from,
                to.equals("0000-00-00")   || to.isBlank()   ? null : to
            );
        }
    }

    private String nullSafe(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private String shortDate(String s) {
        if (s == null) return "—";
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) return "0";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(0);
        return nf.format(val);
    }

    @Override public void onLanguageChanged() {
        if (lblTitle  != null) lblTitle.setText(I18n.t("report.title"));
        if (exportBtn != null) exportBtn.setText(I18n.t("report.export.pdf"));
        if (applyBtn  != null) applyBtn.setText(I18n.t("report.apply"));
        if (resetBtn  != null) resetBtn.setText(I18n.t("report.reset"));
    }

    // Y_AXIS list panel that tells the JScrollPane to always match viewport width,
    // so the X_AXIS rows inside get the full width and every column is visible.
    private static final class ScrollableList extends JPanel implements Scrollable {
        ScrollableList() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int  getScrollableUnitIncrement(Rectangle r, int o, int d)  { return 16; }
        @Override public int  getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth()  { return true;  }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
