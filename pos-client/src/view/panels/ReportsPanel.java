package view.panels;

import static view.AppColors.*;

import i18n.I18n;
import i18n.LanguageListener;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
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

    private static final Color BG      = new Color(0xF5F6F8);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color HDR_BG  = new Color(0xF8FAFC);
    private static final Color HOVER   = new Color(0xFDF8F8);
    private static final Color STRIPE  = new Color(0xFAFAFA);

    private static final int PAD   = 20;
    private static final int HDR_H = 36;

    @FunctionalInterface public interface ReportLoader { void load(String from, String to); }
    @FunctionalInterface public interface PdfExporter  { void export(List<Sale> sales); }

    private final User user;
    private ReportLoader  reportLoader;
    private PdfExporter   pdfExporter;
    private List<Sale>    currentSales = new ArrayList<>();

    private JFormattedTextField fromField, toField;
    private JLabel              summaryLbl, lblTitle, fromLbl, toLbl;
    private JButton             exportBtn, applyBtn, resetBtn;
    private JLabel              toastLbl;
    private javax.swing.Timer   toastTimer;

    private JTable            table;
    private DefaultTableModel tableModel;
    private CardLayout        cardLayout;
    private JPanel            cardPanel;
    private JLabel            stateLabel;

    public ReportsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
    }

    public void setReportLoader(ReportLoader l) { this.reportLoader = l; }
    public void setPdfExporter(PdfExporter e)   { this.pdfExporter  = e; }

    private void build() {
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── header: title + inline filters ───────────────────────────────────────
    private JPanel buildHeader() {
        LocalDate today    = LocalDate.now();
        LocalDate firstOfM = today.withDayOfMonth(1);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        fromField = dateField(); fromField.setText(firstOfM.format(df));
        toField   = dateField(); toField.setText(today.format(df));
        fromLbl   = mLbl(I18n.t("report.from"));
        toLbl     = mLbl(I18n.t("report.to"));

        applyBtn  = accentBtn(I18n.t("report.apply"));
        resetBtn  = outlineBtn(I18n.t("report.reset"));
        exportBtn = outlineBtn(I18n.t("report.export.pdf"));

        applyBtn.addActionListener(e -> applyFilter());
        resetBtn.addActionListener(e -> {
            fromField.setText(firstOfM.format(df));
            toField.setText(today.format(df));
            applyFilter();
        });
        exportBtn.addActionListener(e -> { if (pdfExporter != null) pdfExporter.export(currentSales); });

        lblTitle = new JLabel(I18n.t("report.title"));
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        lblTitle.setForeground(TXT);

        JPanel filterRow = new JPanel();
        filterRow.setLayout(new BoxLayout(filterRow, BoxLayout.X_AXIS));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        Dimension fSz = new Dimension(120, 32);
        fromField.setPreferredSize(fSz); fromField.setMaximumSize(fSz);
        toField.setPreferredSize(fSz);   toField.setMaximumSize(fSz);

        filterRow.add(fromLbl);
        filterRow.add(Box.createHorizontalStrut(8));
        filterRow.add(fromField);
        filterRow.add(Box.createHorizontalStrut(14));
        filterRow.add(toLbl);
        filterRow.add(Box.createHorizontalStrut(8));
        filterRow.add(toField);
        filterRow.add(Box.createHorizontalStrut(14));
        filterRow.add(applyBtn);
        filterRow.add(Box.createHorizontalStrut(8));
        filterRow.add(resetBtn);
        filterRow.add(Box.createHorizontalGlue());
        filterRow.add(exportBtn);

        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setBackground(CARD_BG);
        hdr.setBorder(new EmptyBorder(20, PAD + 4, 16, PAD + 4));
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        hdr.add(lblTitle);
        hdr.add(Box.createVerticalStrut(14));
        hdr.add(filterRow);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD_BG);
        wrap.add(hdr, BorderLayout.CENTER);
        wrap.add(divider(), BorderLayout.SOUTH);
        return wrap;
    }

    // ── content: JTable ───────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(16, 16, 0, 16));

        JPanel card = roundedCard(CARD_BG, 12);
        card.setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        initTable();

        // Detach header from JScrollPane so roundedCard's clip applies to it correctly
        JTableHeader hdr = table.getTableHeader();
        hdr.setBorder(null);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(null);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(CARD_BG);
        view.MainFrame.modernScrollBar(scroll);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(hdr,    BorderLayout.NORTH);
        tableWrap.add(scroll, BorderLayout.CENTER);

        stateLabel = new JLabel(I18n.t("report.empty"), SwingConstants.CENTER);
        stateLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        stateLabel.setForeground(MUTED);
        JPanel statePanel = new JPanel(new BorderLayout());
        statePanel.setBackground(CARD_BG);
        statePanel.add(stateLabel, BorderLayout.CENTER);

        cardPanel.add(tableWrap, "TABLE");
        cardPanel.add(statePanel, "STATE");
        cardLayout.show(cardPanel, "STATE");

        card.add(cardPanel, BorderLayout.CENTER);
        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private void initTable() {
        tableModel = new DefaultTableModel(colNames(), 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            @Override public String getToolTipText(java.awt.event.MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = convertColumnIndexToModel(columnAtPoint(e.getPoint()));
                if (row >= 0 && col == 3) {
                    Object v = tableModel.getValueAt(row, 3);
                    if (v instanceof Object[] a && a.length > 1 && a[1] != null)
                        return (String) a[1];
                }
                return null;
            }
        };
        table.setRowHeight(44);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setBackground(CARD_BG);
        table.setSelectionBackground(HOVER);
        table.setSelectionForeground(TXT);
        table.setFocusable(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        JTableHeader hdr = table.getTableHeader();
        hdr.setBackground(HDR_BG);
        hdr.setPreferredSize(new Dimension(0, HDR_H));
        hdr.setReorderingAllowed(true);
        hdr.setResizingAllowed(true);
        hdr.setDefaultRenderer(new HdrCR());

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(160); cm.getColumn(0).setMinWidth(60);
        cm.getColumn(1).setPreferredWidth(100); cm.getColumn(1).setMinWidth(60);
        cm.getColumn(2).setPreferredWidth(100); cm.getColumn(2).setMinWidth(60);
        cm.getColumn(3).setPreferredWidth(120); cm.getColumn(3).setMinWidth(60);
        cm.getColumn(4).setPreferredWidth(110); cm.getColumn(4).setMinWidth(60);
        cm.getColumn(5).setPreferredWidth(90);  cm.getColumn(5).setMinWidth(60);

        cm.getColumn(0).setCellRenderer(new TextCR(true,  false));
        cm.getColumn(1).setCellRenderer(new TextCR(false, true));
        cm.getColumn(2).setCellRenderer(new TextCR(false, false));
        cm.getColumn(3).setCellRenderer(new PaymentCR());
        cm.getColumn(4).setCellRenderer(new TotalCR());
        cm.getColumn(5).setCellRenderer(new StatusCR());
    }

    private String[] colNames() {
        return new String[]{
            I18n.t("report.receipt"),
            I18n.t("report.date"),
            I18n.t("report.cashier"),
            I18n.t("report.payment"),
            I18n.t("report.total"),
            I18n.t("report.status")
        };
    }

    // ── footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        toastLbl = new JLabel("", SwingConstants.LEFT);
        toastLbl.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLbl.setForeground(GREEN);
        toastLbl.setVisible(false);

        summaryLbl = new JLabel("");
        summaryLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        summaryLbl.setForeground(MUTED);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(CARD_BG);
        footer.setBorder(new EmptyBorder(10, PAD + 4, 14, PAD + 4));
        footer.add(toastLbl,   BorderLayout.WEST);
        footer.add(summaryLbl, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD_BG);
        wrap.add(divider(), BorderLayout.NORTH);
        wrap.add(footer,    BorderLayout.CENTER);
        return wrap;
    }

    // ── public API ────────────────────────────────────────────────────────────
    public void setSaleData(List<Sale> sales) {
        SwingUtilities.invokeLater(() -> {
            currentSales = sales != null ? sales : new ArrayList<>();
            tableModel.setRowCount(0);

            if (currentSales.isEmpty()) {
                stateLabel.setText(I18n.t("report.empty"));
                stateLabel.setForeground(MUTED);
                cardLayout.show(cardPanel, "STATE");
                summaryLbl.setText("");
            } else {
                BigDecimal grand = BigDecimal.ZERO;
                for (Sale s : currentSales) {
                    tableModel.addRow(new Object[]{
                        safe(s.getReceiptNumber(), "—"),
                        date(s.getCreatedAt()),
                        safe(s.getCashierName(), "—"),
                        s.getPaymentMethod(),
                        "₮" + money(s.getTotal()),
                        s.isRefunded()
                    });
                    if (s.getTotal() != null) grand = grand.add(s.getTotal());
                }
                cardLayout.show(cardPanel, "TABLE");
                summaryLbl.setText(MessageFormat.format(I18n.t("report.summary"),
                        currentSales.size(), "₮" + money(grand)));
            }
        });
    }

    public void showError(String msg) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            stateLabel.setText(msg);
            stateLabel.setForeground(RED);
            cardLayout.show(cardPanel, "STATE");
        });
    }

    public void showToast(String msg) {
        toastLbl.setText(msg);
        toastLbl.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(3000, e -> toastLbl.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    public List<Sale> getCurrentSales() { return currentSales; }

    // ── cell renderers ────────────────────────────────────────────────────────
    private static class TextCR extends DefaultTableCellRenderer {
        private final boolean bold, mono;
        TextCR(boolean bold, boolean mono) { this.bold = bold; this.mono = mono; setHorizontalAlignment(LEFT); }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setFont(mono
                ? new Font("Monospaced", Font.PLAIN, 12)
                : new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, 13));
            setForeground(bold ? TXT : MUTED);
            setBackground(sel ? HOVER : CARD_BG);
            setBorder(new EmptyBorder(0, 8, 0, 4));
            return this;
        }
    }

    private static class TotalCR extends DefaultTableCellRenderer {
        TotalCR() { setHorizontalAlignment(LEFT); }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setFont(new Font("Dialog", Font.BOLD, 13));
            setForeground(ACCENT);
            setBackground(sel ? HOVER : CARD_BG);
            setBorder(new EmptyBorder(0, 8, 0, 4));
            return this;
        }
    }

    private static class PaymentCR extends DefaultTableCellRenderer {
        PaymentCR() { setHorizontalAlignment(LEFT); }

        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v != null ? v : "", sel, foc, r, c);
            setFont(new Font("Dialog", Font.PLAIN, 13));
            setForeground(MUTED);
            setBackground(sel ? HOVER : CARD_BG);
            setBorder(new EmptyBorder(0, 8, 0, 4));
            return this;
        }
    }

    private static class StatusCR extends DefaultTableCellRenderer {
        StatusCR() { setHorizontalAlignment(LEFT); }

        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            boolean ref = Boolean.TRUE.equals(v);
            String display = ref ? I18n.t("report.status.refunded") : I18n.t("report.status.sold");

            super.getTableCellRendererComponent(t, display, sel, foc, r, c);
            setFont(new Font("Dialog", Font.PLAIN, 13));
            setForeground(MUTED);
            setBackground(sel ? HOVER : CARD_BG);
            setBorder(new EmptyBorder(0, 8, 0, 4));
            return this;
        }
    }

    private static class HdrCR extends DefaultTableCellRenderer {
        HdrCR() { setHorizontalAlignment(LEFT); setOpaque(true); }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setText(v != null ? v.toString().toUpperCase() : "");
            setFont(new Font("Dialog", Font.BOLD, 10));
            setForeground(MUTED);
            setBackground(HDR_BG);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 8, 0, 4)));
            return this;
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private static JSeparator divider() {
        JSeparator s = new JSeparator();
        s.setForeground(BORDER);
        return s;
    }

    private JFormattedTextField dateField() {
        JFormattedTextField f;
        try {
            MaskFormatter m = new MaskFormatter("####-##-##");
            m.setPlaceholderCharacter('0');
            f = new JFormattedTextField(m);
        } catch (Exception ex) { f = new JFormattedTextField(); }
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setForeground(TXT);
        f.setBackground(CARD_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
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
        b.setFont(new Font("Dialog", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        return b;
    }

    private JButton outlineBtn(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? CHIP_BG : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        b.setForeground(TXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        return b;
    }

    private static JLabel mLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    private void applyFilter() {
        if (reportLoader == null) return;
        String from = fromField.getText().trim();
        String to   = toField.getText().trim();
        reportLoader.load(
            "0000-00-00".equals(from) || from.isBlank() ? null : from,
            "0000-00-00".equals(to)   || to.isBlank()   ? null : to);
    }

    private static String safe(String v, String fb) { return v == null || v.isBlank() ? fb : v; }

    private static String date(String s) {
        if (s == null) return "—";
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0";
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(0);
        return nf.format(v);
    }

    @Override public void onLanguageChanged() {
        if (lblTitle  != null) lblTitle.setText(I18n.t("report.title"));
        if (exportBtn != null) exportBtn.setText(I18n.t("report.export.pdf"));
        if (applyBtn  != null) applyBtn.setText(I18n.t("report.apply"));
        if (resetBtn  != null) resetBtn.setText(I18n.t("report.reset"));
        if (fromLbl   != null) fromLbl.setText(I18n.t("report.from"));
        if (toLbl     != null) toLbl.setText(I18n.t("report.to"));
    }

    private static JPanel roundedCard(Color bg, int r) {
        return new JPanel() {
            { setOpaque(false); }
            @Override public boolean isOpaque() { return false; }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.dispose();
            }
            @Override protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), r, r));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
    }
}
