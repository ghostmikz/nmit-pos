package view;

import client.SocketClient;
import com.google.gson.JsonObject;
import i18n.I18n;
import i18n.LanguageListener;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoginFrame extends JFrame implements LanguageListener {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BRAND    = new Color(0x9b2a2a);
    private static final Color BRAND_DK = new Color(0x7a1f1f);
    private static final Color INK      = new Color(0x1a1a1a);
    private static final Color INK_2    = new Color(0x4a4a4a);
    private static final Color MUTED    = new Color(0x9a9a9a);
    private static final Color LINE     = new Color(0xececec);
    private static final Color BG_IN    = new Color(0xfafafa);

    private static final int SHADOW_X = 8;
    private static final int SHADOW_Y = 16;

    // ── Icon cache (loaded once) ──────────────────────────────────────────────
    private static final ImageIcon IC_LOGO;
    private static final ImageIcon IC_USER;
    private static final ImageIcon IC_LOCK;
    private static final ImageIcon IC_EYE;

    static {
        IC_LOGO = asset("/assets/logo.png",       32);
        IC_USER = asset("/assets/icons/user.png", 18);
        IC_LOCK = asset("/assets/icons/lock.png", 18);
        IC_EYE  = asset("/assets/icons/eye.png",  18);
    }

    private static ImageIcon asset(String path, int size) {
        URL url = LoginFrame.class.getResource(path);
        if (url == null) return null;
        try {
            Image img = new ImageIcon(url).getImage()
                            .getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) { return null; }
    }

    // ── Instance fields ───────────────────────────────────────────────────────
    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         errorLabel;
    private JButton        loginBtn;

    public LoginFrame() {
        setTitle(I18n.t("app.title"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
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
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(
                        0, 0,                   new Color(0xa83333),
                        getWidth(), getHeight(), BRAND_DK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.BOTH;
        c.weighty = 1.0;

        c.gridx = 0; c.weightx = 1.2;
        root.add(buildLeftPanel(), c);

        c.gridx = 1; c.weightx = 1.0;
        root.add(buildRightPanel(), c);

        return root;
    }

    // ── Left panel ────────────────────────────────────────────────────────────

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(56, 64, 56, 64));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.add(logoComp());
        brand.add(Box.createHorizontalStrut(12));
        JLabel name = new JLabel("MIT");
        name.setFont(new Font("Dialog", Font.BOLD, 26));
        name.setForeground(Color.WHITE);
        brand.add(name);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel headline = new JLabel(
                "<html><div style='width:460px'>" +
                "Ухаалаг жижиглэн худалдааны POS систем</div></html>");
        headline.setFont(new Font("Dialog", Font.BOLD, 44));
        headline.setForeground(Color.WHITE);
        headline.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subline = new JLabel(
                "<html><div style='width:420px'>" +
                "Борлуулалт, бараа материал, ажилтны бүртгэлийг нэг газраас удирдаарай.</div></html>");
        subline.setFont(new Font("Dialog", Font.PLAIN, 16));
        subline.setForeground(new Color(255, 255, 255, 190));
        subline.setAlignmentX(LEFT_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(headline);
        center.add(Box.createVerticalStrut(18));
        center.add(subline);
        center.add(Box.createVerticalGlue());

        left.add(brand,  BorderLayout.NORTH);
        left.add(center, BorderLayout.CENTER);
        return left;
    }

    // ── Right panel / card ────────────────────────────────────────────────────

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(56, 56, 56, 56));
        right.add(buildCard());
        return right;
    }

    private JPanel buildCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth() - SHADOW_X, h = getHeight() - SHADOW_Y;
                for (int i = SHADOW_Y; i > 0; i--) {
                    int alpha = (int)(45.0 * (SHADOW_Y - i + 1) / SHADOW_Y);
                    g2.setColor(new Color(0, 0, 0, alpha));
                    g2.fill(new RoundRectangle2D.Float(
                            SHADOW_X * (1 - (float) i / SHADOW_Y),
                            SHADOW_Y * (1 - (float) i / SHADOW_Y),
                            w, h, 22, 22));
                }
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
                g2.dispose();
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 36, 36 + SHADOW_Y, 36 + SHADOW_X));
        card.setMaximumSize(new Dimension(420 + SHADOW_X, Integer.MAX_VALUE));

        JLabel greet = new JLabel("Тавтай морил");
        greet.setFont(new Font("Dialog", Font.BOLD, 26));
        greet.setForeground(INK);
        greet.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Үргэлжлүүлэхийн тулд бүртгэлээрээ нэвтэрнэ үү");
        sub.setFont(new Font("Dialog", Font.PLAIN, 13));
        sub.setForeground(MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        usernameField = new JTextField();
        passwordField = new JPasswordField();
        passwordField.addActionListener(e -> doLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(0xDC2626));
        errorLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        loginBtn = redButton("Нэвтрэх");
        loginBtn.addActionListener(e -> doLogin());

        card.add(greet);
        card.add(Box.createVerticalStrut(6));
        card.add(sub);
        card.add(Box.createVerticalStrut(26));
        card.add(fieldLabel("Ажилчны код"));
        card.add(Box.createVerticalStrut(8));
        card.add(iconField(usernameField, IC_USER, false));
        card.add(Box.createVerticalStrut(16));
        card.add(fieldLabel("Нууц үг"));
        card.add(Box.createVerticalStrut(8));
        card.add(iconField(passwordField, IC_LOCK, true));
        card.add(Box.createVerticalStrut(4));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(loginBtn);

        return card;
    }

    // ── Component builders ────────────────────────────────────────────────────

    // Brand logo: uses assets/logo.png, falls back to drawn waveform
    private JComponent logoComp() {
        if (IC_LOGO != null) {
            JLabel lbl = new JLabel(IC_LOGO);
            lbl.setPreferredSize(new Dimension(32, 32));
            return lbl;
        }
        return new JComponent() {
            { setPreferredSize(new Dimension(32, 32)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                float s = 32f / 36f;
                int[] sx = {4, 4, 12, 18, 24, 32, 32};
                int[] sy = {30, 8, 18, 10, 18,  8, 30};
                int[] dx = new int[sx.length], dy = new int[sy.length];
                for (int i = 0; i < sx.length; i++) {
                    dx[i] = Math.round(sx[i] * s);
                    dy[i] = Math.round(sy[i] * s);
                }
                g2.drawPolyline(dx, dy, dx.length);
                g2.dispose();
            }
        };
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 13));
        l.setForeground(INK_2);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // Field row: rounded container + leading icon + text field + optional eye icon
    private JPanel iconField(JTextField field, ImageIcon leadIcon, boolean showEye) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_IN);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(LINE);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(380, 48));
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        wrap.setBorder(new EmptyBorder(0, 14, 0, 14));

        wrap.add(iconLabel(leadIcon, false), BorderLayout.WEST);

        field.setOpaque(false);
        field.setBorder(new EmptyBorder(0, 10, 0, 0));
        field.setFont(new Font("Dialog", Font.PLAIN, 15));
        field.setForeground(INK);
        wrap.add(field, BorderLayout.CENTER);

        if (showEye) wrap.add(iconLabel(IC_EYE, true), BorderLayout.EAST);

        return wrap;
    }

    // Returns a fixed-size icon label; if icon is null, falls back to a drawn placeholder
    private JLabel iconLabel(ImageIcon icon, boolean trailingPad) {
        JLabel lbl = new JLabel(icon);   // JLabel with null icon is blank — that's fine
        lbl.setPreferredSize(new Dimension(trailingPad ? 28 : 20, 20));
        if (icon == null) lbl.setPreferredSize(new Dimension(0, 0)); // collapse if no image
        return lbl;
    }

    private JButton redButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? BRAND : new Color(0xc47070));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Dialog", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(380, 50));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Login logic ───────────────────────────────────────────────────────────

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText(I18n.t("login.error.empty"));
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText(I18n.t("common.loading"));

        SwingWorker<JsonObject, Void> worker = new SwingWorker<>() {
            @Override protected JsonObject doInBackground() throws Exception {
                if (!SocketClient.getInstance().isConnected())
                    SocketClient.getInstance().connect();
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                data.put("password", password);
                return SocketClient.getInstance().send("LOGIN", null, data);
            }

            @Override protected void done() {
                loginBtn.setEnabled(true);
                loginBtn.setText("Нэвтрэх");
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        JsonObject d = res.getAsJsonObject("data");
                        User user = new User();
                        user.setId(d.get("id").getAsInt());
                        user.setUsername(d.get("username").getAsString());
                        user.setFullName(d.get("fullName").getAsString());
                        user.setRole(d.get("role").getAsString());
                        user.setToken(d.get("token").getAsString());
                        I18n.removeListener(LoginFrame.this);
                        dispose();
                        new MainFrame(user).setVisible(true);
                    } else {
                        errorLabel.setText(res.get("message").getAsString());
                    }
                } catch (Exception ex) {
                    errorLabel.setText("Серверт холбогдож чадсангүй");
                }
            }
        };
        worker.execute();
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
