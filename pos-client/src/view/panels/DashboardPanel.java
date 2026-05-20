package view.panels;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements LanguageListener {

    // ── palette ─────────────────────────────────────────────────────────────
    private static final Color ACCENT    = new Color(0x7a1a1a);
    private static final Color BG        = new Color(0xF0F2F5);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TXT       = new Color(0x1E293B);
    private static final Color MUTED     = new Color(0x64748B);
    private static final Color SEP       = new Color(0xF1F5F9);
    private static final Color BLUE      = new Color(0x2563EB);
    private static final Color BLUE_BG   = new Color(0xDBEAFE);
    private static final Color AMBER     = new Color(0xD97706);
    private static final Color AMBER_BG  = new Color(0xFEF3C7);
    private static final Color GREEN     = new Color(0x16A34A);
    private static final Color RED_CLR   = new Color(0xDC2626);
    private static final Color RED_BG    = new Color(0xFEE2E2);

    @FunctionalInterface
    public interface DataLoader { void load(); }

    // ── state ────────────────────────────────────────────────────────────────
    private final User user;
    private DataLoader dataLoader;

    // ── KPI labels ───────────────────────────────────────────────────────────
    private JLabel lblTxnVal, lblRevVal, lblDiscVal, lblLowVal;
    private JLabel lblTitle, lblSubtitle;
    private JLabel lblLowStockTitle, lblTopTitle, lblChartTitle;

    // ── lists ────────────────────────────────────────────────────────────────
    private JPanel lowStockList;
    private JPanel topProductsList;

    // ── chart ────────────────────────────────────────────────────────────────
    private BarChartPanel barChart;

    // ── toast ─────────────────────────────────────────────────────────────────
    private JLabel toastLabel;
    private Timer  toastTimer;

    public DashboardPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
        I18n.addListener(this);
    }

    public void setDataLoader(DataLoader loader) { this.dataLoader = loader; }

    // ─────────────────────────────────────────────────────────────────────────
    private void build() {
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new EmptyBorder(18, 28, 18, 24));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD_BG);

        lblTitle = new JLabel(I18n.t("dashboard.title"));
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 22));
        lblTitle.setForeground(TXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSubtitle = new JLabel(I18n.t("dashboard.subtitle"));
        lblSubtitle.setFont(new Font("Dialog", Font.PLAIN, 13));
        lblSubtitle.setForeground(MUTED);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(lblTitle);
        left.add(Box.createVerticalStrut(3));
        left.add(lblSubtitle);

        JButton refreshBtn = accentBtn(I18n.t("dashboard.refresh"));
        refreshBtn.addActionListener(e -> { if (dataLoader != null) dataLoader.load(); });

        bar.add(left,       BorderLayout.WEST);
        bar.add(refreshBtn, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BG);
        wrapper.add(bar,         BorderLayout.CENTER);
        wrapper.add(separator(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JScrollPane buildCenter() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildKpiRow());
        content.add(Box.createVerticalStrut(20));
        content.add(buildListsRow());
        content.add(Box.createVerticalStrut(20));
        content.add(buildChartCard());
        content.add(Box.createVerticalStrut(12));

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLabel.setForeground(GREEN);
        toastLabel.setVisible(false);
        toastLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(toastLabel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG);
        return scroll;
    }

    // ── KPI row ───────────────────────────────────────────────────────────────
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        lblTxnVal  = new JLabel("—");
        lblRevVal  = new JLabel("—");
        lblDiscVal = new JLabel("—");
        lblLowVal  = new JLabel("—");

        row.add(kpiCard(I18n.t("dashboard.kpi.transactions"), lblTxnVal,  BLUE,    BLUE_BG));
        row.add(kpiCard(I18n.t("dashboard.kpi.revenue"),      lblRevVal,  ACCENT,  RED_BG));
        row.add(kpiCard(I18n.t("dashboard.kpi.discounts"),    lblDiscVal, AMBER,   AMBER_BG));
        row.add(kpiCard(I18n.t("dashboard.kpi.low.stock"),    lblLowVal,  RED_CLR, RED_BG));
        return row;
    }

    private JPanel kpiCard(String label, JLabel valueLabel, Color accent, Color accentBg) {
        JPanel card = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(accent);
                g2.fillRect(0, 0, 5, getHeight());
                g2.dispose();
            }
        };
        card.setPreferredSize(new Dimension(200, 110));
        card.setOpaque(false);

        valueLabel.setFont(new Font("Dialog", Font.BOLD, 28));
        valueLabel.setForeground(TXT);
        valueLabel.setBounds(18, 20, 165, 36);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 12));
        lbl.setForeground(MUTED);
        lbl.setBounds(18, 60, 165, 18);

        JLabel iconDot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentBg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        iconDot.setOpaque(false);
        iconDot.setBounds(162, 18, 22, 22);

        card.add(valueLabel);
        card.add(lbl);
        card.add(iconDot);
        return card;
    }

    // ── lists row ─────────────────────────────────────────────────────────────
    private JPanel buildListsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        row.add(buildLowStockCard());
        row.add(buildTopProductsCard());
        return row;
    }

    private JPanel buildLowStockCard() {
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout());

        lblLowStockTitle = sectionTitle(I18n.t("dashboard.low.stock.title"));
        card.add(lblLowStockTitle, BorderLayout.NORTH);

        lowStockList = new JPanel();
        lowStockList.setLayout(new BoxLayout(lowStockList, BoxLayout.Y_AXIS));
        lowStockList.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(lowStockList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTopProductsCard() {
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout());

        lblTopTitle = sectionTitle(I18n.t("dashboard.top.products.title"));
        card.add(lblTopTitle, BorderLayout.NORTH);

        topProductsList = new JPanel();
        topProductsList.setLayout(new BoxLayout(topProductsList, BoxLayout.Y_AXIS));
        topProductsList.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(topProductsList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── custom bar chart (no JFreeChart needed) ───────────────────────────────
    private JPanel buildChartCard() {
        JPanel card = whiteCard();
        card.setLayout(new BorderLayout());
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setPreferredSize(new Dimension(Integer.MAX_VALUE, 260));

        lblChartTitle = sectionTitle(I18n.t("dashboard.chart.title"));
        card.add(lblChartTitle, BorderLayout.NORTH);

        barChart = new BarChartPanel();
        card.add(barChart, BorderLayout.CENTER);
        return card;
    }

    /** Lightweight bar chart drawn with Java2D — no external dependencies. */
    private static class BarChartPanel extends JPanel {
        private final List<String> labels  = new ArrayList<>();
        private final List<Double> values  = new ArrayList<>();

        BarChartPanel() {
            setBackground(CARD_BG);
            setOpaque(true);
        }

        void setData(List<String> lbls, List<Double> vals) {
            labels.clear(); labels.addAll(lbls);
            values.clear(); values.addAll(vals);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values.isEmpty()) {
                g.setFont(new Font("Dialog", Font.PLAIN, 12));
                g.setColor(MUTED);
                String msg = "No data";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padL = 60, padR = 20, padT = 16, padB = 40;
            int chartW = getWidth()  - padL - padR;
            int chartH = getHeight() - padT - padB;
            int n       = values.size();

            double maxVal = values.stream().mapToDouble(d -> d).max().orElse(1);
            if (maxVal == 0) maxVal = 1;

            int barW   = Math.max(8, chartW / (n * 2));
            int gap    = (chartW - barW * n) / (n + 1);

            // grid lines
            g2.setColor(new Color(0xF1F5F9));
            int gridLines = 4;
            for (int i = 0; i <= gridLines; i++) {
                int y = padT + (int) (chartH * i / gridLines);
                g2.drawLine(padL, y, padL + chartW, y);

                // y-axis label
                long labelVal = Math.round(maxVal * (gridLines - i) / gridLines);
                String yLbl = formatCompact(labelVal);
                g2.setFont(new Font("Dialog", Font.PLAIN, 9));
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(yLbl, padL - fm.stringWidth(yLbl) - 4,
                              y + fm.getAscent() / 2);
                g2.setColor(new Color(0xF1F5F9));
            }

            // bars + x labels
            for (int i = 0; i < n; i++) {
                int barH   = (int) (chartH * values.get(i) / maxVal);
                int x      = padL + gap + i * (barW + gap);
                int y      = padT + chartH - barH;

                // bar with rounded top
                g2.setColor(ACCENT);
                if (barH > 0) {
                    int arc = Math.min(6, barW / 2);
                    g2.fillRoundRect(x, y, barW, barH, arc, arc);
                    // square off the bottom corners
                    if (barH > arc)
                        g2.fillRect(x, y + arc, barW, barH - arc);
                }

                // x-axis label
                String lbl = i < labels.size() ? labels.get(i) : "";
                g2.setFont(new Font("Dialog", Font.PLAIN, 9));
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lbl, x + (barW - fm.stringWidth(lbl)) / 2,
                              padT + chartH + fm.getAscent() + 4);
            }

            // baseline
            g2.setColor(new Color(0xE2E8F0));
            g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);
            g2.dispose();
        }

        private String formatCompact(long v) {
            if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
            if (v >= 1_000)     return String.format("%.0fK", v / 1_000.0);
            return String.valueOf(v);
        }
    }

    // ── public data setters ───────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public void setDashboardData(Map<String, Object> data) {
        SwingUtilities.invokeLater(() -> {
            if (data == null) return;

            // daily KPIs
            Object dailyObj = data.get("daily");
            if (dailyObj instanceof Map) {
                Map<String, Object> daily = (Map<String, Object>) dailyObj;
                lblTxnVal.setText(fmt(daily.get("totalTransactions")));
                lblRevVal.setText("₮" + fmtMoney(daily.get("totalRevenue")));
                lblDiscVal.setText("₮" + fmtMoney(daily.get("totalDiscounts")));
            }

            // low stock list
            Object lowObj = data.get("lowStock");
            lowStockList.removeAll();
            int lowCount = 0;
            if (lowObj instanceof List) {
                List<Map<String, Object>> low = (List<Map<String, Object>>) lowObj;
                lowCount = low.size();
                if (low.isEmpty()) {
                    lowStockList.add(emptyLabel(I18n.t("dashboard.empty")));
                } else {
                    for (Map<String, Object> row : low) {
                        lowStockList.add(lowStockRow(row));
                        lowStockList.add(rowSep());
                    }
                }
            }
            lblLowVal.setText(String.valueOf(lowCount));

            // top products list
            Object topObj = data.get("topProducts");
            topProductsList.removeAll();
            if (topObj instanceof List) {
                List<Map<String, Object>> top = (List<Map<String, Object>>) topObj;
                if (top.isEmpty()) {
                    topProductsList.add(emptyLabel(I18n.t("dashboard.empty")));
                } else {
                    int rank = 1;
                    for (Map<String, Object> row : top) {
                        topProductsList.add(topProductRow(row, rank++));
                        topProductsList.add(rowSep());
                    }
                }
            }

            // weekly chart
            Object weeklyObj = data.get("weeklySales");
            if (weeklyObj instanceof List) {
                List<Map<String, Object>> weekly = (List<Map<String, Object>>) weeklyObj;
                List<String> lbls = new ArrayList<>();
                List<Double> vals = new ArrayList<>();
                for (Map<String, Object> row : weekly) {
                    String date = String.valueOf(row.get("saleDate"));
                    double rev  = toDouble(row.get("totalRevenue"));
                    lbls.add(date.length() >= 10 ? date.substring(5) : date);
                    vals.add(rev);
                }
                barChart.setData(lbls, vals);
            }

            lowStockList.revalidate();
            lowStockList.repaint();
            topProductsList.revalidate();
            topProductsList.repaint();
            revalidate();
            repaint();
        });
    }

    // ── row builders ─────────────────────────────────────────────────────────
    private JPanel lowStockRow(Map<String, Object> row) {
        String name = String.valueOf(row.getOrDefault("productName",  "—"));
        String cat  = String.valueOf(row.getOrDefault("categoryName", "—"));
        int    qty  = (int) Math.round(toDouble(row.get("stockQuantity")));
        String unit = String.valueOf(row.getOrDefault("unit", ""));

        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(CARD_BG);
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD_BG);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        nameLabel.setForeground(TXT);

        JLabel catLabel = new JLabel(cat);
        catLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        catLabel.setForeground(MUTED);

        left.add(nameLabel);
        left.add(catLabel);

        Color badgeBg  = qty == 0 ? RED_BG   : AMBER_BG;
        Color badgeFg  = qty == 0 ? RED_CLR  : AMBER;
        JLabel badge   = badge(qty + " " + unit, badgeBg, badgeFg);

        p.add(left,  BorderLayout.CENTER);
        p.add(badge, BorderLayout.EAST);
        return p;
    }

    private JPanel topProductRow(Map<String, Object> row, int rank) {
        String name    = String.valueOf(row.getOrDefault("productName", "—"));
        int    sold    = (int) Math.round(toDouble(row.get("totalSold")));
        double revenue = toDouble(row.get("totalRevenue"));

        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(CARD_BG);
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setBackground(CARD_BG);

        JLabel rankLabel = new JLabel("#" + rank);
        rankLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        rankLabel.setForeground(rank <= 3 ? ACCENT : MUTED);
        rankLabel.setPreferredSize(new Dimension(30, 20));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        nameLabel.setForeground(TXT);

        left.add(rankLabel);
        left.add(nameLabel);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(CARD_BG);

        JLabel revLabel  = new JLabel("₮" + fmtMoney(revenue));
        revLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        revLabel.setForeground(ACCENT);
        revLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel soldLabel = new JLabel(sold + " sold");
        soldLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        soldLabel.setForeground(MUTED);
        soldLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        right.add(revLabel);
        right.add(soldLabel);

        p.add(left,  BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private JPanel whiteCard() {
        JPanel c = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        return c;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 14));
        l.setForeground(TXT);
        l.setBorder(new EmptyBorder(14, 16, 10, 16));
        return l;
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

    private JLabel emptyLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Dialog", Font.PLAIN, 13));
        l.setForeground(MUTED);
        l.setBorder(new EmptyBorder(24, 0, 24, 0));
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private JLabel badge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setOpaque(false);
        return l;
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

    private String fmt(Object val) {
        if (val == null) return "0";
        if (val instanceof Number) return String.valueOf(((Number) val).intValue());
        return String.valueOf(val);
    }

    private String fmtMoney(Object val) {
        double d = toDouble(val);
        if (d >= 1_000_000) return String.format("%.1fM", d / 1_000_000);
        if (d >= 1_000)     return String.format("%.1fK", d / 1_000);
        return String.format("%.0f", d);
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }

    public void showToast(String msg) {
        toastLabel.setText(msg);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(2500, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    @Override
    public void onLanguageChanged() {
        if (lblTitle         != null) lblTitle.setText(I18n.t("dashboard.title"));
        if (lblSubtitle      != null) lblSubtitle.setText(I18n.t("dashboard.subtitle"));
        if (lblLowStockTitle != null) lblLowStockTitle.setText(I18n.t("dashboard.low.stock.title"));
        if (lblTopTitle      != null) lblTopTitle.setText(I18n.t("dashboard.top.products.title"));
        if (lblChartTitle    != null) lblChartTitle.setText(I18n.t("dashboard.chart.title"));
    }
}
