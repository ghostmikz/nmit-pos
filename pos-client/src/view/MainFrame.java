package view;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;
import view.panels.*;

import static view.AppColors.SCROLL_THUMB;
import static view.AppColors.SCROLL_THUMB_HV;
import static view.AppColors.SURFACE;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements LanguageListener {

    private final User user;
    private JPanel     contentArea;
    private CardLayout cardLayout;

    private JButton posBtn, inventoryBtn, reportsBtn, dashboardBtn, usersBtn, settingsBtn, logoutBtn;
    private JLabel  roleLabel;

    private final List<LanguageListener> panelListeners = new ArrayList<>();
    private Runnable logoutListener;

    // ── palette ───────────────────────────────────────────────────────────────
    private static final Color SIDEBAR         = new Color(0xb31b1b);
    private static final Color NAV_FG          = new Color(0xFFCDD2);
    private static final Color HOVER_BG        = new Color(255, 255, 255, 22);
    private static final Color LOGOUT_FG       = new Color(0xFFABAB);
    private static final Color USER_CARD_TINT  = new Color(0, 0, 0, 45);
    private static final Color AVATAR_FILL     = new Color(0xcc2e2e);
    private static final Color NAV_ACTIVE_BG   = new Color(255, 255, 255, 38);
    private static final Color NAV_ACTIVE_PILL = new Color(255, 255, 255, 220);
    private static final Color SEP_LINE        = new Color(255, 255, 255, 30);
    private static final Color WIN_BTN_DIM     = new Color(255, 255, 255, 140);
    // SCROLL_THUMB, SCROLL_THUMB_HV — from AppColors (static import above)

    // ── icons ─────────────────────────────────────────────────────────────────
    private static final ImageIcon IC_LOGO;
    private static final ImageIcon IC_POS;
    private static final ImageIcon IC_INVENTORY;
    private static final ImageIcon IC_REPORTS;
    private static final ImageIcon IC_DASHBOARD;
    private static final ImageIcon IC_USERS;
    private static final ImageIcon IC_SETTINGS;
    private static final ImageIcon IC_LOGOUT;

    static {
        IC_LOGO      = assetH ("/assets/logo.png",            36);
        IC_POS       = assetSq("/assets/icons/pos.png",       18);
        IC_INVENTORY = assetSq("/assets/icons/inventory.png", 18);
        IC_REPORTS   = assetSq("/assets/icons/reports.png",   18);
        IC_DASHBOARD = assetSq("/assets/icons/dashboard.png", 18);
        IC_USERS     = assetSq("/assets/icons/users.png",     18);
        IC_SETTINGS  = assetSq("/assets/icons/settings.png",  18);
        IC_LOGOUT    = assetSq("/assets/icons/logout.png",    18);
    }

    public MainFrame(User user) {
        this.user = user;
        setTitle(I18n.t("app.title") + " — " + user.getFullName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setContentPane(buildUI());
        I18n.addListener(this);
    }

    @Override
    public void onLanguageChanged() {
        setTitle(I18n.t("app.title") + " — " + user.getFullName());
        if (roleLabel    != null) roleLabel.setText(roleName(user.getRole()));
        if (posBtn       != null) posBtn.setText(I18n.t("nav.pos"));
        if (inventoryBtn != null) inventoryBtn.setText(I18n.t("nav.inventory"));
        if (reportsBtn   != null) reportsBtn.setText(I18n.t("nav.reports"));
        if (dashboardBtn != null) dashboardBtn.setText(I18n.t("nav.dashboard"));
        if (usersBtn     != null) usersBtn.setText(I18n.t("nav.users"));
        if (settingsBtn  != null) settingsBtn.setText(I18n.t("nav.settings"));
        if (logoutBtn    != null) logoutBtn.setText(I18n.t("nav.logout"));
        for (LanguageListener l : panelListeners) l.onLanguageChanged();
    }

    // ── layout ────────────────────────────────────────────────────────────────
    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildSidebar(),  BorderLayout.WEST);
        root.add(buildContent(),  BorderLayout.CENTER);
        return root;
    }

    // ── title bar (full-width, close/min on right) ────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SIDEBAR);
        bar.setPreferredSize(new Dimension(0, 40));

        JPanel winRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        winRow.setOpaque(false);
        winRow.add(winBtn("−", e -> setState(ICONIFIED)));
        winRow.add(winBtn("×", e -> dispatchEvent(
                new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));
        bar.add(winRow, BorderLayout.EAST);
        return bar;
    }

    // ── sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 0));

        // ── logo — wrapped in LEFT_ALIGNMENT panel to avoid BoxLayout mismatch ──
        sidebar.add(Box.createVerticalStrut(16));
        JPanel logoWrap = new JPanel(new BorderLayout());
        logoWrap.setOpaque(false);
        logoWrap.setMaximumSize(new Dimension(240, 48));
        logoWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (IC_LOGO != null) logoWrap.add(new JLabel(IC_LOGO, SwingConstants.CENTER), BorderLayout.CENTER);
        sidebar.add(logoWrap);
        sidebar.add(Box.createVerticalStrut(20));

        // ── nav items ─────────────────────────────────────────────────────────
        posBtn = navBtn(I18n.t("nav.pos"), true, IC_POS);
        posBtn.addActionListener(e -> switchPanel("POS", posBtn, sidebar));
        sidebar.add(posBtn);
        sidebar.add(Box.createVerticalStrut(2));

        if (user.isManager()) {
            inventoryBtn = navBtn(I18n.t("nav.inventory"), false, IC_INVENTORY);
            inventoryBtn.addActionListener(e -> switchPanel("INVENTORY", inventoryBtn, sidebar));
            sidebar.add(inventoryBtn);
            sidebar.add(Box.createVerticalStrut(2));

            reportsBtn = navBtn(I18n.t("nav.reports"), false, IC_REPORTS);
            reportsBtn.addActionListener(e -> switchPanel("REPORTS", reportsBtn, sidebar));
            sidebar.add(reportsBtn);
            sidebar.add(Box.createVerticalStrut(2));

            dashboardBtn = navBtn(I18n.t("nav.dashboard"), false, IC_DASHBOARD);
            dashboardBtn.addActionListener(e -> switchPanel("DASHBOARD", dashboardBtn, sidebar));
            sidebar.add(dashboardBtn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        if (user.isAdmin()) {
            usersBtn = navBtn(I18n.t("nav.users"), false, IC_USERS);
            usersBtn.addActionListener(e -> switchPanel("USERS", usersBtn, sidebar));
            sidebar.add(usersBtn);
            sidebar.add(Box.createVerticalStrut(2));
        }

        // ── bottom ────────────────────────────────────────────────────────────
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(thinSep());
        sidebar.add(Box.createVerticalStrut(6));

        settingsBtn = navBtn(I18n.t("nav.settings"), false, IC_SETTINGS);
        settingsBtn.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        sidebar.add(settingsBtn);
        sidebar.add(Box.createVerticalStrut(2));

        logoutBtn = navBtn(I18n.t("nav.logout"), false, IC_LOGOUT);
        logoutBtn.setForeground(LOGOUT_FG);
        logoutBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    I18n.t("nav.logout") + "?", I18n.t("common.confirm"),
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION && logoutListener != null)
                logoutListener.run();
        });
        sidebar.add(logoutBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(thinSep());
        sidebar.add(buildUserCard());
        sidebar.add(Box.createVerticalStrut(12));
        return sidebar;
    }

    private JPanel buildUserCard() {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(USER_CARD_TINT);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(240, 68));

        JLabel avatar = new JLabel(initials(user.getFullName()), SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AVATAR_FILL);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font("Dialog", Font.BOLD, 14));
        avatar.setForeground(Color.WHITE);
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(40, 40));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(user.getFullName());
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        roleLabel = new JLabel(roleName(user.getRole()));
        roleLabel.setFont(new Font("Dialog", Font.PLAIN, 11));
        roleLabel.setForeground(NAV_FG);
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(roleLabel);

        card.add(avatar, BorderLayout.WEST);
        card.add(info,   BorderLayout.CENTER);
        return card;
    }

    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        return contentArea;
    }

    // ── public API ────────────────────────────────────────────────────────────
    public void addPanel(JPanel panel, String key) {
        contentArea.add(panel, key);
        if (panel instanceof LanguageListener ll) panelListeners.add(ll);
    }

    public void showPanel(String key)                    { cardLayout.show(contentArea, key); }
    public void setLogoutListener(Runnable listener)     { this.logoutListener = listener; }

    public void close() {
        I18n.removeListener(this);
        dispose();
    }

    // ── nav switching ─────────────────────────────────────────────────────────
    private void switchPanel(String key, JButton active, JPanel sidebar) {
        cardLayout.show(contentArea, key);
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton b) {
                boolean sel = b == active;
                b.putClientProperty("active", sel);
                if (!LOGOUT_FG.equals(b.getForeground()))
                    b.setForeground(sel ? Color.WHITE : NAV_FG);
                b.repaint();
            }
        }
    }

    // ── nav button with pill + hover ──────────────────────────────────────────
    private JButton navBtn(String label, boolean selected, ImageIcon icon) {
        JButton btn = new JButton(label, icon) {
            @Override protected void paintComponent(Graphics g) {
                boolean active  = Boolean.TRUE.equals(getClientProperty("active"));
                boolean hovered = getModel().isRollover();
                if (active || hovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? NAV_ACTIVE_BG : HOVER_BG);
                    g2.fillRoundRect(8, 4, getWidth() - 16, getHeight() - 8, 10, 10);
                    if (active) {
                        g2.setColor(NAV_ACTIVE_PILL);
                        g2.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
                    }
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.putClientProperty("active", selected);
        btn.setFont(new Font("Dialog", Font.PLAIN, 14));
        btn.setForeground(selected ? Color.WHITE : NAV_FG);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setIconTextGap(12);
        btn.setBorder(new EmptyBorder(0, 20, 0, 0));
        btn.setMaximumSize(new Dimension(240, 44));
        btn.setPreferredSize(new Dimension(240, 44));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton winBtn(String label, ActionListener action) {
        JButton b = new JButton(label);
        b.setFont(new Font("Dialog", Font.PLAIN, 16));
        b.setForeground(WIN_BTN_DIM);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(Color.WHITE); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(WIN_BTN_DIM); }
        });
        b.addActionListener(action);
        return b;
    }

    private JPanel thinSep() {
        JPanel s = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(SEP_LINE);
                g.fillRect(16, 0, getWidth() - 32, 1);
            }
        };
        s.setOpaque(false);
        s.setMaximumSize(new Dimension(240, 1));
        s.setPreferredSize(new Dimension(240, 1));
        s.setAlignmentX(LEFT_ALIGNMENT);
        return s;
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String roleName(String role) {
        return switch (role) {
            case "admin"   -> I18n.t("users.role.admin");
            case "manager" -> I18n.t("users.role.manager");
            default        -> I18n.t("users.role.cashier");
        };
    }

    // ── image utilities ───────────────────────────────────────────────────────
    public static ImageIcon assetH(String path, int targetH) {
        URL url = MainFrame.class.getResource(path);
        if (url == null) return null;
        try {
            BufferedImage src = ImageIO.read(url);
            if (src == null) return null;
            int targetW = Math.max(1, src.getWidth() * targetH / src.getHeight());
            return hdpiIcon(scaleHQ(src, targetW, targetH), scaleHQ(src, targetW * 2, targetH * 2), targetW, targetH);
        } catch (Exception e) { return null; }
    }

    public static ImageIcon assetSq(String path, int size) {
        URL url = MainFrame.class.getResource(path);
        if (url == null) return null;
        try {
            BufferedImage src = ImageIO.read(url);
            if (src == null) return null;
            return hdpiIcon(scaleHQ(src, size, size), scaleHQ(src, size * 2, size * 2), size, size);
        } catch (Exception e) { return null; }
    }

    private static ImageIcon hdpiIcon(BufferedImage x1, BufferedImage x2, int logW, int logH) {
        return new ImageIcon(x1) {
            @Override public int getIconWidth()  { return logW; }
            @Override public int getIconHeight() { return logH; }
            @Override public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                AffineTransform tx = g2.getTransform();
                double sx = tx.getScaleX(), sy = tx.getScaleY();
                BufferedImage img = (sx >= 1.5 && x2 != null) ? x2 : x1;
                g2.setTransform(new AffineTransform());
                int px = (int) Math.round(tx.getTranslateX() + x * sx);
                int py = (int) Math.round(tx.getTranslateY() + y * sy);
                int pw = (int) Math.round(logW * sx);
                int ph = (int) Math.round(logH * sy);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(img, px, py, pw, ph, null);
                g2.setTransform(tx);
            }
        };
    }

    private static BufferedImage scaleHQ(BufferedImage src, int targetW, int targetH) {
        int w = src.getWidth(), h = src.getHeight();
        if (w == targetW && h == targetH) return src;
        BufferedImage img = src;
        while (w > targetW * 2 || h > targetH * 2) {
            w = Math.max(targetW, w / 2);
            h = Math.max(targetH, h / 2);
            img = drawScaled(img, w, h);
        }
        return drawScaled(img, targetW, targetH);
    }

    private static BufferedImage drawScaled(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    public static void modernScrollBar(JScrollPane sp) {
        modernScrollBarAxis(sp.getVerticalScrollBar(), false);
    }

    public static void modernHScrollBar(JScrollPane sp) {
        modernScrollBarAxis(sp.getHorizontalScrollBar(), true);
    }

    private static void modernScrollBarAxis(JScrollBar sb, boolean horizontal) {
        if (horizontal) sb.setPreferredSize(new Dimension(0, 6));
        else            sb.setPreferredSize(new Dimension(6, 0));
        sb.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = SCROLL_THUMB;
                trackColor = SURFACE;
            }
            @Override protected JButton createDecreaseButton(int o) { return noBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return noBtn(); }
            private JButton noBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(trackColor); g.fillRect(r.x, r.y, r.width, r.height);
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? SCROLL_THUMB_HV : SCROLL_THUMB);
                if (horizontal) g2.fillRoundRect(r.x + 2, r.y + 1, r.width - 4, r.height - 2, 6, 6);
                else            g2.fillRoundRect(r.x + 1, r.y + 2, r.width - 2, r.height - 4, 6, 6);
                g2.dispose();
            }
        });
    }
}
