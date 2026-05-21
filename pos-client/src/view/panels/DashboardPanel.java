package view.panels;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements LanguageListener {

    private static final Color ACCENT    = new Color(0x7a1a1a);
    private static final Color BG        = new Color(0xF0F2F5);
    private static final Color CARD_BG   = Color.WHITE;
    private static final Color TXT       = new Color(0x1E293B);
    private static final Color MUTED     = new Color(0x64748B);
    private static final Color SEP       = new Color(0xE2E8F0);
    private static final Color BLUE      = new Color(0x2563EB);
    private static final Color BLUE_BG   = new Color(0xDBEAFE);
    private static final Color AMBER     = new Color(0xD97706);
    private static final Color AMBER_BG  = new Color(0xFEF3C7);
    private static final Color GREEN     = new Color(0x16A34A);
    private static final Color RED_CLR   = new Color(0xDC2626);
    private static final Color RED_BG    = new Color(0xFEE2E2);
    private static final Color SHADOW    = new Color(0, 0, 0, 18);
    private static final Color[] PIE_COLORS = {
        new Color(0x7a1a1a), new Color(0x2563EB), new Color(0xD97706),
        new Color(0x16A34A), new Color(0x7C3AED), new Color(0x0891B2),
        new Color(0xDB2777), new Color(0x65A30D), new Color(0xDC2626),
        new Color(0x475569)
    };

    @FunctionalInterface
    public interface DataLoader { void load(); }

    private final User user;
    private DataLoader dataLoader;

    // KPI value labels
    private JLabel lblTxnVal, lblRevVal, lblLowVal;
    // KPI title labels — stored so onLanguageChanged() can update them
    private JLabel lblTxnTitle, lblRevTitle, lblLowKpiTitle;
    // Section / header labels
    private JLabel lblTitle, lblSubtitle;
    private JLabel lblLowStockTitle, lblTopTitle, lblPieCatTitle;
    // List panels
    private JPanel lowStockList, topProductsList;
    // Charts
    private PieChartPanel pieCats;
    // Empty-state labels — stored so onLanguageChanged() can retranslate them
    private JLabel emptyStockLbl, emptyTopLbl;
    // Toast
    private JLabel toastLabel;
    private Timer  toastTimer;

    public DashboardPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
    }

    public void setDataLoader(DataLoader loader) { this.dataLoader = loader; }

    private void build() {
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }

    // ── top bar ───────────────────────────────────────────────────────────────
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
        wrapper.add(bar, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(SEP);
        wrapper.add(sep, BorderLayout.SOUTH);
        return wrapper;
    }

    // ── center layout ─────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildKpiRow(), BorderLayout.NORTH);

        // right column: top products + low stock stacked
        JPanel rightStack = new JPanel(new GridLayout(2, 1, 0, 14));
        rightStack.setBackground(BG);
        rightStack.setPreferredSize(new Dimension(290, 100));
        rightStack.add(buildTopProductsCard());
        rightStack.add(buildLowStockCard());

        // pie chart fills remaining center
        pieCats        = new PieChartPanel();
        lblPieCatTitle = sectionTitle(I18n.t("dashboard.pie.category"));
        JPanel pieCard = buildPieCard(lblPieCatTitle, pieCats);

        JPanel mainGrid = new JPanel(new BorderLayout(14, 0));
        mainGrid.setBackground(BG);
        mainGrid.add(pieCard,    BorderLayout.CENTER);
        mainGrid.add(rightStack, BorderLayout.EAST);

        content.add(mainGrid, BorderLayout.CENTER);

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLabel.setForeground(GREEN);
        toastLabel.setVisible(false);
        toastLabel.setPreferredSize(new Dimension(100, 0));
        content.add(toastLabel, BorderLayout.SOUTH);

        return content;
    }

    // ── KPI row (3 cards) ─────────────────────────────────────────────────────
    private JPanel buildKpiRow() {
        lblTxnVal  = new JLabel("—");
        lblRevVal  = new JLabel("—");
        lblLowVal  = new JLabel("—");

        lblTxnTitle    = new JLabel(I18n.t("dashboard.kpi.transactions").toUpperCase());
        lblRevTitle    = new JLabel(I18n.t("dashboard.kpi.revenue").toUpperCase());
        lblLowKpiTitle = new JLabel(I18n.t("dashboard.kpi.low.stock").toUpperCase());

        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setBackground(BG);
        row.setPreferredSize(new Dimension(100, 112));

        row.add(kpiCard(lblTxnTitle,    lblTxnVal,  BLUE,    BLUE_BG));
        row.add(kpiCard(lblRevTitle,    lblRevVal,  ACCENT,  RED_BG));
        row.add(kpiCard(lblLowKpiTitle, lblLowVal,  RED_CLR, RED_BG));
        return row;
    }

    private JPanel kpiCard(JLabel titleLbl, JLabel valueLabel, Color accent, Color accentBg) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW);
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight() - 3, 6, 6);
                g2.fillRect(3, 0, 3, getHeight() - 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());

        JPanel topArea = new JPanel(new BorderLayout(8, 0));
        topArea.setOpaque(false);
        topArea.setBorder(new EmptyBorder(18, 20, 6, 18));

        titleLbl.setFont(new Font("Dialog", Font.PLAIN, 10));
        titleLbl.setForeground(MUTED);

        JLabel circle = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentBg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(30, 30));

        topArea.add(titleLbl, BorderLayout.CENTER);
        topArea.add(circle,   BorderLayout.EAST);

        valueLabel.setFont(new Font("Dialog", Font.BOLD, 26));
        valueLabel.setForeground(TXT);
        valueLabel.setBorder(new EmptyBorder(0, 20, 16, 18));

        card.add(topArea,    BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.SOUTH);
        return card;
    }

    // ── pie card ──────────────────────────────────────────────────────────────
    private JPanel buildPieCard(JLabel titleLabel, PieChartPanel chart) {
        JPanel card = shadowCard();
        card.setLayout(new BorderLayout());
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(chart,      BorderLayout.CENTER);
        return card;
    }

    // ── list cards ────────────────────────────────────────────────────────────
    private JPanel buildLowStockCard() {
        JPanel card = shadowCard();
        card.setLayout(new BorderLayout());

        lblLowStockTitle = sectionTitle(I18n.t("dashboard.low.stock.title"));
        card.add(lblLowStockTitle, BorderLayout.NORTH);

        lowStockList = new JPanel();
        lowStockList.setLayout(new BoxLayout(lowStockList, BoxLayout.Y_AXIS));
        lowStockList.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(lowStockList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTopProductsCard() {
        JPanel card = shadowCard();
        card.setLayout(new BorderLayout());

        lblTopTitle = sectionTitle(I18n.t("dashboard.top.products.title"));
        card.add(lblTopTitle, BorderLayout.NORTH);

        topProductsList = new JPanel();
        topProductsList.setLayout(new BoxLayout(topProductsList, BoxLayout.Y_AXIS));
        topProductsList.setBackground(CARD_BG);

        JScrollPane scroll = new JScrollPane(topProductsList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── pie chart (category revenue) ──────────────────────────────────────────
    private static class PieChartPanel extends JPanel {
        private final List<String> labels = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();

        PieChartPanel() {
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

            double total = values.stream().mapToDouble(d -> d).sum();
            if (values.isEmpty() || total == 0) {
                g.setFont(new Font("Dialog", Font.PLAIN, 13));
                g.setColor(MUTED);
                String msg = I18n.t("dashboard.empty");
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int n       = values.size();
            int rows    = Math.min(n, 5);
            int legendH = rows * 20 + 8;
            int pad     = 16;
            int pieDiam = Math.min(getWidth() - pad * 2, getHeight() - legendH - pad * 3);
            pieDiam = Math.max(pieDiam, 20);
            int px = (getWidth() - pieDiam) / 2;
            int py = pad;

            // shadow
            g2.setColor(SHADOW);
            g2.fillOval(px + 2, py + 3, pieDiam, pieDiam);

            // slices
            double angle = -90.0;
            for (int i = 0; i < n; i++) {
                double sweep = 360.0 * values.get(i) / total;
                g2.setColor(PIE_COLORS[i % PIE_COLORS.length]);
                g2.fill(new Arc2D.Double(px, py, pieDiam, pieDiam, angle, sweep, Arc2D.PIE));
                g2.setColor(CARD_BG);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Arc2D.Double(px, py, pieDiam, pieDiam, angle, sweep, Arc2D.PIE));
                angle += sweep;
            }

            // donut hole
            int hole = pieDiam / 3;
            g2.setColor(CARD_BG);
            g2.fillOval(px + (pieDiam - hole) / 2, py + (pieDiam - hole) / 2, hole, hole);

            // center total
            g2.setFont(new Font("Dialog", Font.BOLD, 12));
            g2.setColor(TXT);
            String totStr = formatCompact((long) total);
            FontMetrics tfm = g2.getFontMetrics();
            int cx = px + pieDiam / 2, cy = py + pieDiam / 2;
            g2.drawString(totStr, cx - tfm.stringWidth(totStr) / 2, cy + tfm.getAscent() / 2 - 1);

            // legend
            g2.setFont(new Font("Dialog", Font.PLAIN, 11));
            int lx = pad, ly = py + pieDiam + 14;
            for (int i = 0; i < Math.min(n, 5); i++) {
                g2.setColor(PIE_COLORS[i % PIE_COLORS.length]);
                g2.fillRoundRect(lx, ly + i * 20 + 3, 10, 10, 4, 4);
                g2.setColor(TXT);
                double pct = values.get(i) / total * 100;
                FontMetrics fm = g2.getFontMetrics();
                String lbl = labels.get(i);
                int maxLblW = getWidth() - lx - 16 - 40;
                while (fm.stringWidth(lbl) > maxLblW && lbl.length() > 3)
                    lbl = lbl.substring(0, lbl.length() - 1);
                g2.drawString(lbl + "  " + String.format("%.0f%%", pct), lx + 16, ly + i * 20 + 12);
            }

            g2.dispose();
        }

        private String formatCompact(long v) {
            if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
            if (v >= 1_000)     return String.format("%.0fK", v / 1_000.0);
            return String.valueOf(v);
        }
    }

    // ── data setter ───────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public void setDashboardData(Map<String, Object> data) {
        SwingUtilities.invokeLater(() -> {
            if (data == null) return;

            Object dailyObj = data.get("daily");
            if (dailyObj instanceof Map) {
                Map<String, Object> daily = (Map<String, Object>) dailyObj;
                double rev = toDouble(daily.get("totalRevenue"));
                lblTxnVal.setText(fmt(daily.get("totalTransactions")));
                lblRevVal.setText("₮" + fmtMoney(rev));
            }

            Object lowObj = data.get("lowStock");
            lowStockList.removeAll();
            emptyStockLbl = null;
            int lowCount = 0;
            if (lowObj instanceof List) {
                List<Map<String, Object>> low = (List<Map<String, Object>>) lowObj;
                lowCount = low.size();
                if (low.isEmpty()) {
                    emptyStockLbl = emptyLabel(I18n.t("dashboard.empty"));
                    lowStockList.add(emptyStockLbl);
                } else {
                    for (Map<String, Object> row : low) {
                        lowStockList.add(lowStockRow(row));
                        lowStockList.add(rowSep());
                    }
                }
            }
            lblLowVal.setText(String.valueOf(lowCount));

            Object topObj = data.get("topProducts");
            topProductsList.removeAll();
            emptyTopLbl = null;
            if (topObj instanceof List) {
                List<Map<String, Object>> top = (List<Map<String, Object>>) topObj;
                if (top.isEmpty()) {
                    emptyTopLbl = emptyLabel(I18n.t("dashboard.empty"));
                    topProductsList.add(emptyTopLbl);
                } else {
                    int rank = 1;
                    for (Map<String, Object> row : top) {
                        topProductsList.add(topProductRow(row, rank++));
                        topProductsList.add(rowSep());
                    }
                }
            }

            Object crObj = data.get("categoryRevenue");
            if (pieCats != null && crObj instanceof List) {
                List<Map<String, Object>> cr = (List<Map<String, Object>>) crObj;
                List<String> lbls = new ArrayList<>();
                List<Double> vals = new ArrayList<>();
                for (Map<String, Object> row : cr) {
                    lbls.add(String.valueOf(row.getOrDefault("categoryName", "Other")));
                    vals.add(toDouble(row.get("totalRevenue")));
                }
                pieCats.setData(lbls, vals);
            }

            lowStockList.revalidate();
            lowStockList.repaint();
            topProductsList.revalidate();
            topProductsList.repaint();
            revalidate();
            repaint();
        });
    }

    // ── row builders ──────────────────────────────────────────────────────────
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

        Color badgeBg = qty == 0 ? RED_BG  : AMBER_BG;
        Color badgeFg = qty == 0 ? RED_CLR : AMBER;
        JLabel badge  = badge(qty + " " + unit, badgeBg, badgeFg);

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
        rankLabel.setPreferredSize(new Dimension(28, 20));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        nameLabel.setForeground(TXT);

        left.add(rankLabel);
        left.add(nameLabel);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(CARD_BG);

        JLabel revLabel = new JLabel("₮" + fmtMoney(revenue));
        revLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        revLabel.setForeground(ACCENT);
        revLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel soldLabel = new JLabel(sold + " " + I18n.t("dashboard.sold"));
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
    private JPanel shadowCard() {
        JPanel c = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW);
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 14, 14);
                g2.dispose();
            }
        };
        c.setOpaque(false);
        return c;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 13));
        l.setForeground(TXT);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT),
            new EmptyBorder(13, 13, 11, 16)));
        return l;
    }

    private JPanel rowSep() {
        JPanel s = new JPanel();
        s.setBackground(new Color(0xF8FAFC));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        return s;
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
        b.setBorder(new EmptyBorder(9, 20, 9, 20));
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
        toastLabel.setPreferredSize(new Dimension(100, 20));
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(2500, e -> {
            toastLabel.setVisible(false);
            toastLabel.setPreferredSize(new Dimension(100, 0));
        });
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    @Override
    public void onLanguageChanged() {
        if (lblTitle         != null) lblTitle.setText(I18n.t("dashboard.title"));
        if (lblSubtitle      != null) lblSubtitle.setText(I18n.t("dashboard.subtitle"));
        if (lblTxnTitle      != null) lblTxnTitle.setText(I18n.t("dashboard.kpi.transactions").toUpperCase());
        if (lblRevTitle      != null) lblRevTitle.setText(I18n.t("dashboard.kpi.revenue").toUpperCase());
        if (lblLowKpiTitle   != null) lblLowKpiTitle.setText(I18n.t("dashboard.kpi.low.stock").toUpperCase());
        if (lblLowStockTitle != null) lblLowStockTitle.setText(I18n.t("dashboard.low.stock.title"));
        if (lblTopTitle      != null) lblTopTitle.setText(I18n.t("dashboard.top.products.title"));
        if (lblPieCatTitle   != null) lblPieCatTitle.setText(I18n.t("dashboard.pie.category"));
        if (emptyStockLbl    != null) emptyStockLbl.setText(I18n.t("dashboard.empty"));
        if (emptyTopLbl      != null) emptyTopLbl.setText(I18n.t("dashboard.empty"));
        if (pieCats          != null) pieCats.repaint();
    }
}
