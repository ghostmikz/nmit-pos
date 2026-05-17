package view;

import i18n.I18n;
import i18n.LanguageListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.net.URL;

public class LoginFrame extends JFrame implements LanguageListener {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BRAND    = new Color(0x9b2a2a);
    private static final Color BRAND_DK = new Color(0x7a1f1f);
    private static final Color GRAD_TOP = new Color(0xa83333);
    private static final Color INK      = new Color(0x1a1a1a);
    private static final Color INK_2    = new Color(0x4a4a4a);
    private static final Color MUTED    = new Color(0x9a9a9a);
    private static final Color LINE     = new Color(0xececec);
    private static final Color BG_IN    = new Color(0xfafafa);

    // ── Icons loaded once ─────────────────────────────────────────────────────
    private static final ImageIcon IC_LOGO;
    private static final ImageIcon IC_USER;
    private static final ImageIcon IC_LOCK;
    private static final ImageIcon IC_EYE;
    private static final ImageIcon IC_EYE_OFF;

    static {
        IC_LOGO    = assetH ("/assets/logo.png",          44);
        IC_USER    = assetSq("/assets/icons/user.png",    18);
        IC_LOCK    = assetSq("/assets/icons/lock.png",    18);
        IC_EYE     = assetSq("/assets/icons/eye.png",     18);
        IC_EYE_OFF = assetSq("/assets/icons/eye-off.png", 18);
    }

    private static ImageIcon assetH(String path, int targetH) {
        URL url = LoginFrame.class.getResource(path);
        if (url == null) return null;
        try {
            BufferedImage src = ImageIO.read(url);
            if (src == null) return null;
            int targetW = Math.max(1, src.getWidth() * targetH / src.getHeight());
            return hdpiIcon(scaleHQ(src, targetW, targetH), scaleHQ(src, targetW * 2, targetH * 2), targetW, targetH);
        } catch (Exception e) { return null; }
    }

    private static ImageIcon assetSq(String path, int size) {
        URL url = LoginFrame.class.getResource(path);
        if (url == null) return null;
        try {
            BufferedImage src = ImageIO.read(url);
            if (src == null) return null;
            return hdpiIcon(scaleHQ(src, size, size), scaleHQ(src, size * 2, size * 2), size, size);
        } catch (Exception e) { return null; }
    }

    // Reads the real screen scale from the Graphics2D transform at paint time
    // and draws at physical pixel coordinates — avoids relying on BaseMultiResolutionImage
    // selection which can fail on JBR/Wayland for small icons.
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

    @FunctionalInterface
    public interface LoginListener {
        void onLogin(String username, String password);
    }

    // ── Fields ────────────────────────────────────────────────────────────────
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         errorLabel;
    private JButton        loginBtn;
    private LoginListener  loginListener;

