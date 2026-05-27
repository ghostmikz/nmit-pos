package view.panels;

import static view.AppColors.*;

import i18n.I18n;
import i18n.LanguageListener;
import model.CartItem;
import model.Category;
import model.Product;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Base64;
import java.util.function.Consumer;

public class POSPanel extends JPanel implements LanguageListener {

    @FunctionalInterface
    public interface PaymentHandler {
        void pay(List<CartItem> items, List<Map<String, Object>> payments, BigDecimal total, Runnable onSuccess);
    }

    @FunctionalInterface
    public interface ImageLoader {
        void load(int productId, Consumer<String> callback);
    }


    private static final ImageIcon IC_CAMERA = view.MainFrame.assetSq("/assets/icons/camera.png", 28);
    private static final ImageIcon IC_SEARCH = view.MainFrame.assetSq("/assets/icons/search.png", 22);

    // Paint-path Color constants — allocated once, reused on every repaint
    private static final Color SEARCH_BORDER  = new Color(0xE2E8F0);
    private static final Color CARD_SHADOW_C  = new Color(0, 0, 0, 10);
    private static final Color IMG_BG_C       = new Color(0xF1F5F9);
    private static final Color QTY_BTN_C      = new Color(0xF1F5F9);
    private static final Color CAT_FG         = new Color(0x475569);
    private static final Color HOVER_CARD_BG  = new Color(0xFFF5F5);

    // Font constants — allocated once, shared across all product cards and cart rows
    private static final Font FONT_TITLE  = new Font("Dialog", Font.BOLD,  20);
    private static final Font FONT_BTN    = new Font("Dialog", Font.BOLD,  14);
    private static final Font FONT_BODY   = new Font("Dialog", Font.PLAIN, 14);
    private static final Font FONT_SM_B   = new Font("Dialog", Font.BOLD,  13);
    private static final Font FONT_SM_R   = new Font("Dialog", Font.PLAIN, 13);
    private static final Font FONT_XS_B   = new Font("Dialog", Font.BOLD,  11);
    private static final Font FONT_XS_R   = new Font("Dialog", Font.PLAIN, 11);

    private static final NumberFormat       MNT;
    private static final DateTimeFormatter  CLK = DateTimeFormatter.ofPattern("HH:mm:ss");

    static { MNT = NumberFormat.getNumberInstance(new Locale("mn", "MN")); }

    private final User                        user;
    private       PaymentHandler              paymentHandler;
    private       ImageLoader                 imageLoader;
    private       List<Map<String, Object>>   paymentMethods = new ArrayList<>();
    private final PaymentDialogs              paymentDialogs;

    private final Map<Integer, Image>  imageCache = new HashMap<>();
    // Product card cache — rebuilt only when product list changes, not on every filter
    private final Map<Integer, JPanel> cardCache  = new HashMap<>();
    // Cart row label caches — updated in-place on qty change instead of full rebuild
    private final java.util.IdentityHashMap<CartItem, JLabel> cartQtyLbls = new java.util.IdentityHashMap<>();
    private final java.util.IdentityHashMap<CartItem, JLabel> cartSubLbls = new java.util.IdentityHashMap<>();

    private final List<Product>        allProducts = new ArrayList<>();
    private final List<CartItem>       cart        = new ArrayList<>();
    private final List<List<CartItem>> heldCarts   = new ArrayList<>();
    private int    selectedCategoryId = -1;
    private String searchQuery        = "";
    private String searchPlaceholder  = I18n.t("pos.search");
    private String barcodePlaceholder = I18n.t("pos.barcode");

