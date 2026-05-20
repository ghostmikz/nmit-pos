package view.panels;

import i18n.I18n;
import i18n.LanguageListener;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportsPanel extends JPanel implements LanguageListener {

    // ── palette ─────────────────────────────────────────────────────────────
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
    private static final int COL_REC  = 130;
    private static final int COL_DATE = 130;
    private static final int COL_CASH = 120;
    private static final int COL_PAY  = 110;
    private static final int COL_DISC = 110;
    private static final int COL_SUB  = 100;
    private static final int COL_DIS  = 90;
    private static final int COL_TOT  = 100;
    private static final int COL_STS  = 100;
    private static final int ROW_H    = 50;
    private static final int ROW_PAD  = 16;

    @FunctionalInterface
    public interface ReportLoader { void load(String startDate, String endDate); }
    @FunctionalInterface
    public interface PdfExporter  { void export(List<Sale> sales); }

    // ── state ────────────────────────────────────────────────────────────────
    private final User user;
    private ReportLoader reportLoader;
    private PdfExporter  pdfExporter;
    private List<Sale>   currentSales = new ArrayList<>();

    // ── controls ─────────────────────────────────────────────────────────────
    private JFormattedTextField fromField, toField;
    private JPanel              saleListPanel;
    private JLabel              summaryLabel;
    private JLabel              lblTitle;
    private JButton             exportBtn, applyBtn, resetBtn;
    private JLabel              toastLabel;
    private Timer               toastTimer;

    public ReportsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
    }

    public void setReportLoader(ReportLoader loader) { this.reportLoader = loader; }
    public void setPdfExporter(PdfExporter exporter) { this.pdfExporter = exporter; }

    // ─────────────────────────────────────────────────────────────────────────
    private void build() {
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildContent(),   BorderLayout.CENTER);
        add(buildFooter(),    BorderLayout.SOUTH);
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
        wrapper.add(bar,          BorderLayout.CENTER);
        wrapper.add(separator(),  BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildContent() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(16, 24, 0, 24));

        outer.add(buildFilterBar(), BorderLayout.NORTH);
        outer.add(buildTable(),     BorderLayout.CENTER);
        return outer;
    }

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

        // Labels
        JLabel fromLbl = filterLabel(I18n.t("report.from"));
        JLabel toLbl   = filterLabel(I18n.t("report.to"));

        // Date fields
        fromField = dateField();
        toField   = dateField();

        // Defaults: first day of current month → today
        LocalDate today     = LocalDate.now();
        LocalDate firstOfM  = today.withDayOfMonth(1);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        fromField.setText(firstOfM.format(f));
        toField.setText(today.format(f));

        applyBtn = ghostBtn(I18n.t("report.apply"));
        applyBtn.addActionListener(e -> applyFilter());

        resetBtn = ghostBtn(I18n.t("report.reset"));
        resetBtn.addActionListener(e -> {
            fromField.setText(firstOfM.format(f));
            toField.setText(today.format(f));
            applyFilter();
        });

        // Layout
        int x = 16;
        fromLbl.setBounds(x, 12, 60, 16); x += 64;
        fromField.setBounds(x, 8, 130, 32); x += 138;
        toLbl.setBounds(x, 12, 40, 16); x += 44;
        toField.setBounds(x, 8, 130, 32); x += 138;
        applyBtn.setBounds(x, 10, 80, 30); x += 88;
        resetBtn.setBounds(x, 10, 80, 30);

        bar.add(fromLbl); bar.add(fromField);
        bar.add(toLbl);   bar.add(toField);
        bar.add(applyBtn); bar.add(resetBtn);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(0, 0, 12, 0));
        wrapper.add(bar, BorderLayout.CENTER);
        return wrapper;
    }

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

        saleListPanel = new JPanel();
        saleListPanel.setLayout(new BoxLayout(saleListPanel, BoxLayout.Y_AXIS));
        saleListPanel.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(saleListPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(CARD_BG);
        tableCard.add(scroll, BorderLayout.CENTER);

        return tableCard;
    }

    private JPanel buildColumnHeader() {
        return layoutRow(
            new String[]{"#", I18n.t("report.receipt"), I18n.t("report.date"),
                         I18n.t("report.cashier"), I18n.t("report.payment"),
                         I18n.t("report.discount"), I18n.t("report.subtotal"),
                         I18n.t("report.discount"), I18n.t("report.total"),
                         I18n.t("report.status")},
            CARD_BG, MUTED, new Font("Dialog", Font.BOLD, 10), true
        );
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

        footer.add(toastLabel,    BorderLayout.WEST);
        footer.add(summaryLabel,  BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BG);
        wrapper.add(separator(), BorderLayout.NORTH);
        wrapper.add(footer,      BorderLayout.CENTER);
        return wrapper;
    }

    // ── public data setters ───────────────────────────────────────────────────
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
                BigDecimal total = BigDecimal.ZERO;
                int idx = 0;
                for (Sale s : currentSales) {
                    saleListPanel.add(buildSaleRow(s, idx));
                    saleListPanel.add(rowSep());
                    if (s.getTotal() != null) total = total.add(s.getTotal());
                    idx++;
                }
                BigDecimal finalTotal = total;
                String summary = MessageFormat.format(I18n.t("report.summary"),
                        currentSales.size(), "₮" + formatMoney(finalTotal));
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
        toastTimer = new Timer(3000, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    public List<Sale> getCurrentSales() { return currentSales; }

    // ── row builder ───────────────────────────────────────────────────────────
    private JPanel buildSaleRow(Sale s, int rowIndex) {
        Color bg = rowIndex % 2 == 1 ? STRIPE : CARD_BG;

        String receipt  = nullSafe(s.getReceiptNumber(), "—");
        String date     = shortDate(s.getCreatedAt());
        String cashier  = nullSafe(s.getCashierName(), "—");
        String payment  = nullSafe(s.getPaymentMethod(), "—");
        String discount = nullSafe(s.getDiscountName(), "—");
        String subtotal = "₮" + formatMoney(s.getSubtotal());
        String discAmt  = s.getDiscountAmount() != null && s.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                          ? "-₮" + formatMoney(s.getDiscountAmount()) : "—";
        String total    = "₮" + formatMoney(s.getTotal());

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(bg); }
        });

        JPanel inner = new JPanel(null) {
            @Override public void doLayout() {
                int x = ROW_PAD, h = getHeight();
                int n = getComponentCount();
                // widths matching column order
                int[] widths = { 36, COL_REC, COL_DATE, COL_CASH, COL_PAY,
                                 COL_DISC, COL_SUB, COL_DIS, COL_TOT, COL_STS };
                for (int i = 0; i < n; i++) {
                    Component c = getComponent(i);
                    int w = (i < widths.length) ? widths[i] : 80;
                    c.setBounds(x, 0, w, h);
                    x += w;
                }
            }
        };
        inner.setBackground(bg);
        inner.setBorder(new EmptyBorder(0, 0, 0, ROW_PAD));

        inner.add(rowLabel(String.valueOf(rowIndex + 1), MUTED, Font.PLAIN));
        inner.add(rowLabel(receipt,  TXT,  Font.BOLD));
        inner.add(rowLabel(date,     MUTED, Font.PLAIN));
        inner.add(rowLabel(cashier,  TXT,  Font.PLAIN));
        inner.add(rowLabel(payment,  MUTED, Font.PLAIN));
        inner.add(rowLabel(discount, MUTED, Font.PLAIN));
        inner.add(rowLabel(subtotal, TXT,  Font.PLAIN));
        inner.add(rowLabel(discAmt,  new Color(0xD97706), Font.PLAIN));
        inner.add(rowLabel(total,    ACCENT, Font.BOLD));

        // status badge
        boolean refunded  = s.isRefunded();
        Color badgeBg     = refunded ? RED_BG   : GREEN_BG;
        Color badgeFg     = refunded ? RED_CLR  : GREEN;
        String badgeText  = refunded ? I18n.t("report.status.refunded") : I18n.t("report.status.sold");
        inner.add(statusBadge(badgeText, badgeBg, badgeFg));

        row.add(inner, BorderLayout.CENTER);
        return row;
    }

    // ── layout helper (shared for header + rows) ──────────────────────────────
    private JPanel layoutRow(String[] texts, Color bg, Color fg, Font font, boolean isHeader) {
        JPanel row = new JPanel(null) {
            @Override public void doLayout() {
                int x = ROW_PAD, h = getHeight();
                int[] widths = { 36, COL_REC, COL_DATE, COL_CASH, COL_PAY,
                                 COL_DISC, COL_SUB, COL_DIS, COL_TOT, COL_STS };
                for (int i = 0; i < getComponentCount(); i++) {
                    Component c = getComponent(i);
                    int w = (i < widths.length) ? widths[i] : 80;
                    c.setBounds(x, 0, w, h);
                    x += w;
                }
            }
        };
        row.setBackground(bg);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 36 : ROW_H));
        row.setPreferredSize(new Dimension(Integer.MAX_VALUE, isHeader ? 36 : ROW_H));
        row.setBorder(isHeader
            ? new EmptyBorder(0, 0, 0, ROW_PAD)
            : new EmptyBorder(0, 0, 0, ROW_PAD));

        for (String t : texts) {
            JLabel l = new JLabel(isHeader ? t.toUpperCase() : t);
            l.setFont(font);
            l.setForeground(fg);
            row.add(l);
        }
        return row;
    }

    // ── component helpers ─────────────────────────────────────────────────────
    private JLabel rowLabel(String text, Color fg, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", style, 13));
        l.setForeground(fg);
        return l;
    }

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
        } catch (Exception e) {
            f = new JFormattedTextField();
        }
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setForeground(TXT);
        f.setBackground(new Color(0xF8FAFC));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1, true),
            new EmptyBorder(4, 10, 4, 10)));
        f.setColumns(10);
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

    private String shortDate(String createdAt) {
        if (createdAt == null) return "—";
        // typically "2025-05-20 14:32:00" — return first 10 chars
        return createdAt.length() >= 10 ? createdAt.substring(0, 10) : createdAt;
    }

    private String formatMoney(BigDecimal val) {
        if (val == null) return "0";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(0);
        return nf.format(val);
    }

    @Override
    public void onLanguageChanged() {
        if (lblTitle  != null) lblTitle.setText(I18n.t("report.title"));
        if (exportBtn != null) exportBtn.setText(I18n.t("report.export.pdf"));
        if (applyBtn  != null) applyBtn.setText(I18n.t("report.apply"));
        if (resetBtn  != null) resetBtn.setText(I18n.t("report.reset"));
    }
}
