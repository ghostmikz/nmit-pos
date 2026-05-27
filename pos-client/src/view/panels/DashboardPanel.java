package view.panels;

import static view.AppColors.*;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements LanguageListener {

    private static final Color CARD_BG = Color.WHITE;
    private static final Color SHADOW  = new Color(0, 0, 0, 18);
    private static final Color SEP_CLR = new Color(0xF8FAFC);

    @FunctionalInterface
    public interface DateRangeLoader { void load(String start, String end); }

    private final User user;
    private DateRangeLoader dataLoader;

    // KPI value labels
    private JLabel lblTxnVal, lblRevVal, lblAvgVal, lblLowVal;
    // KPI title labels
    private JLabel lblTxnTitle, lblRevTitle, lblAvgTitle, lblLowKpiTitle;
    // Section / header labels
    private JLabel lblTitle, lblLowStockTitle, lblTopTitle, lblRecentTitle;
    // Date filter
    private JFormattedTextField fromField, toField;
    private JButton applyBtn, refreshBtn;
    // List panels
    private JPanel lowStockList, topProductsList, recentSalesList;
    // Chart dataset (updated in-place to avoid chart rebuild on every refresh)
    private DefaultCategoryDataset chartDataset;
    // Empty-state labels
    private JLabel emptyStockLbl, emptyTopLbl, emptyRecentLbl;
    // Toast
    private JLabel toastLabel;
    private Timer  toastTimer;

    public DashboardPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        build();
    }

    public void setDataLoader(DateRangeLoader loader) { this.dataLoader = loader; }

    private void triggerLoad() {
        if (dataLoader == null) return;
        String from = fromField.getText().trim();
        String to   = toField.getText().trim();
        dataLoader.load(
            "0000-00-00".equals(from) || from.isBlank() ? null : from,
            "0000-00-00".equals(to)   || to.isBlank()   ? null : to);
    }

    private void build() {
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }

    // ── top bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        LocalDate today   = LocalDate.now();
        LocalDate defFrom = today.minusDays(29);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        fromField = dateField(); fromField.setText(defFrom.format(df));
        toField   = dateField(); toField.setText(today.format(df));

        applyBtn   = accentBtn(I18n.t("dashboard.apply"));
        refreshBtn = outlineBtn(I18n.t("dashboard.refresh"));
        applyBtn.addActionListener(e -> triggerLoad());
        refreshBtn.addActionListener(e -> triggerLoad());

        lblTitle = new JLabel(I18n.t("dashboard.title"));
        lblTitle.setFont(new Font("Dialog", Font.BOLD, 22));
        lblTitle.setForeground(TXT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(lblTitle);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.add(mLbl(I18n.t("dashboard.filter.from")));
        fromField.setPreferredSize(new Dimension(112, 30));
        filterRow.add(fromField);
        filterRow.add(mLbl(I18n.t("dashboard.filter.to")));
        toField.setPreferredSize(new Dimension(112, 30));
        filterRow.add(toField);
        filterRow.add(applyBtn);
        filterRow.add(refreshBtn);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CARD_BG);
        bar.setBorder(new EmptyBorder(16, 24, 14, 20));
        bar.add(left,      BorderLayout.WEST);
        bar.add(filterRow, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(CARD_BG);
        wrap.add(bar, BorderLayout.CENTER);
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        wrap.add(sep, BorderLayout.SOUTH);
        return wrap;
    }

    // ── center content ────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        content.add(buildKpiRow(), BorderLayout.NORTH);

        // mid: chart (center fills) + low stock (east, fixed width)
        JPanel midRow = new JPanel(new BorderLayout(14, 0));
        midRow.setBackground(BG);
        JPanel lowStockCard = buildLowStockCard();
        lowStockCard.setPreferredSize(new Dimension(270, 0));
        chartDataset = new DefaultCategoryDataset();
        midRow.add(buildChartCard(),  BorderLayout.CENTER);
        midRow.add(lowStockCard, BorderLayout.EAST);

        // bottom: top products + recent sales (equal halves)
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 14, 0));
        bottomRow.setBackground(BG);
        bottomRow.setPreferredSize(new Dimension(0, 250));
        bottomRow.add(buildTopProductsCard());
        bottomRow.add(buildRecentSalesCard());

        JPanel mainStack = new JPanel(new BorderLayout(0, 14));
        mainStack.setBackground(BG);
        mainStack.add(midRow,    BorderLayout.CENTER);
        mainStack.add(bottomRow, BorderLayout.SOUTH);

        content.add(mainStack, BorderLayout.CENTER);

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLabel.setForeground(GREEN);
        toastLabel.setVisible(false);
        toastLabel.setPreferredSize(new Dimension(100, 0));
        content.add(toastLabel, BorderLayout.SOUTH);

        return content;
    }

    // ── KPI row (4 cards) ─────────────────────────────────────────────────────
    private JPanel buildKpiRow() {
        lblTxnVal   = valLabel("—");
        lblRevVal   = valLabel("—");
        lblAvgVal   = valLabel("—");
        lblLowVal   = valLabel("—");

        lblTxnTitle    = titleLabel(I18n.t("dashboard.kpi.transactions").toUpperCase());
        lblRevTitle    = titleLabel(I18n.t("dashboard.kpi.revenue").toUpperCase());
        lblAvgTitle    = titleLabel(I18n.t("dashboard.kpi.avg.sale").toUpperCase());
        lblLowKpiTitle = titleLabel(I18n.t("dashboard.kpi.low.stock").toUpperCase());

        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setBackground(BG);
        row.setPreferredSize(new Dimension(100, 100));
        row.add(kpiCard(lblTxnTitle,    lblTxnVal,  BLUE));
        row.add(kpiCard(lblRevTitle,    lblRevVal,  ACCENT));
        row.add(kpiCard(lblAvgTitle,    lblAvgVal,  AMBER));
        row.add(kpiCard(lblLowKpiTitle, lblLowVal,  RED));
        return row;
    }

    private JPanel kpiCard(JLabel titleLbl, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW);
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight() - 3, 5, 5);
                g2.fillRect(3, 0, 2, getHeight() - 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(16, 18, 4, 14));
        top.add(titleLbl, BorderLayout.CENTER);

        valueLabel.setBorder(new EmptyBorder(0, 18, 14, 14));
        card.add(top,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.SOUTH);
        return card;
    }

    private JLabel valLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 24));
        l.setForeground(TXT);
        return l;
    }

    private JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.PLAIN, 10));
        l.setForeground(MUTED);
        return l;
    }

    // ── line chart ────────────────────────────────────────────────────────────
    private JPanel buildChartCard() {
        JPanel card = shadowCard();
        card.setLayout(new BorderLayout());
        card.add(sectionTitle(I18n.t("dashboard.chart.title")), BorderLayout.NORTH);
        card.add(buildLineChartPanel(), BorderLayout.CENTER);
        return card;
    }

    private ChartPanel buildLineChartPanel() {
        var chart = ChartFactory.createLineChart(
                null, null, null,
                chartDataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(CARD_BG);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(CARD_BG);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(BORDER);
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 10));
        plot.getDomainAxis().setTickLabelPaint(MUTED);
        plot.getDomainAxis().setAxisLineVisible(false);
        plot.getDomainAxis().setTickMarksVisible(false);
        plot.getRangeAxis().setTickLabelFont(new Font("Dialog", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelPaint(MUTED);
        plot.getRangeAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, ACCENT);
        renderer.setSeriesStroke(0, new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        plot.setRenderer(renderer);

        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(CARD_BG);
        cp.setOpaque(true);
        cp.setBorder(null);
        cp.setMouseWheelEnabled(false);
        cp.setDomainZoomable(false);
        cp.setRangeZoomable(false);
        return cp;
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
        JScrollPane scroll = listScroll(lowStockList);
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
        card.add(listScroll(topProductsList), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentSalesCard() {
        JPanel card = shadowCard();
        card.setLayout(new BorderLayout());
        lblRecentTitle = sectionTitle(I18n.t("dashboard.recent.sales.title"));
        card.add(lblRecentTitle, BorderLayout.NORTH);

        recentSalesList = new JPanel();
        recentSalesList.setLayout(new BoxLayout(recentSalesList, BoxLayout.Y_AXIS));
        recentSalesList.setBackground(CARD_BG);
        card.add(listScroll(recentSalesList), BorderLayout.CENTER);
        return card;
    }

    private static JScrollPane listScroll(JPanel list) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(8);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(CARD_BG);
        return scroll;
    }

    // ── data setter ───────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    public void setDashboardData(Map<String, Object> data) {
        SwingUtilities.invokeLater(() -> {
            if (data == null) return;

            // KPI summary
            Object sumObj = data.get("summary");
            if (sumObj instanceof Map) {
                Map<String, Object> sum = (Map<String, Object>) sumObj;
                lblTxnVal.setText(fmt(sum.get("totalTransactions")));
                lblRevVal.setText("₮" + fmtMoney(toDouble(sum.get("totalRevenue"))));
                lblAvgVal.setText("₮" + fmtMoney(toDouble(sum.get("avgSaleValue"))));
            }

            // Low stock
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

            // Top products
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

            // Daily trend chart
            chartDataset.clear();
            Object trendObj = data.get("dailyTrend");
            if (trendObj instanceof List) {
                List<Map<String, Object>> trend = (List<Map<String, Object>>) trendObj;
                for (Map<String, Object> row : trend) {
                    String date = shortLabel(String.valueOf(row.getOrDefault("saleDate", "")));
                    double rev  = toDouble(row.get("totalRevenue"));
                    chartDataset.addValue(rev, "Revenue", date);
                }
            }

            // Recent sales
            Object recentObj = data.get("recentSales");
            recentSalesList.removeAll();
            emptyRecentLbl = null;
            if (recentObj instanceof List) {
                List<Map<String, Object>> recent = (List<Map<String, Object>>) recentObj;
                if (recent.isEmpty()) {
                    emptyRecentLbl = emptyLabel(I18n.t("dashboard.empty"));
                    recentSalesList.add(emptyRecentLbl);
                } else {
                    for (Map<String, Object> row : recent) {
                        recentSalesList.add(recentSaleRow(row));
                        recentSalesList.add(rowSep());
                    }
                }
            }

            lowStockList.revalidate();    lowStockList.repaint();
            topProductsList.revalidate(); topProductsList.repaint();
            recentSalesList.revalidate(); recentSalesList.repaint();
            revalidate(); repaint();
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
        JLabel nl = new JLabel(name);
        nl.setFont(new Font("Dialog", Font.BOLD, 13));
        nl.setForeground(TXT);
        JLabel cl = new JLabel(cat);
        cl.setFont(new Font("Dialog", Font.PLAIN, 11));
        cl.setForeground(MUTED);
        left.add(nl);
        left.add(cl);

        Color badgeBg = qty == 0 ? RED_BG  : AMBER_BG;
        Color badgeFg = qty == 0 ? RED     : AMBER;
        p.add(left,                        BorderLayout.CENTER);
        p.add(badge(qty + " " + unit, badgeBg, badgeFg), BorderLayout.EAST);
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
        JLabel rankLbl = new JLabel("#" + rank);
        rankLbl.setFont(new Font("Dialog", Font.BOLD, 12));
        rankLbl.setForeground(rank <= 3 ? ACCENT : MUTED);
        rankLbl.setPreferredSize(new Dimension(28, 20));
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        nameLbl.setForeground(TXT);
        left.add(rankLbl);
        left.add(nameLbl);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(CARD_BG);
        JLabel revLbl = new JLabel("₮" + fmtMoney(revenue));
        revLbl.setFont(new Font("Dialog", Font.BOLD, 12));
        revLbl.setForeground(ACCENT);
        revLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel soldLbl = new JLabel(sold + " " + I18n.t("dashboard.sold"));
        soldLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        soldLbl.setForeground(MUTED);
        soldLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(revLbl);
        right.add(soldLbl);

        p.add(left,  BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel recentSaleRow(Map<String, Object> row) {
        String receipt = String.valueOf(row.getOrDefault("receiptNumber", "—"));
        String cashier = String.valueOf(row.getOrDefault("cashierName", "—"));
        double total   = toDouble(row.get("total"));
        boolean ref    = Boolean.TRUE.equals(row.get("isRefunded"));
        String dateStr = shortLabel(String.valueOf(row.getOrDefault("createdAt", "")));

        Color bg = CARD_BG;
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(10, 16, 10, 16));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color hover = new Color(0xFDF8F8);
            public void mouseEntered(java.awt.event.MouseEvent e) { p.setBackground(hover); p.repaint(); }
            public void mouseExited(java.awt.event.MouseEvent e)  { p.setBackground(bg);   p.repaint(); }
        });

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel recLbl = new JLabel(receipt);
        recLbl.setFont(new Font("Monospaced", Font.PLAIN, 11));
        recLbl.setForeground(TXT);
        JLabel cashLbl = new JLabel(cashier + "  •  " + dateStr);
        cashLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        cashLbl.setForeground(MUTED);
        left.add(recLbl);
        left.add(cashLbl);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        JLabel totLbl = new JLabel("₮" + fmtMoney(total));
        totLbl.setFont(new Font("Dialog", Font.BOLD, 13));
        totLbl.setForeground(ACCENT);
        totLbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel stsBadge = badge(
                ref ? I18n.t("report.status.refunded") : I18n.t("report.status.sold"),
                ref ? RED_BG : GREEN_BG,
                ref ? RED : GREEN);
        stsBadge.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(totLbl);
        right.add(Box.createVerticalStrut(2));
        right.add(stsBadge);

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
        s.setBackground(SEP_CLR);
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

    private JFormattedTextField dateField() {
        JFormattedTextField f;
        try {
            MaskFormatter m = new MaskFormatter("####-##-##");
            m.setPlaceholderCharacter('0');
            f = new JFormattedTextField(m);
        } catch (Exception ex) { f = new JFormattedTextField(); }
        f.setFont(new Font("Dialog", Font.PLAIN, 12));
        f.setForeground(TXT);
        f.setBackground(CARD_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            new EmptyBorder(3, 8, 3, 8)));
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
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        return b;
    }

    private static JLabel mLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setForeground(MUTED);
        return l;
    }

    private String fmt(Object val) {
        if (val == null) return "0";
        if (val instanceof Number) return String.valueOf(((Number) val).intValue());
        return String.valueOf(val);
    }

    private String fmtMoney(double d) {
        if (d >= 1_000_000) return String.format("%.1fM", d / 1_000_000);
        if (d >= 1_000)     return String.format("%.1fK", d / 1_000);
        return String.format("%.0f", d);
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }

    private String shortLabel(String s) {
        if (s == null || s.length() < 10) return "—";
        return s.substring(5, 10); // MM-DD
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
        if (lblTxnTitle      != null) lblTxnTitle.setText(I18n.t("dashboard.kpi.transactions").toUpperCase());
        if (lblRevTitle      != null) lblRevTitle.setText(I18n.t("dashboard.kpi.revenue").toUpperCase());
        if (lblAvgTitle      != null) lblAvgTitle.setText(I18n.t("dashboard.kpi.avg.sale").toUpperCase());
        if (lblLowKpiTitle   != null) lblLowKpiTitle.setText(I18n.t("dashboard.kpi.low.stock").toUpperCase());
        if (lblLowStockTitle != null) lblLowStockTitle.setText(I18n.t("dashboard.low.stock.title"));
        if (lblTopTitle      != null) lblTopTitle.setText(I18n.t("dashboard.top.products.title"));
        if (lblRecentTitle   != null) lblRecentTitle.setText(I18n.t("dashboard.recent.sales.title"));
        if (applyBtn         != null) applyBtn.setText(I18n.t("dashboard.apply"));
        if (refreshBtn       != null) refreshBtn.setText(I18n.t("dashboard.refresh"));
        if (emptyStockLbl    != null) emptyStockLbl.setText(I18n.t("dashboard.empty"));
        if (emptyTopLbl      != null) emptyTopLbl.setText(I18n.t("dashboard.empty"));
        if (emptyRecentLbl   != null) emptyRecentLbl.setText(I18n.t("dashboard.empty"));
    }
}
