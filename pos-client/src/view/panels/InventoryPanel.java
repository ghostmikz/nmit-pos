package view.panels;

import static view.AppColors.*;

import i18n.I18n;
import i18n.LanguageListener;
import model.Category;
import model.Product;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.function.Consumer;

public class InventoryPanel extends JPanel implements LanguageListener {

    // ── Functional interfaces ─────────────────────────────────────────────────
    @FunctionalInterface public interface ProductSaver {
        void save(Product p, byte[] imageBytes, boolean imageChanged, Runnable onSuccess);
    }
    @FunctionalInterface public interface ProductDeleter {
        void delete(int productId, Runnable onSuccess);
    }
    @FunctionalInterface public interface CategorySaver {
        void save(Category c, boolean isNew, Runnable onSuccess);
    }
    @FunctionalInterface public interface CategoryDeleter {
        void delete(int catId, Runnable onSuccess, Consumer<String> onError);
    }
    @FunctionalInterface public interface ImageLoader {
        void load(int productId, Consumer<String> callback);
    }

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color SEP         = new Color(0xF1F5F9);
    private static final Color ROW_HOVER_C = new Color(0xFDF9F9);  // inventory row hover (subtle red tint)
    private static final Color CAT_HOVER_C = new Color(0xFDF2F2);  // category selected bg
    private static final Color IMG_HINT_C  = new Color(0xA0AABA);  // image placeholder hint text (disabled-style)

    static final Color[] CAT_COLORS = {
        new Color(0x2C9B6A), new Color(0xD97706), new Color(0x2563EB),
        new Color(0xB92B2B), new Color(0x7C3AED), new Color(0xDB2777),
        new Color(0x0891B2), new Color(0x65A30D), new Color(0x475569),
        new Color(0xE11D48), new Color(0x059669), new Color(0x7C2D12)
    };
    static final String[] UNITS = {"ш", "кг", "г", "л", "мл", "боодол"};

    private static final NumberFormat MNT;
    static { MNT = NumberFormat.getNumberInstance(new Locale("mn", "MN")); }

    // ── Column layout constants ───────────────────────────────────────────────
    private static final int ROW_H    = 54;
    private static final int COL_CAT  = 130;
    private static final int COL_PRC  = 95;
    private static final int COL_STK  = 80;
    private static final int COL_STS  = 110;
    private static final int COL_ACT  = 120;
    private static final int ROW_PAD  = 18;
    private static final int PAGE_SIZE = 12;

    // ── State ─────────────────────────────────────────────────────────────────
    private final User user;
    private final List<Product>  allProducts   = new ArrayList<>();
    private final List<Category> allCategories = new ArrayList<>();
    private final Map<Integer, Image> imageCache = new HashMap<>();

    private ProductSaver    productSaver;
    private ProductDeleter  productDeleter;
    private CategorySaver   categorySaver;
    private CategoryDeleter categoryDeleter;
    private ImageLoader     imageLoader;

    private int    selectedCatId = -1;
    private String searchQuery   = "";
    private String stockFilter   = "all";
    private int    sortMode      = 0;
    private int    currentPage   = 1;

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel     topTitle;
    private JButton    addProdBtn;
    private JTextField catSearchField;
    private JPanel     catListPanel;
    private JTextField searchField;
    private String     searchPlaceholder = "";
    private JButton[]  filterChips;
    private JPanel     productListPanel;
    private JPanel     paginationPanel;
    private JLabel     toastLabel;
    private javax.swing.Timer toastTimer;

    // ── Construction ──────────────────────────────────────────────────────────
    public InventoryPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel leftCard = roundedCard(Color.WHITE, 12);
        leftCard.setLayout(new BorderLayout());
        leftCard.setPreferredSize(new Dimension(224, 0));
        leftCard.add(buildLeft(), BorderLayout.CENTER);

        JPanel rightCard = roundedCard(Color.WHITE, 12);
        rightCard.setLayout(new BorderLayout());
        rightCard.add(buildRight(), BorderLayout.CENTER);

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(leftCard,  BorderLayout.WEST);
        row.add(rightCard, BorderLayout.CENTER);
        add(row, BorderLayout.CENTER);
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

    // ── Public API ────────────────────────────────────────────────────────────
    public void setProducts(List<Product> products) {
        allProducts.clear();
        allProducts.addAll(products);
        refreshList();
    }

    public void setCategories(List<Category> categories) {
        allCategories.clear();
        allCategories.addAll(categories);
        rebuildCatList("");
        refreshList();
    }