    // UI refs updated on language change
    private JLabel     clockLabel;
    private JLabel     topBarTitle;
    private JTextField searchField;
    private JTextField barcodeField;
    private JPanel     categoryBar;
    private JPanel     productGrid;
    private JPanel     cartItemsPanel;
    private JLabel     totalLabel;
    private JLabel     totalPrefixLabel;
    private JPanel     holdOrdersBar;
    private CardLayout cartCardLayout;
    private JPanel     cartCardPanel;
    private JLabel     emptyLabel;
    private JLabel     cartTitleLabel;
    private JButton    clearBtn;
    private JButton    payBtnCash, payBtnCard, payBtnQPay, payBtnMonpay, payBtnSplit;
    private javax.swing.Timer clockTimer;
    // Barcode scanner
    private final StringBuilder barcodeBuffer = new StringBuilder();
    private javax.swing.Timer   barcodeTimer;
    private KeyEventDispatcher  scannerDispatcher;
    // Toast
    private JLabel toastLabel;
    private javax.swing.Timer toastTimer;

    public POSPanel(User user) {
        this.user   = user;
        paymentDialogs = new PaymentDialogs(this, MNT);
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildLeft(), BorderLayout.CENTER);
        add(buildCart(), BorderLayout.EAST);
        startClock();
        initBarcodeScanner();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setProducts(List<Product> products) {
        allProducts.clear();
        allProducts.addAll(products);
        cardCache.clear();
        refreshGrid();
    }

    public void setCategories(List<Category> categories) {
        categoryBar.removeAll();
        categoryBar.add(pill(I18n.t("pos.cat.all"), -1));
        for (Category c : categories) categoryBar.add(pill(c.getName(), c.getId()));
        categoryBar.revalidate();
        categoryBar.repaint();
    }