    public LoginFrame() {
        setTitle(I18n.t("app.title"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setBackground(BRAND_DK);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setContentPane(buildUI());
        I18n.addListener(this);
    }

    @Override
    public void onLanguageChanged() {
        setTitle(I18n.t("app.title"));
        errorLabel.setText(" ");
    }

    // ── Root ──────────────────────────────────────────────────────────────────

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            private BufferedImage cache;
            @Override protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                    cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2 = cache.createGraphics();
                    g2.setPaint(new GradientPaint(0, 0, GRAD_TOP, w, h, BRAND_DK));
                    g2.fillRect(0, 0, w, h);
                    g2.dispose();
                }
                g.drawImage(cache, 0, 0, null);
            }
        };
        root.setBackground(BRAND_DK);
        root.add(buildTopBar(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; c.weighty = 1.0;
        c.gridx = 0; c.weightx = 1.2; columns.add(buildLeftPanel(), c);
        c.gridx = 1; c.weightx = 1.0; columns.add(buildRightPanel(), c);
        root.add(columns, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bar.setOpaque(false);
        bar.add(winBtn("−", e -> setState(Frame.ICONIFIED)));
        bar.add(winBtn("×", e -> dispatchEvent(
                new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING))));
        return bar;
    }

    private JButton winBtn(String label, java.awt.event.ActionListener action) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Dialog", Font.PLAIN, 16));
        btn.setForeground(new Color(255, 255, 255, 160));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(new Color(255, 255, 255, 160));
            }
        });
        return btn;
    }

    // ── Left panel ────────────────────────────────────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(56, 64, 56, 64));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(logoComp());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel headline = new JLabel(
                "<html><div style='width:460px'>Ухаалаг жижиглэн худалдааны POS систем</div></html>");
        headline.setFont(new Font("Dialog", Font.BOLD, 44));
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subline = new JLabel(
                "<html><div style='width:420px'>Борлуулалт, бараа материал, ажилтны бүртгэлийг нэг газраас удирдаарай.</div></html>");
        subline.setFont(new Font("Dialog", Font.PLAIN, 16));
        subline.setForeground(new Color(255, 255, 255, 190));
        subline.setAlignmentX(LEFT_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(headline);
        center.add(Box.createVerticalStrut(18));
        center.add(subline);
        center.add(Box.createVerticalGlue());

        left.add(brand, BorderLayout.NORTH);
        left.add(center, BorderLayout.CENTER);
        return left;
    }

    private JComponent logoComp() {
        if (IC_LOGO != null) return new JLabel(IC_LOGO);
        // Fallback: drawn waveform + "MIT" text
        JPanel fb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fb.setOpaque(false);
        fb.add(new JComponent() {
            { setPreferredSize(new Dimension(32, 32)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float s = 32f / 36f;
                int[] sx = {4, 4, 12, 18, 24, 32, 32}, sy = {30, 8, 18, 10, 18, 8, 30};
                int[] dx = new int[sx.length], dy = new int[sy.length];
                for (int i = 0; i < sx.length; i++) { dx[i] = Math.round(sx[i]*s); dy[i] = Math.round(sy[i]*s); }
                g2.drawPolyline(dx, dy, dx.length);
                g2.dispose();
            }
        });
        fb.add(Box.createHorizontalStrut(10));
        JLabel txt = new JLabel("MIT");
        txt.setFont(new Font("Dialog", Font.BOLD, 26));
        txt.setForeground(Color.WHITE);
        fb.add(txt);
        return fb;
    }

    // ── Right panel ───────────────────────────────────────────────────────────

    private JPanel buildRightPanel() {
        // GridBagLayout fill=NONE: card gets exactly its preferred size, centered.
        // Field preferred widths are 360px (not Short.MAX_VALUE), so the card's
        // computed preferred size is ~432×390px — correct and stable.
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(48, 48, 48, 48));
        right.add(buildCard(), new GridBagConstraints());
        return right;
    }

    // ── Card ──────────────────────────────────────────────────────────────────

    private JPanel buildCard() {
        // White rounded rect cached as BufferedImage — during window move this is
        // a single drawImage call instead of a Graphics2D anti-aliased fill.
        JPanel card = new JPanel() {
            private BufferedImage cache;
            @Override protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                    cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cache.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
                    g2.dispose();
                }
                g.drawImage(cache, 0, 0, null);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 36, 36, 36));

        JLabel greet = new JLabel("Тавтай морил");
        greet.setFont(new Font("Dialog", Font.BOLD, 26));
        greet.setForeground(INK);
        greet.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Үргэлжлүүлэхийн тулд бүртгэлээрээ нэвтэрнэ үү");
        sub.setFont(new Font("Dialog", Font.PLAIN, 13));
        sub.setForeground(MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        usernameField = new JTextField();
        usernameField.addActionListener(e -> fireLogin());
        passwordField = new JPasswordField();
        passwordField.addActionListener(e -> fireLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(0xDC2626));
        errorLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        loginBtn = redButton("Нэвтрэх");
        loginBtn.addActionListener(e -> fireLogin());

        card.add(greet);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(26));
        card.add(fieldLabel("Ажилтны код"));
        card.add(Box.createVerticalStrut(8));
        card.add(inputRow(usernameField, IC_USER, false));
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Нууц үг"));
        card.add(Box.createVerticalStrut(8));
        card.add(inputRow(passwordField, IC_LOCK, true));
        card.add(Box.createVerticalStrut(4));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(loginBtn);

        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 13));
        l.setForeground(INK_2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel inputRow(JTextField field, ImageIcon lead, boolean showEye) {
        // Background cached — fixed 360×48, only redraws if size changes
        JPanel wrap = new JPanel(new BorderLayout()) {
            private BufferedImage cache;
            @Override protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                    cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cache.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BG_IN);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                    g2.setColor(LINE);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10));
                    g2.dispose();
                }
                g.drawImage(cache, 0, 0, null);
            }
        };
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(360, 48));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setBorder(new EmptyBorder(0, 14, 0, 14));

        if (lead != null) {
            JLabel icon = new JLabel(lead);
            icon.setBorder(new EmptyBorder(0, 0, 0, 8));
            wrap.add(icon, BorderLayout.WEST);
        }

        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, 2, 0, 0));
        field.setFont(new Font("Dialog", Font.PLAIN, 15));
        field.setForeground(INK);
        wrap.add(field, BorderLayout.CENTER);

        if (showEye && IC_EYE != null) {
            JLabel eye = new JLabel(IC_EYE);
            eye.setBorder(new EmptyBorder(0, 8, 0, 0));
            eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            JPasswordField pf = (JPasswordField) field;
            eye.addMouseListener(new java.awt.event.MouseAdapter() {
                private boolean shown = false;
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    shown = !shown;
                    pf.setEchoChar(shown ? (char) 0 : '•');
                    eye.setIcon(shown ? IC_EYE_OFF : IC_EYE);
                }
            });
            wrap.add(eye, BorderLayout.EAST);
        }
        return wrap;
    }

    private JButton redButton(String text) {
        JButton btn = new JButton(text) {
            private BufferedImage cache;
            @Override protected void paintComponent(Graphics g) {
                int w = getWidth(), h = getHeight();
                boolean en = isEnabled();
                if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
                    cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cache.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(en ? BRAND : new Color(0xc47070));
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
                    g2.dispose();
                }
                g.drawImage(cache, 0, 0, null);
                super.paintComponent(g);
            }
            @Override public void setEnabled(boolean b) {
                super.setEnabled(b);
                cache = null;
                repaint();
            }
        };
        btn.setFont(new Font("Dialog", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(360, 50));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Public view API (called by LoginController) ───────────────────────────

    public void setLoginListener(LoginListener listener) {
        this.loginListener = listener;
    }

    public void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? I18n.t("common.loading") : "Нэвтрэх");
    }

    public void showError(String msg) {
        errorLabel.setText(msg);
    }

    public void close() {
        I18n.removeListener(this);
        dispose();
    }

    private void fireLogin() {
        if (loginListener != null)
            loginListener.onLogin(usernameField.getText().trim(),
                                  new String(passwordField.getPassword()));
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            LoginFrame frame = new LoginFrame();
            new controller.LoginController(frame);
            frame.setVisible(true);
        });
    }
}