    public void setProductSaver(ProductSaver h)       { productSaver    = h; }
    public void setProductDeleter(ProductDeleter h)   { productDeleter  = h; }
    public void setCategorySaver(CategorySaver h)     { categorySaver   = h; }
    public void setCategoryDeleter(CategoryDeleter h) { categoryDeleter = h; }
    public void setImageLoader(ImageLoader h)         { imageLoader     = h; }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
    }

    public void showToast(String msg) {
        toastLabel.setText(msg);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(2400, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    @Override public void onLanguageChanged() {
        searchPlaceholder = I18n.t("inventory.search");
        if (searchField != null && searchQuery.isEmpty()) {
            searchField.setText(searchPlaceholder);
            searchField.setForeground(MUTED);
        }
        topTitle.setText(I18n.t("inventory.title"));
        addProdBtn.setText(I18n.t("inventory.add"));
        if (filterChips != null) {
            filterChips[0].setText(I18n.t("inventory.filter.all"));
            filterChips[1].setText(I18n.t("inventory.filter.low"));
            filterChips[2].setText(I18n.t("inventory.filter.out"));
        }
        rebuildCatList(catSearchField.getText());
        refreshList();
    }

    // ── Left: category panel ──────────────────────────────────────────────────
    private JPanel buildLeft() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 16, 10, 12));
        JLabel title = new JLabel(I18n.t("inventory.cat.header"));
        title.setFont(new Font("Dialog", Font.BOLD, 11));
        title.setForeground(MUTED);
        JButton addBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BORDER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setFont(getFont());
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String s = "+";
                g2.drawString(s, (getWidth()-fm.stringWidth(s))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        addBtn.setFont(new Font("Dialog", Font.BOLD, 16));
        addBtn.setPreferredSize(new Dimension(26, 26));
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setContentAreaFilled(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.addActionListener(e -> openCategoryDialog(null));
        header.add(title,  BorderLayout.WEST);
        header.add(addBtn, BorderLayout.EAST);

        // Cat search
        JPanel searchWrap = new JPanel(new BorderLayout(6, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(6, 10, 6, 10));
        catSearchField = new JTextField();
        catSearchField.setFont(new Font("Dialog", Font.PLAIN, 12));
        catSearchField.setOpaque(false);
        catSearchField.setBorder(null);
        catSearchField.getDocument().addDocumentListener(dl(() -> rebuildCatList(catSearchField.getText())));
        JLabel si = new JLabel(view.MainFrame.assetSq("/assets/icons/search.png", 12));
        searchWrap.add(si, BorderLayout.WEST);
        searchWrap.add(catSearchField, BorderLayout.CENTER);
        JPanel searchPad = new JPanel(new BorderLayout());
        searchPad.setOpaque(false);
        searchPad.setBorder(new EmptyBorder(0, 10, 8, 10));
        searchPad.add(searchWrap);

        // Cat list
        catListPanel = new JPanel();
        catListPanel.setLayout(new BoxLayout(catListPanel, BoxLayout.Y_AXIS));
        catListPanel.setBackground(Color.WHITE);
        catListPanel.setBorder(new EmptyBorder(4, 8, 4, 8));
        JScrollPane catScroll = new JScrollPane(catListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        catScroll.setBorder(null);
        catScroll.getViewport().setBackground(Color.WHITE);
        view.MainFrame.modernScrollBar(catScroll);
        catScroll.getVerticalScrollBar().setUnitIncrement(16);

        // New category footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
            new EmptyBorder(10, 10, 12, 10)));
        JButton newCat = pill(I18n.t("inventory.cat.new"), -999, false);
        newCat.addActionListener(e -> openCategoryDialog(null));
        footer.add(newCat);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header,    BorderLayout.NORTH);
        top.add(searchPad, BorderLayout.CENTER);
        panel.add(top,      BorderLayout.NORTH);
        panel.add(catScroll,BorderLayout.CENTER);
        panel.add(footer,   BorderLayout.SOUTH);
        return panel;
    }

    private void rebuildCatList(String filter) {
        catListPanel.removeAll();
        String f = filter == null ? "" : filter.toLowerCase();
        catListPanel.add(catItem(-1, I18n.t("inventory.cat.all"), countFor(-1), null));
        for (Category c : allCategories) {
            if (!f.isEmpty() && !c.getName().toLowerCase().contains(f)) continue;
            catListPanel.add(catItem(c.getId(), c.getName(), countFor(c.getId()), c));
        }
        catListPanel.revalidate();
        catListPanel.repaint();
    }

    private int countFor(int catId) {
        if (catId == -1) return (int) allProducts.stream().filter(Product::isActive).count();
        return (int) allProducts.stream().filter(p -> p.isActive() && p.getCategoryId() == catId).count();
    }

    private JPanel catItem(int catId, String name, int count, Category cat) {
        boolean active = catId == selectedCatId;
        Color dot = cat != null ? catColor(cat.getId()) : SCROLL_THUMB_HV;

        JPanel item = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                if (active) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(CAT_HOVER_C);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return !active; }
        };
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(7, 8, 7, 8));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        item.setAlignmentX(LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel dotWrap = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(dot);
                g2.fillOval(0, (getHeight()-8)/2, 8, 8);
                g2.dispose();
            }
        };
        dotWrap.setOpaque(false);
        dotWrap.setPreferredSize(new Dimension(10, 8));

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        nameLbl.setForeground(active ? ACCENT : TXT);

        JLabel cntLbl = new JLabel(String.valueOf(count));
        cntLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        cntLbl.setForeground(active ? ACCENT : MUTED);

        item.add(dotWrap, BorderLayout.WEST);
        item.add(nameLbl, BorderLayout.CENTER);
        item.add(cntLbl,  BorderLayout.EAST);

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && cat != null) showCatMenu(cat, item, e.getX(), e.getY());
                else { selectedCatId = catId; currentPage = 1; rebuildCatList(catSearchField.getText()); refreshList(); }
            }
        };
        item.addMouseListener(ma);
        nameLbl.addMouseListener(ma);
        return item;
    }

    private void showCatMenu(Category cat, Component src, int x, int y) {
        JPopupMenu m = new JPopupMenu();
        JMenuItem edit = new JMenuItem(I18n.t("inventory.ctx.edit"));
        edit.addActionListener(e -> openCategoryDialog(cat));
        JMenuItem del = new JMenuItem(I18n.t("inventory.ctx.delete"));
        del.setForeground(RED);
        del.addActionListener(e -> confirmDeleteCat(cat));
        m.add(edit); m.addSeparator(); m.add(del);
        m.show(src, x, y);
    }

    // ── Right: product area ───────────────────────────────────────────────────
    private JPanel buildRight() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(Color.WHITE);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(new EmptyBorder(14, 20, 10, 20));
        topTitle = new JLabel(I18n.t("inventory.title"));
        topTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        topTitle.setForeground(TXT);
        addProdBtn = new JButton(I18n.t("inventory.add")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        addProdBtn.setFont(new Font("Dialog", Font.BOLD, 13));
        addProdBtn.setBackground(ACCENT);
        addProdBtn.setForeground(Color.WHITE);
        addProdBtn.setBorderPainted(false);
        addProdBtn.setFocusPainted(false);
        addProdBtn.setBorder(new EmptyBorder(9, 18, 9, 18));
        addProdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addProdBtn.addActionListener(e -> openProductDialog(null));
        topBar.add(topTitle,  BorderLayout.WEST);
        topBar.add(addProdBtn,BorderLayout.EAST);

        // Filter bar
        JPanel filterBar = new JPanel(new BorderLayout(12, 0));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)),
            new EmptyBorder(6, 20, 10, 20)));

        // Search
        JPanel searchWrap = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(8, 12, 8, 12));
        searchWrap.setPreferredSize(new Dimension(280, 40));
        searchWrap.setMinimumSize(new Dimension(280, 40));
        searchWrap.setMaximumSize(new Dimension(280, 40));
        searchPlaceholder = I18n.t("inventory.search");
        searchField = new JTextField(searchPlaceholder);
        searchField.setForeground(MUTED);
        searchField.setFont(new Font("Dialog", Font.PLAIN, 14));
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchPlaceholder.equals(searchField.getText())) { searchField.setText(""); searchField.setForeground(TXT); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) { searchField.setText(searchPlaceholder); searchField.setForeground(MUTED); }
            }
        });
        searchField.getDocument().addDocumentListener(dl(() -> {
            String t = searchField.getText();
            searchQuery = searchPlaceholder.equals(t) ? "" : t.trim().toLowerCase();
            currentPage = 1;
            refreshList();
        }));
        JLabel searchIcon = new JLabel(view.MainFrame.assetSq("/assets/icons/search.png", 16));
        searchWrap.add(searchIcon,  BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        // Filter chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        filterChips = new JButton[3];
        String[] chipKeys = {"inventory.filter.all","inventory.filter.low","inventory.filter.out"};
        String[] chipVals = {"all","low","out"};
        for (int i = 0; i < 3; i++) {
            int idx = i;
            filterChips[i] = pill(I18n.t(chipKeys[i]), -1, i == 0);
            filterChips[i].addActionListener(e -> {
                stockFilter = chipVals[idx];
                currentPage = 1;
                for (JButton c : filterChips) { c.setBackground(BORDER); c.setForeground(new Color(0x475569)); }
                filterChips[idx].setBackground(ACCENT);
                filterChips[idx].setForeground(Color.WHITE);
                refreshList();
            });
            chips.add(filterChips[i]);
        }

        filterBar.add(searchWrap, BorderLayout.WEST);
        filterBar.add(chips,      BorderLayout.CENTER);

        // Column header
        JPanel colHeader = buildColHeader();

        // Product list
        productListPanel = new JPanel();
        productListPanel.setLayout(new BoxLayout(productListPanel, BoxLayout.Y_AXIS));
        productListPanel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(productListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        view.MainFrame.modernScrollBar(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.add(colHeader, BorderLayout.NORTH);
        tableCard.add(scroll,    BorderLayout.CENTER);

        // Pagination — white bar with top separator
        paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        paginationPanel.setOpaque(true);
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
            new EmptyBorder(10, 20, 12, 20)));

        // Toast — compact floating bar at bottom
        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        toastLabel.setForeground(Color.WHITE);
        toastLabel.setOpaque(true);
        toastLabel.setBackground(new Color(0x1E293B));
        toastLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        toastLabel.setVisible(false);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Color.WHITE);
        center.add(tableCard,      BorderLayout.CENTER);
        center.add(paginationPanel,BorderLayout.SOUTH);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(Color.WHITE);
        mainContent.add(filterBar, BorderLayout.NORTH);
        mainContent.add(center,    BorderLayout.CENTER);
        mainContent.add(toastLabel,BorderLayout.SOUTH);

        area.add(topBar,      BorderLayout.NORTH);
        area.add(mainContent, BorderLayout.CENTER);
        return area;
    }

    private JPanel buildColHeader() {
        JPanel header = new JPanel(null) {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 36); }
            @Override public void doLayout() { layoutRowComponents(this); }
        };
        header.setBackground(new Color(0xF8FAFC));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)));

        header.add(colHdr(I18n.t("inventory.col.product")));
        header.add(colHdr(I18n.t("inventory.col.category")));
        header.add(colHdr(I18n.t("inventory.col.price")));
        header.add(colHdr(I18n.t("inventory.col.stock")));
        header.add(colHdr(I18n.t("inventory.col.status")));
        header.add(new JLabel()); // actions col empty header
        return header;
    }

    private JLabel colHdr(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setForeground(MUTED);
        l.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        return l;
    }

    // ── Product list (pagination + rows) ─────────────────────────────────────
    private void refreshList() {
        List<Product> filtered = getFiltered();
        int total = filtered.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        if (currentPage > pages) currentPage = pages;
        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        List<Product> page = filtered.subList(from, to);

        productListPanel.removeAll();
        if (page.isEmpty()) {
            JLabel empty = new JLabel(I18n.t("inventory.empty"), SwingConstants.CENTER);
            empty.setFont(new Font("Dialog", Font.PLAIN, 14));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(48, 0, 48, 0));
            productListPanel.add(empty);
        } else {
            for (int i = 0; i < page.size(); i++) {
                productListPanel.add(productRow(page.get(i)));
                if (i < page.size() - 1) {
                    JPanel sep = new JPanel();
                    sep.setBackground(SEP);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep.setAlignmentX(LEFT_ALIGNMENT);
                    productListPanel.add(sep);
                }
            }
        }
        productListPanel.revalidate();
        productListPanel.repaint();
        rebuildPagination(total, pages);
    }

    private List<Product> getFiltered() {
        List<Product> list = new ArrayList<>();
        for (Product p : allProducts) {
            if (!p.isActive()) continue;
            if (selectedCatId != -1 && p.getCategoryId() != selectedCatId) continue;
            if (!searchQuery.isEmpty() &&
                !p.getName().toLowerCase().contains(searchQuery) &&
                (p.getBarcode() == null || !p.getBarcode().toLowerCase().contains(searchQuery))) continue;
            if ("low".equals(stockFilter) && !(p.getStockQuantity() > 0 && p.getStockQuantity() <= p.getLowStockAlert())) continue;
            if ("out".equals(stockFilter) && p.getStockQuantity() != 0) continue;
            list.add(p);
        }
        list.sort(switch (sortMode) {
            case 1 -> Comparator.comparing(Product::getPrice);
            case 2 -> Comparator.comparingInt(Product::getStockQuantity);
            case 3 -> Comparator.comparingInt(Product::getCategoryId);
            default -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        });
        return list;
    }

    // ── Product row ───────────────────────────────────────────────────────────
    private JPanel productRow(Product p) {
        JPanel row = new JPanel(null) {
            @Override public void doLayout() { layoutRowComponents(this); }
            @Override public Dimension getPreferredSize() { return new Dimension(0, ROW_H); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, ROW_H); }
        };
        row.setBackground(Color.WHITE);
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Name + barcode
        JPanel namePanel = new JPanel(new BorderLayout(0, 2));
        namePanel.setOpaque(false);
        namePanel.setBorder(new EmptyBorder(0, ROW_PAD, 0, 8));
        JLabel nameLbl = new JLabel(p.getName());
        nameLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        nameLbl.setForeground(TXT);
        JLabel skuLbl  = new JLabel(p.getBarcode() != null && !p.getBarcode().isEmpty() ? p.getBarcode() : "—");
        skuLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        skuLbl.setForeground(MUTED);
        namePanel.add(nameLbl, BorderLayout.CENTER);
        namePanel.add(skuLbl,  BorderLayout.SOUTH);

        // Category tag
        JPanel catWrap = new JPanel(new GridBagLayout());
        catWrap.setOpaque(false);
        catWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        if (p.getCategoryName() != null) {
            Color c = catColor(p.getCategoryId());
            JLabel tag = new JLabel(p.getCategoryName()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 22));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.dispose();
                    super.paintComponent(g);
                }
                @Override public boolean isOpaque() { return false; }
            };
            tag.setForeground(c);
            tag.setFont(new Font("Dialog", Font.PLAIN, 11));
            tag.setBorder(new EmptyBorder(3, 9, 3, 9));
            GridBagConstraints cc = new GridBagConstraints();
            cc.anchor = GridBagConstraints.WEST;
            catWrap.add(tag, cc);
        }

        // Price
        JPanel priceWrap = new JPanel(new GridBagLayout());
        priceWrap.setOpaque(false);
        priceWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        JLabel priceLbl = new JLabel("₮" + MNT.format(p.getPrice()));
        priceLbl.setFont(new Font("Dialog", Font.BOLD, 13));
        priceLbl.setForeground(TXT);
        { GridBagConstraints gbc = new GridBagConstraints(); gbc.anchor = GridBagConstraints.WEST; priceWrap.add(priceLbl, gbc); }

        // Stock
        JPanel stockWrap = new JPanel(new GridBagLayout());
        stockWrap.setOpaque(false);
        stockWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        JLabel stockLbl = new JLabel(p.getStockQuantity() + " " + (p.getUnit() != null ? p.getUnit() : "ш"));
        stockLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        stockLbl.setForeground(TXT);
        { GridBagConstraints gbc = new GridBagConstraints(); gbc.anchor = GridBagConstraints.WEST; stockWrap.add(stockLbl, gbc); }

        // Status badge
        JPanel statusWrap = new JPanel(new GridBagLayout());
        statusWrap.setOpaque(false);
        statusWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        String stTxt; Color stFg, stBg;
        if (p.getStockQuantity() == 0)                                         { stTxt = I18n.t("inventory.status.out"); stFg = RED;  stBg = RED_BG;   }
        else if (p.getStockQuantity() <= p.getLowStockAlert())                  { stTxt = I18n.t("inventory.status.low"); stFg = AMBER;    stBg = AMBER_BG; }
        else                                                                    { stTxt = I18n.t("inventory.status.in");  stFg = GREEN;    stBg = GREEN_BG; }
        final Color _fg = stFg, _bg = stBg;
        JLabel badge = new JLabel(stTxt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(_bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        badge.setForeground(_fg);
        badge.setFont(new Font("Dialog", Font.BOLD, 11));
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        statusWrap.add(badge);

        // Action buttons
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        JButton editBtn = tinyBtn(I18n.t("inventory.ctx.edit"), ACCENT);
        JButton delBtn  = tinyBtn(I18n.t("inventory.ctx.delete"), RED);
        editBtn.addActionListener(e -> openProductDialog(p));
        delBtn.addActionListener(e  -> confirmDeleteProduct(p));
        { GridBagConstraints ac = new GridBagConstraints(); ac.insets = new Insets(0, 2, 0, 2); ac.gridx = 0; actions.add(editBtn, ac); ac.gridx = 1; actions.add(delBtn, ac); }

        row.add(namePanel);
        row.add(catWrap);
        row.add(priceWrap);
        row.add(stockWrap);
        row.add(statusWrap);
        row.add(actions);

        // Hover + right-click
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(ROW_HOVER_C); row.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE);         row.repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) showProductMenu(p, row, e.getX(), e.getY());
            }
        };
        for (Component c : row.getComponents()) c.addMouseListener(ma);
        row.addMouseListener(ma);
        return row;
    }

    // shared doLayout for header and data rows
    private void layoutRowComponents(JPanel panel) {
        Component[] cs = panel.getComponents();
        if (cs.length < 6) return;
        int h = panel.getHeight(), x = 0;
        int fixed = COL_CAT + COL_PRC + COL_STK + COL_STS + COL_ACT + ROW_PAD;
        int nameW = Math.max(80, panel.getWidth() - fixed);
        cs[0].setBounds(x, 0, nameW,   h); x += nameW;
        cs[1].setBounds(x, 0, COL_CAT, h); x += COL_CAT;
        cs[2].setBounds(x, 0, COL_PRC, h); x += COL_PRC;
        cs[3].setBounds(x, 0, COL_STK, h); x += COL_STK;
        cs[4].setBounds(x, 0, COL_STS, h); x += COL_STS;
        cs[5].setBounds(x, 0, COL_ACT, h);
    }

    private void showProductMenu(Product p, Component src, int x, int y) {
        JPopupMenu m = new JPopupMenu();
        JMenuItem edit = new JMenuItem(I18n.t("inventory.ctx.edit"));
        edit.addActionListener(e -> openProductDialog(p));
        JMenuItem dup  = new JMenuItem(I18n.t("inventory.ctx.duplicate"));
        dup.addActionListener(e -> duplicateProduct(p));
        JMenuItem del  = new JMenuItem(I18n.t("inventory.ctx.delete"));
        del.setForeground(RED);
        del.addActionListener(e -> confirmDeleteProduct(p));
        m.add(edit); m.add(dup); m.addSeparator(); m.add(del);
        m.show(src, x, y);
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    private void rebuildPagination(int total, int pages) {
        paginationPanel.removeAll();
        if (pages <= 1) { paginationPanel.revalidate(); paginationPanel.repaint(); return; }
        JLabel info = new JLabel(total + " items");
        info.setFont(new Font("Dialog", Font.PLAIN, 12));
        info.setForeground(MUTED);
        paginationPanel.add(info);
        paginationPanel.add(Box.createHorizontalStrut(10));
        paginationPanel.add(pgBtn("‹", currentPage > 1, currentPage - 1));
        for (int i = 1; i <= pages; i++) {
            paginationPanel.add(pgBtn(String.valueOf(i), true, i));
        }
        paginationPanel.add(pgBtn("›", currentPage < pages, currentPage + 1));
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    private JButton pgBtn(String label, boolean enabled, int target) {
        boolean active = label.equals(String.valueOf(currentPage));
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? ACCENT : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        b.setForeground(active ? Color.WHITE : (enabled ? TXT : MUTED));
        b.setPreferredSize(new Dimension(30, 30));
        b.setBorder(BorderFactory.createLineBorder(active ? ACCENT : new Color(0xE2E8F0)));
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
        b.setEnabled(enabled);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> { currentPage = target; refreshList(); });
        return b;
    }

    // ── Product dialog ────────────────────────────────────────────────────────
    private void openProductDialog(Product edit) {
        boolean isNew = edit == null;
        JDialog dlg = makeDialog(isNew ? I18n.t("inventory.form.add") : I18n.t("inventory.form.edit"));

        ImageUploadArea imgArea = new ImageUploadArea();
        if (!isNew && edit.isHasImage()) {
            if (imageCache.containsKey(edit.getId())) {
                imgArea.setImage(imageCache.get(edit.getId()));
            } else if (imageLoader != null) {
                imageLoader.load(edit.getId(), b64 -> {
                    if (b64 != null) {
                        try {
                            byte[] bytes = Base64.getDecoder().decode(b64);
                            Image img = ImageIO.read(new ByteArrayInputStream(bytes));
                            if (img != null) { imageCache.put(edit.getId(), img); imgArea.setImage(img); }
                        } catch (Exception ignored) {}
                    }
                });
            }
        }

        JTextField fName    = styledField(isNew ? "" : edit.getName());
        JTextField fBarcode = styledField(isNew ? "" : val(edit.getBarcode()));
        JTextField fPrice   = styledField(isNew ? "" : edit.getPrice().toPlainString());
        JTextField fCost    = styledField(isNew || edit.getCostPrice() == null ? "" : edit.getCostPrice().toPlainString());
        JTextField fQty     = new JTextField(isNew ? "0" : String.valueOf(edit.getStockQuantity()));
        fQty.setFont(new Font("Dialog", Font.PLAIN, 13));
        fQty.setOpaque(false);
        fQty.setBorder(new EmptyBorder(0, 4, 0, 4));
        fQty.setHorizontalAlignment(JTextField.CENTER);
        JPanel     fQtyStepper = makeStepper(fQty);
        JTextField fAlert   = styledField(isNew ? "10" : String.valueOf(edit.getLowStockAlert()));
        JTextField fExpiry  = styledField(isNew ? "" : val(edit.getExpiryDate()));

        JComboBox<String> fCat  = new JComboBox<>();
        fCat.setFont(new Font("Dialog", Font.PLAIN, 13));
        fCat.addItem(I18n.t("inventory.form.none.category"));
        int selCatIdx = 0;
        for (int i = 0; i < allCategories.size(); i++) {
            Category c = allCategories.get(i);
            fCat.addItem(c.getName());
            if (!isNew && c.getId() == edit.getCategoryId()) selCatIdx = i + 1;
        }
        fCat.setSelectedIndex(selCatIdx);

        JComboBox<String> fUnit = new JComboBox<>(UNITS);
        fUnit.setFont(new Font("Dialog", Font.PLAIN, 13));
        if (!isNew && edit.getUnit() != null)
            for (int i = 0; i < UNITS.length; i++) if (UNITS[i].equals(edit.getUnit())) { fUnit.setSelectedIndex(i); break; }

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(16, 24, 16, 24));
        body.add(imgArea);
        body.add(vs(12));
        body.add(formField(I18n.t("inventory.form.name"),    fName));
        body.add(vs(10));
        body.add(formRow(
            I18n.t("inventory.form.barcode"), fBarcode,
            I18n.t("inventory.form.category"), fCat));
        body.add(vs(10));
        body.add(formRow(
            I18n.t("inventory.form.unit"), fUnit,
            I18n.t("inventory.form.price"), fPrice));
        body.add(vs(10));
        body.add(formRow(
            I18n.t("inventory.form.cost"), fCost,
            I18n.t("inventory.form.qty"), fQtyStepper));
        body.add(vs(10));
        body.add(formRow(
            I18n.t("inventory.form.alert"), fAlert,
            I18n.t("inventory.form.expiry"), fExpiry));

        JScrollPane bodyScroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setBorder(null);

        JButton cancelBtn = ghostBtn(I18n.t("common.cancel"));
        cancelBtn.addActionListener(e -> dlg.dispose());
        JButton saveBtn = navBtn(I18n.t("common.save"), ACCENT, Color.WHITE);
        saveBtn.addActionListener(e -> {
            String name = fName.getText().trim();
            if (name.isEmpty()) { fName.requestFocus(); return; }
            BigDecimal price;
            try { price = new BigDecimal(fPrice.getText().trim()); } catch (Exception ex) { fPrice.requestFocus(); return; }
            Product p = new Product();
            if (!isNew) p.setId(edit.getId());
            p.setName(name);
            p.setBarcode(fBarcode.getText().trim());
            int ci = fCat.getSelectedIndex();
            p.setCategoryId(ci > 0 ? allCategories.get(ci - 1).getId() : 0);
            p.setUnit((String) fUnit.getSelectedItem());
            p.setPrice(price);
            try { p.setCostPrice(new BigDecimal(fCost.getText().trim())); } catch (Exception ignored) {}
            try { p.setStockQuantity(Integer.parseInt(fQty.getText().trim())); } catch (Exception ignored) {}
            try { p.setLowStockAlert(Math.max(1, Integer.parseInt(fAlert.getText().trim()))); } catch (Exception ignored) { p.setLowStockAlert(10); }
            String exp = fExpiry.getText().trim(); p.setExpiryDate(exp.isEmpty() ? null : exp);
            if (productSaver != null) {
                dlg.dispose();
                productSaver.save(p, imgArea.selectedBytes, imgArea.changed,
                    () -> showToast(isNew ? I18n.t("inventory.toast.prod.added") : I18n.t("inventory.toast.prod.updated")));
            }
        });

        JPanel footer = dialogFooter(cancelBtn, saveBtn);
        dlg.add(bodyScroll, BorderLayout.CENTER);
        dlg.add(footer,     BorderLayout.SOUTH);
        dlg.setPreferredSize(new Dimension(500, 630));
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ── Category dialog ───────────────────────────────────────────────────────
    private void openCategoryDialog(Category edit) {
        boolean isNew = edit == null;
        JDialog dlg   = makeDialog(isNew ? I18n.t("inventory.cat.add.title") : I18n.t("inventory.cat.edit.title"));
        JTextField fName = styledField(isNew ? "" : edit.getName());

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 16, 24));
        body.add(formField(I18n.t("inventory.cat.name"), fName));

        JButton cancel = ghostBtn(I18n.t("common.cancel"));
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = navBtn(I18n.t("common.save"), ACCENT, Color.WHITE);
        save.addActionListener(e -> {
            String name = fName.getText().trim();
            if (name.isEmpty()) { fName.requestFocus(); return; }
            Category c = new Category(); if (!isNew) c.setId(edit.getId()); c.setName(name);
            if (categorySaver != null) {
                dlg.dispose();
                categorySaver.save(c, isNew,
                    () -> showToast(isNew ? I18n.t("inventory.toast.cat.added") : I18n.t("inventory.toast.cat.updated")));
            }
        });

        dlg.add(body,               BorderLayout.CENTER);
        dlg.add(dialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setPreferredSize(new Dimension(360, 200));
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void confirmDeleteProduct(Product p) {
        int ok = JOptionPane.showConfirmDialog(this,
            MessageFormat.format(I18n.t("inventory.confirm.delete.prod"), p.getName()),
            I18n.t("inventory.ctx.delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION && productDeleter != null)
            productDeleter.delete(p.getId(), () -> showToast(I18n.t("inventory.toast.prod.deleted")));
    }

    private void confirmDeleteCat(Category c) {
        int ok = JOptionPane.showConfirmDialog(this,
            MessageFormat.format(I18n.t("inventory.confirm.delete.cat"), c.getName()),
            I18n.t("inventory.ctx.delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION && categoryDeleter != null)
            categoryDeleter.delete(c.getId(),
                () -> showToast(I18n.t("inventory.toast.cat.deleted")),
                this::showError);
    }

    private void duplicateProduct(Product src) {
        if (productSaver == null) return;
        Product dup = new Product();
        dup.setName(src.getName() + " (copy)");
        dup.setBarcode(""); dup.setCategoryId(src.getCategoryId()); dup.setUnit(src.getUnit());
        dup.setPrice(src.getPrice()); dup.setCostPrice(src.getCostPrice());
        dup.setStockQuantity(0); dup.setLowStockAlert(src.getLowStockAlert());
        productSaver.save(dup, null, false, () -> showToast(I18n.t("inventory.toast.prod.added")));
    }

    // ── Image upload area ─────────────────────────────────────────────────────
    private static class ImageUploadArea extends JPanel {
        private Image displayImage;
        byte[]  selectedBytes;
        boolean changed = false;

        ImageUploadArea() {
            setPreferredSize(new Dimension(0, 120));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            setAlignmentX(LEFT_ALIGNMENT);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileNameExtensionFilter("Image files", "jpg","jpeg","png","gif","bmp","webp"));
                    if (fc.showOpenDialog(ImageUploadArea.this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            File f = fc.getSelectedFile();
                            selectedBytes = Files.readAllBytes(f.toPath());
                            displayImage  = ImageIO.read(f);
                            changed = true; repaint();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

        void setImage(Image img) { SwingUtilities.invokeLater(() -> { displayImage = img; repaint(); }); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setColor(CHIP_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            if (displayImage != null) {
                int iw = displayImage.getWidth(null), ih = displayImage.getHeight(null);
                double s = Math.max((double)getWidth()/iw, (double)getHeight()/ih);
                int dw = (int)(iw*s), dh = (int)(ih*s);
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.drawImage(displayImage, (getWidth()-dw)/2, (getHeight()-dh)/2, dw, dh, null);
            } else {
                ImageIcon cam = view.MainFrame.assetSq("/assets/icons/camera.png", 28);
                if (cam != null) g2.drawImage(cam.getImage(), (getWidth()-28)/2, (getHeight()-28)/2-10, 28, 28, null);
                g2.setFont(new Font("Dialog", Font.PLAIN, 12));
                g2.setColor(SCROLL_THUMB_HV);
                FontMetrics fm = g2.getFontMetrics();
                String hint = "Click to upload image";
                g2.drawString(hint, (getWidth()-fm.stringWidth(hint))/2, getHeight()/2+18);
            }
            g2.setColor(SCROLL_THUMB);
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4}, 0));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
            g2.dispose();
        }
    }

    // ── Dialog helpers ────────────────────────────────────────────────────────
    private JDialog makeDialog(String title) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);
        dlg.setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());
        dlg.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(0xCDD5E0)));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR);
        header.setPreferredSize(new Dimension(0, 40));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Dialog", Font.BOLD, 14));
        t.setForeground(Color.WHITE);
        t.setBorder(new EmptyBorder(0, 16, 0, 0));

        JButton x = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = 5;
                g2.setColor(getModel().isRollover() ? Color.WHITE : new Color(255, 255, 255, 160));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - r, cy - r, cx + r, cy + r);
                g2.drawLine(cx + r, cy - r, cx - r, cy + r);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        x.setContentAreaFilled(false);
        x.setBorderPainted(false);
        x.setFocusPainted(false);
        x.setRolloverEnabled(true);
        x.setPreferredSize(new Dimension(32, 32));
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.getModel().addChangeListener(e -> x.repaint());
        x.addActionListener(e -> dlg.dispose());

        final int[] drag = {0, 0};
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { drag[0] = e.getX(); drag[1] = e.getY(); }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point p = dlg.getLocation();
                dlg.setLocation(p.x + e.getX() - drag[0], p.y + e.getY() - drag[1]);
            }
        });

        JPanel xWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        xWrap.setOpaque(false);
        xWrap.add(x);
        header.add(t,     BorderLayout.WEST);
        header.add(xWrap, BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);
        return dlg;
    }

    private JPanel dialogFooter(JButton cancel, JButton save) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)));
        p.add(cancel); p.add(save);
        return p;
    }

    private static JPanel makeStepper(JTextField field) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        JButton minus = stepBtn("−");
        JButton plus  = stepBtn("+");
        minus.addActionListener(e -> {
            try { int v = Integer.parseInt(field.getText().trim()); if (v > 0) field.setText(String.valueOf(v-1)); } catch (Exception ignored) {}
        });
        plus.addActionListener(e -> {
            try { int v = Integer.parseInt(field.getText().trim()); field.setText(String.valueOf(v+1)); } catch (Exception ignored) {}
        });
        wrap.add(minus, BorderLayout.WEST);
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(plus,  BorderLayout.EAST);
        return wrap;
    }

    private static JButton stepBtn(String sym) {
        JButton b = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() || getModel().isPressed() ? STEP_HOVER : SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(getFont());
                g2.setColor(MUTED);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(sym, (getWidth()-fm.stringWidth(sym))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 18));
        b.setPreferredSize(new Dimension(36, 0));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTextField styledField(String val) {
        JTextField f = new JTextField(val) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(8, 11, 8, 11));
        return f;
    }

    private JPanel formField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        JLabel l = new JLabel(label); l.setFont(new Font("Dialog", Font.BOLD, 11)); l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel formRow(String l1, JComponent f1, String l2, JComponent f2) {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        p.add(formField(l1, f1)); p.add(formField(l2, f2));
        return p;
    }

    // ── Button helpers ────────────────────────────────────────────────────────
    private JButton navBtn(String label, Color bg, Color fg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? getBackground().darker() : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(fg);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton ghostBtn(String label) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(0xF1F5F9) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.PLAIN, 13));
        b.setBackground(Color.WHITE); b.setForeground(TXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton pill(String label, int catId, boolean active) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setBackground(active ? ACCENT : BORDER);
        b.setForeground(active ? Color.WHITE : new Color(0x475569));
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setBorderPainted(false); b.setFocusPainted(false); b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 16, 6, 16));
        return b;
    }

    private JButton tinyBtn(String label, Color fg) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.PLAIN, 11));
        b.setForeground(fg);
        b.setBackground(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 24));
        b.setBorderPainted(false); b.setFocusPainted(false); b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(4, 10, 4, 10));
        return b;
    }

    // ── Utility ───────────────────────────────────────────────────────────────
    static Color catColor(int catId) {
        return CAT_COLORS[(Math.abs(catId) - 1) % CAT_COLORS.length];
    }

    private static Component vs(int h) { return Box.createVerticalStrut(h); }
    private static String val(String s) { return s != null ? s : ""; }

    private static DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { r.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }
}