    public void setPaymentHandler(PaymentHandler handler) {
        this.paymentHandler = handler;
        paymentDialogs.setPaymentHandler(handler);
    }
    public void setImageLoader(ImageLoader loader) { this.imageLoader = loader; }
    public void setPaymentMethods(List<Map<String, Object>> methods) {
        this.paymentMethods = methods != null ? methods : new ArrayList<>();
        paymentDialogs.setPaymentMethods(this.paymentMethods);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onLanguageChanged() {
        searchPlaceholder = I18n.t("pos.search");
        topBarTitle.setText(I18n.t("pos.title"));
        if (searchPlaceholder.equals(searchField.getText()) || searchField.getText().isEmpty()) {
            searchField.setText(searchPlaceholder);
            searchField.setForeground(new Color(0x94A3B8));
            searchQuery = "";
        }
        barcodePlaceholder = I18n.t("pos.barcode");
        if (barcodePlaceholder.equals(barcodeField.getText()) || barcodeField.getText().isEmpty()) {
            barcodeField.setText(barcodePlaceholder);
            barcodeField.setForeground(new Color(0x94A3B8));
        }
        cartTitleLabel.setText(I18n.t("pos.cart"));
        clearBtn.setText(I18n.t("pos.clear"));
        emptyLabel.setText("<html><center>" + I18n.t("pos.cart.empty") + "</center></html>");
        totalPrefixLabel.setText(I18n.t("pos.total"));
        payBtnCash.setText(I18n.t("pos.payment.cash"));
        payBtnCard.setText(I18n.t("pos.payment.card"));
        payBtnQPay.setText(I18n.t("pos.payment.qpay"));
        payBtnMonpay.setText(I18n.t("pos.payment.monpay"));
        payBtnSplit.setText(I18n.t("pos.payment.split"));
        // Rebuild category bar pills with new "All" label
        Component[] comps = categoryBar.getComponents();
        if (comps.length > 0 && comps[0] instanceof JButton b)
            b.setText(I18n.t("pos.cat.all"));
        categoryBar.revalidate();
        categoryBar.repaint();
        rebuildHoldBar();
    }

    // ── Left area ─────────────────────────────────────────────────────────────

    private JPanel buildLeft() {
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(BG);
        left.add(buildTopBar(),   BorderLayout.NORTH);
        left.add(buildMainArea(), BorderLayout.CENTER);
        return left;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new EmptyBorder(14, 20, 14, 20));

        topBarTitle = new JLabel(I18n.t("pos.title"));
        topBarTitle.setFont(FONT_TITLE);
        topBarTitle.setForeground(TXT);
        bar.add(topBarTitle, BorderLayout.WEST);

        clockLabel = new JLabel();
        clockLabel.setFont(FONT_BODY);
        clockLabel.setForeground(MUTED);
        bar.add(clockLabel, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildMainArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(BG);
        area.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel filterArea = new JPanel();
        filterArea.setLayout(new BoxLayout(filterArea, BoxLayout.Y_AXIS));
        filterArea.setBackground(BG);
        filterArea.add(buildSearchBar());
        filterArea.add(Box.createVerticalStrut(10));
        filterArea.add(buildCategoryBar());
        filterArea.add(Box.createVerticalStrut(14));
        area.add(filterArea, BorderLayout.NORTH);

        productGrid = new JPanel() {
            private static final int GAP = 10;
            private int cols() {
                int w = getParent() != null ? getParent().getWidth() : 0;
                return w == 0 ? 5 : Math.max(1, (w - GAP) / (CARD_W + GAP));
            }
            @Override public void doLayout() {
                int w = getWidth(), cols = cols();
                int cardW = (w - GAP * (cols + 1)) / cols;
                int x = GAP, y = GAP, col = 0;
                for (Component c : getComponents()) {
                    c.setBounds(x, y, cardW, CARD_H);
                    if (++col == cols) { col = 0; x = GAP; y += CARD_H + GAP; }
                    else x += cardW + GAP;
                }
            }
            @Override public Dimension getPreferredSize() {
                int w = getParent() != null ? getParent().getWidth() : 600;
                int cols = cols(), count = getComponentCount();
                int rows = count == 0 ? 0 : (int) Math.ceil((double) count / cols);
                return new Dimension(w, rows * (CARD_H + GAP) + GAP);
            }
        };
        productGrid.setBackground(BG);
        JScrollPane scroll = new JScrollPane(productGrid,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        view.MainFrame.modernScrollBar(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        area.add(scroll, BorderLayout.CENTER);

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(FONT_SM_B);
        toastLabel.setForeground(Color.WHITE);
        toastLabel.setOpaque(true);
        toastLabel.setBackground(new Color(0xDC2626));
        toastLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        toastLabel.setVisible(false);
        area.add(toastLabel, BorderLayout.SOUTH);
        return area;
    }

    private JPanel buildSearchBar() {
        JPanel wrap = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(SEARCH_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(8, 12, 8, 12));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrap.setAlignmentX(LEFT_ALIGNMENT);

        JLabel icon = new JLabel(IC_SEARCH);
        icon.setPreferredSize(new Dimension(22, 22));
        icon.setOpaque(false);

        searchField = new JTextField(searchPlaceholder);
        searchField.setForeground(new Color(0x94A3B8));
        searchField.setFont(FONT_BODY);
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchPlaceholder.equals(searchField.getText())) { searchField.setText(""); searchField.setForeground(TXT); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) { searchField.setText(searchPlaceholder); searchField.setForeground(new Color(0x94A3B8)); }
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                String t = searchField.getText();
                searchQuery = searchPlaceholder.equals(t) ? "" : t.trim().toLowerCase();
                refreshGrid();
            }
            @Override public void insertUpdate(DocumentEvent e)  { update(); }
            @Override public void removeUpdate(DocumentEvent e)  { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        wrap.add(icon,        BorderLayout.WEST);
        wrap.add(searchField, BorderLayout.CENTER);
        return wrap;
    }

    private JScrollPane buildCategoryBar() {
        categoryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        categoryBar.setOpaque(false);
        JScrollPane scroll = new JScrollPane(categoryBar,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(0, 58));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        scroll.setAlignmentX(LEFT_ALIGNMENT);
        view.MainFrame.modernHScrollBar(scroll);
        return scroll;
    }

    private JButton pill(String label, int catId) {
        JButton btn = new JButton(label) {
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
        btn.putClientProperty("catId", catId);
        boolean active = catId == selectedCategoryId;
        btn.setBackground(active ? ACCENT : BORDER);
        btn.setForeground(active ? Color.WHITE : CAT_FG);
        btn.setFont(FONT_BTN);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        int w = btn.getFontMetrics(btn.getFont()).stringWidth(label) + 40;
        btn.setPreferredSize(new Dimension(w, 44));
        btn.addActionListener(e -> {
            selectedCategoryId = catId;
            for (Component c : categoryBar.getComponents()) {
                if (c instanceof JButton b) {
                    Object id = b.getClientProperty("catId");
                    boolean sel = id instanceof Integer i && i == catId;
                    b.setBackground(sel ? ACCENT : BORDER);
                    b.setForeground(sel ? Color.WHITE : new Color(0x475569));
                    b.repaint();
                }
            }
            refreshGrid();
        });
        return btn;
    }

    private void refreshGrid() {
        productGrid.removeAll();
        String q = searchQuery;
        for (Product p : allProducts) {
            if (!p.isActive()) continue;
            if (selectedCategoryId != -1 && p.getCategoryId() != selectedCategoryId) continue;
            if (!q.isEmpty() && !p.getName().toLowerCase().contains(q)) continue;
            productGrid.add(cardCache.computeIfAbsent(p.getId(), id -> productCard(p)));
        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    private static final int CARD_W = 155;
    private static final int CARD_H = 195;
    private static final int IMG_H  = 100;

    private JPanel productCard(Product p) {
        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_SHADOW_C);
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, 14, 14);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        card.setBackground(Color.WHITE);

        JPanel imgArea = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setColor(IMG_BG_C);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 14, 14, 14);
                Image img = imageCache.get(p.getId());
                if (img != null) {
                    int iw = img.getWidth(null), ih = img.getHeight(null);
                    int w = getWidth(), h = getHeight();
                    double scale = Math.max((double) w / iw, (double) h / ih);
                    int dw = (int)(iw * scale), dh = (int)(ih * scale);
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h + 14, 14, 14));
                    g2.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                } else if (IC_CAMERA != null) {
                    int sz = 28, x = (getWidth() - sz) / 2, y = (getHeight() - sz) / 2;
                    g2.drawImage(IC_CAMERA.getImage(), x, y, sz, sz, null);
                }
                g2.dispose();
            }
        };
        imgArea.setOpaque(false);
        imgArea.setPreferredSize(new Dimension(CARD_W, IMG_H));

        if (p.isHasImage() && !imageCache.containsKey(p.getId()) && imageLoader != null) {
            imageLoader.load(p.getId(), base64 -> {
                if (base64 != null) {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(base64);
                        Image decoded = ImageIO.read(new ByteArrayInputStream(bytes));
                        if (decoded != null) imageCache.put(p.getId(), decoded);
                    } catch (Exception ignored) {}
                }
                SwingUtilities.invokeLater(imgArea::repaint);
            });
        }

        JPanel info = new JPanel(new BorderLayout(0, 4));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(8, 10, 10, 10));

        JLabel name = new JLabel("<html><body style='width:" + (CARD_W - 20) + "px'>" + p.getName() + "</body></html>");
        name.setFont(FONT_SM_R);
        name.setForeground(TXT);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JLabel price = new JLabel("₮" + MNT.format(p.getPrice()));
        price.setFont(FONT_SM_B);
        price.setForeground(ACCENT);
        JLabel stock = new JLabel(p.getStockQuantity() + (p.getUnit() != null ? p.getUnit() : "ш"));
        stock.setFont(FONT_XS_R);
        stock.setForeground(MUTED);
        stock.setHorizontalAlignment(SwingConstants.RIGHT);
        bottom.add(price, BorderLayout.WEST);
        bottom.add(stock, BorderLayout.EAST);

        info.add(name,   BorderLayout.CENTER);
        info.add(bottom, BorderLayout.SOUTH);

        card.add(imgArea, BorderLayout.NORTH);
        card.add(info,    BorderLayout.CENTER);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { addToCart(p); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(HOVER_CARD_BG); card.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { card.setBackground(Color.WHITE);         card.repaint(); }
        };
        for (Component c : new Component[]{card, imgArea, info, name, bottom, price, stock})
            c.addMouseListener(ma);
        return card;
    }

    private void addToCart(Product p) {
        for (CartItem ci : cart) {
            if (ci.getProduct().getId() == p.getId()) { ci.increment(); refreshCart(); return; }
        }
        cart.add(new CartItem(p));
        refreshCart();
    }

    // ── Cart panel ────────────────────────────────────────────────────────────

    private JPanel buildCart() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xE2E8F0)));

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(buildCartHeader(),  BorderLayout.NORTH);
        north.add(buildBarcodeRow(), BorderLayout.SOUTH);

        panel.add(north,             BorderLayout.NORTH);
        panel.add(buildCartCenter(), BorderLayout.CENTER);
        panel.add(buildCartFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBarcodeRow() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)),
                new EmptyBorder(8, 16, 8, 16)));

        JPanel fieldWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(SEARCH_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        fieldWrap.setOpaque(false);
        fieldWrap.setBorder(new EmptyBorder(5, 10, 5, 10));

        barcodeField = new JTextField(barcodePlaceholder);
        barcodeField.setFont(FONT_SM_R);
        barcodeField.setForeground(new Color(0x94A3B8));
        barcodeField.setOpaque(false);
        barcodeField.setBorder(null);
        barcodeField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (barcodePlaceholder.equals(barcodeField.getText())) {
                    barcodeField.setText("");
                    barcodeField.setForeground(TXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (barcodeField.getText().isEmpty()) {
                    barcodeField.setText(barcodePlaceholder);
                    barcodeField.setForeground(new Color(0x94A3B8));
                }
            }
        });
        barcodeField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = barcodeField.getText().trim();
                    barcodeField.setText(barcodePlaceholder);
                    barcodeField.setForeground(new Color(0x94A3B8));
                    if (!text.isEmpty() && !text.equals(barcodePlaceholder)) {
                        processBarcodeInput(text);
                    }
                }
            }
        });

        fieldWrap.add(barcodeField, BorderLayout.CENTER);
        wrap.add(fieldWrap, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildCartHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(14, 16, 12, 16));

        cartTitleLabel = new JLabel(I18n.t("pos.cart"));
        cartTitleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        cartTitleLabel.setForeground(TXT);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JLabel cashier = new JLabel(user.getFullName());
        cashier.setFont(new Font("Dialog", Font.PLAIN, 12));
        cashier.setForeground(MUTED);

        clearBtn = new JButton(I18n.t("pos.clear")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(0xFECACA) : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        clearBtn.setFont(new Font("Dialog", Font.PLAIN, 12));
        clearBtn.setForeground(new Color(0xEF4444));
        clearBtn.setBackground(new Color(0xFEF2F2));
        clearBtn.setContentAreaFilled(false);
        clearBtn.setBorderPainted(false);
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> clearCart());

        right.add(cashier);
        right.add(clearBtn);
        header.add(cartTitleLabel, BorderLayout.WEST);
        header.add(right,          BorderLayout.EAST);
        return header;
    }

    private JPanel buildCartCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        holdOrdersBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        holdOrdersBar.setBackground(new Color(0xF8FAFC));
        holdOrdersBar.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0xE2E8F0)));
        rebuildHoldBar();
        center.add(holdOrdersBar, BorderLayout.NORTH);

        cartCardLayout = new CardLayout();
        cartCardPanel  = new JPanel(cartCardLayout);
        cartCardPanel.setOpaque(false);

        JPanel emptyCard = new JPanel();
        emptyCard.setLayout(new BoxLayout(emptyCard, BoxLayout.Y_AXIS));
        emptyCard.setBackground(Color.WHITE);
        ImageIcon cartIcon = view.MainFrame.assetSq("/assets/icons/pos.png", 52);
        if (cartIcon != null) {
            JLabel iconLbl = new JLabel(cartIcon);
            iconLbl.setAlignmentX(CENTER_ALIGNMENT);
            iconLbl.setOpaque(false);
            emptyCard.add(Box.createVerticalGlue());
            emptyCard.add(iconLbl);
            emptyCard.add(Box.createVerticalStrut(12));
        } else {
            emptyCard.add(Box.createVerticalGlue());
        }
        emptyLabel = new JLabel("<html><center>" + I18n.t("pos.cart.empty") + "</center></html>", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        emptyLabel.setForeground(new Color(0xCBD5E1));
        emptyLabel.setAlignmentX(CENTER_ALIGNMENT);
        emptyCard.add(emptyLabel);
        emptyCard.add(Box.createVerticalGlue());

        cartItemsPanel = new JPanel();
        cartItemsPanel.setLayout(new BoxLayout(cartItemsPanel, BoxLayout.Y_AXIS));
        cartItemsPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(cartItemsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        view.MainFrame.modernScrollBar(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        cartCardPanel.add(emptyCard, "EMPTY");
        cartCardPanel.add(scroll,    "ITEMS");
        cartCardLayout.show(cartCardPanel, "EMPTY");

        center.add(cartCardPanel, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildCartFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)),
                new EmptyBorder(12, 16, 14, 16)));

        // Total row
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setAlignmentX(LEFT_ALIGNMENT);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        totalPrefixLabel = new JLabel(I18n.t("pos.total"));
        totalPrefixLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        totalPrefixLabel.setForeground(TXT);
        totalLabel = new JLabel("₮0");
        totalLabel.setFont(new Font("Dialog", Font.BOLD, 22));
        totalLabel.setForeground(ACCENT);
        totalRow.add(totalPrefixLabel, BorderLayout.WEST);
        totalRow.add(totalLabel,       BorderLayout.EAST);
        footer.add(totalRow);
        footer.add(Box.createVerticalStrut(12));

        // Payment buttons
        JPanel payGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        payGrid.setOpaque(false);
        payBtnCash   = payBtn(I18n.t("pos.payment.cash"),   1, new Color(0x16A34A));
        payBtnCard   = payBtn(I18n.t("pos.payment.card"),   2, new Color(0x2563EB));
        payBtnQPay   = payBtn(I18n.t("pos.payment.qpay"),   3, new Color(0x7C3AED));
        payBtnMonpay = payBtn(I18n.t("pos.payment.monpay"), 4, new Color(0xD97706));
        payGrid.add(payBtnCash);
        payGrid.add(payBtnCard);
        payGrid.add(payBtnQPay);
        payGrid.add(payBtnMonpay);

        payBtnSplit = new JButton(I18n.t("pos.payment.split")) {
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
        payBtnSplit.setFont(FONT_SM_B);
        payBtnSplit.setBackground(new Color(0xF1F5F9));
        payBtnSplit.setForeground(new Color(0x475569));
        payBtnSplit.setBorderPainted(false);
        payBtnSplit.setFocusPainted(false);
        payBtnSplit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        payBtnSplit.addActionListener(e -> {
            if (cart.isEmpty()) { showError(I18n.t("pos.error.empty_cart")); return; }
            showPaymentDialog(0);
        });

        JPanel payArea = new JPanel();
        payArea.setLayout(new BoxLayout(payArea, BoxLayout.Y_AXIS));
        payArea.setOpaque(false);
        payArea.setAlignmentX(LEFT_ALIGNMENT);
        payGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        payGrid.setAlignmentX(LEFT_ALIGNMENT);
        payBtnSplit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        payBtnSplit.setAlignmentX(LEFT_ALIGNMENT);
        payArea.add(payGrid);
        payArea.add(Box.createVerticalStrut(8));
        payArea.add(payBtnSplit);
        footer.add(payArea);

        return footer;
    }

    private JButton payBtn(String label, int methodId, Color color) {
        JButton btn = new JButton(label) {
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
        btn.setFont(FONT_SM_B);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (cart.isEmpty()) { showError(I18n.t("pos.error.empty_cart")); return; }
            showPaymentDialog(methodId);
        });
        return btn;
    }

    private void showPaymentDialog(int methodId) {
        paymentDialogs.showPaymentDialog(methodId, cartTotal(), new ArrayList<>(cart), this::clearCart);
    }

    // ── Cart logic ────────────────────────────────────────────────────────────

    private void holdCart() {
        if (cart.isEmpty()) return;
        heldCarts.add(new ArrayList<>(cart));
        cart.clear();
        refreshCart();
        rebuildHoldBar();
    }

    private void restoreHeld(int idx) {
        if (idx < 0 || idx >= heldCarts.size()) return;
        cart.clear();
        cart.addAll(heldCarts.remove(idx));
        refreshCart();
        rebuildHoldBar();
    }

    private void clearCart() {
        cart.clear();
        refreshCart();
    }

    private BigDecimal cartSubtotal() {
        return cart.stream().map(CartItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal cartTotal() { return cartSubtotal(); }

    private void rebuildHoldBar() {
        holdOrdersBar.removeAll();
        JButton holdBtn = holdBarBtn(I18n.t("pos.hold"), ACCENT, Color.WHITE);
        holdBtn.addActionListener(e -> holdCart());
        holdOrdersBar.add(holdBtn);
        for (int i = 0; i < heldCarts.size(); i++) {
            final int idx = i;
            JButton badge = holdBarBtn(I18n.t("pos.hold.order") + " " + (i + 1), new Color(0xFEF3C7), new Color(0x92400E));
            badge.addActionListener(e -> restoreHeld(idx));
            holdOrdersBar.add(badge);
        }
        holdOrdersBar.revalidate();
        holdOrdersBar.repaint();
    }

    private JButton holdBarBtn(String label, Color bg, Color fg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(FONT_XS_B);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(4, 10, 4, 10));
        return btn;
    }

    private void refreshCart() {
        boolean sameItems = cart.size() == cartQtyLbls.size() && cartQtyLbls.keySet().containsAll(cart);
        if (sameItems) {
            // Fast path: only update qty and subtotal labels, no layout rebuild
            for (CartItem ci : cart) {
                cartQtyLbls.get(ci).setText(String.valueOf(ci.getQuantity()));
                cartSubLbls.get(ci).setText("₮" + MNT.format(ci.getSubtotal()));
            }
            cartItemsPanel.repaint();
        } else {
            // Layout changed (item added or removed) — rebuild rows and refresh caches
            cartQtyLbls.clear();
            cartSubLbls.clear();
            cartItemsPanel.removeAll();
            for (int i = 0; i < cart.size(); i++) {
                cartItemsPanel.add(cartItemRow(cart.get(i)));
                if (i < cart.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(SEARCH_BORDER);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    cartItemsPanel.add(sep);
                }
            }
            cartItemsPanel.revalidate();
            cartItemsPanel.repaint();
        }
        totalLabel.setText("₮" + MNT.format(cartTotal()));
        cartCardLayout.show(cartCardPanel, cart.isEmpty() ? "EMPTY" : "ITEMS");
    }

    private JPanel cartItemRow(CartItem ci) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(Color.WHITE);
        row.setBorder(new EmptyBorder(10, 16, 10, 16));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JPanel info = new JPanel(new BorderLayout());
        info.setOpaque(false);
        String rawName     = ci.getProduct().getName();
        String displayName = rawName.length() > 22 ? rawName.substring(0, 20) + "…" : rawName;
        JLabel nameLbl = new JLabel(displayName);
        nameLbl.setFont(FONT_SM_R);
        nameLbl.setForeground(TXT);
        if (rawName.length() > 22) nameLbl.setToolTipText(rawName);
        JLabel unitLbl = new JLabel("₮" + MNT.format(ci.getProduct().getPrice()) + "/" + (ci.getProduct().getUnit() != null ? ci.getProduct().getUnit() : "ш"));
        unitLbl.setFont(FONT_XS_R);
        unitLbl.setForeground(MUTED);
        info.add(nameLbl, BorderLayout.CENTER);
        info.add(unitLbl, BorderLayout.SOUTH);

        JButton minus = qtyBtn("−");
        JLabel  qLbl  = new JLabel(String.valueOf(ci.getQuantity()));
        qLbl.setFont(FONT_SM_B);
        qLbl.setPreferredSize(new Dimension(24, 24));
        qLbl.setMaximumSize(new Dimension(24, 24));
        qLbl.setHorizontalAlignment(SwingConstants.CENTER);
        JButton plus  = qtyBtn("+");

        minus.addActionListener(e -> {
            if (ci.getQuantity() == 1) cart.remove(ci); else ci.decrement();
            refreshCart();
        });
        plus.addActionListener(e -> { ci.increment(); refreshCart(); });

        JLabel sub = new JLabel("₮" + MNT.format(ci.getSubtotal()));
        sub.setFont(FONT_SM_B);
        sub.setForeground(ACCENT);
        sub.setPreferredSize(new Dimension(70, 0));
        sub.setMaximumSize(new Dimension(70, Integer.MAX_VALUE));
        sub.setHorizontalAlignment(SwingConstants.RIGHT);

        // Cache the mutable labels so refreshCart fast-path can update them without rebuild
        cartQtyLbls.put(ci, qLbl);
        cartSubLbls.put(ci, sub);

        row.add(info);
        row.add(Box.createHorizontalGlue());
        row.add(minus);
        row.add(Box.createHorizontalStrut(4));
        row.add(qLbl);
        row.add(Box.createHorizontalStrut(4));
        row.add(plus);
        row.add(Box.createHorizontalStrut(10));
        row.add(sub);
        return row;
    }

    private JButton qtyBtn(String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? new Color(0xCBD5E1)
                         : getModel().isRollover() ? new Color(0xE2E8F0)
                         : QTY_BTN_C;
                int d = Math.min(getWidth(), getHeight()) - 2;
                g2.setColor(bg);
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.setFont(getFont());
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(label)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(label, tx, ty);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(new Font("Dialog", Font.PLAIN, 14));
        btn.setForeground(TXT);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setRolloverEnabled(true);
        btn.getModel().addChangeListener(e -> btn.repaint());
        btn.setPreferredSize(new Dimension(26, 26));
        btn.setMaximumSize(new Dimension(26, 26));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Barcode scanner ───────────────────────────────────────────────────────

    private void initBarcodeScanner() {
        barcodeTimer = new javax.swing.Timer(120, e -> barcodeBuffer.setLength(0));
        barcodeTimer.setRepeats(false);

        scannerDispatcher = event -> {
            if (!isShowing()) return false;
            if (event.getID() != KeyEvent.KEY_TYPED) return false;
            // Only intercept when the main app window (JFrame) has focus — any dialog stops the scanner
            Window activeWin = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            if (!(activeWin instanceof JFrame)) return false;
            Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (focused == searchField || focused == barcodeField) return false;

            char c = event.getKeyChar();
            if (c == '\n' || c == '\r') {
                barcodeTimer.stop();
                String code = barcodeBuffer.toString().trim();
                barcodeBuffer.setLength(0);
                if (code.length() >= 3) {
                    SwingUtilities.invokeLater(() -> processBarcodeInput(code));
                    return true;
                }
                return false;
            }
            if (Character.isLetterOrDigit(c) || c == '-') {
                barcodeBuffer.append(c);
                barcodeTimer.restart();
            }
            return false;
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(scannerDispatcher);
    }

    private void processBarcodeInput(String barcode) {
        for (Product p : allProducts) {
            if (p.isActive() && barcode.equalsIgnoreCase(p.getBarcode())) {
                addToCart(p);
                return;
            }
        }
        showBarcodeToast(I18n.t("pos.barcode.not.found").replace("{0}", barcode));
    }

    private void showBarcodeToast(String msg) {
        if (toastLabel == null) return;
        toastLabel.setText(msg);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new javax.swing.Timer(2500, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    // ── Clock ─────────────────────────────────────────────────────────────────

    private void startClock() {
        clockTimer = new javax.swing.Timer(1000, e -> clockLabel.setText(LocalTime.now().format(CLK)));
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (clockTimer != null) clockTimer.stop();
        if (toastTimer  != null) toastTimer.stop();
        if (scannerDispatcher != null)
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(scannerDispatcher);
    }
}
