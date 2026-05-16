package view;

import client.SocketClient;
import com.google.gson.JsonObject;
import i18n.I18n;
import i18n.LanguageListener;
import model.User;
import util.AppSettings;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LoginFrame extends JFrame implements LanguageListener {

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         errorLabel;
    private JButton        loginBtn;
    private JLabel         titleLabel;
    private JLabel         subtitleLabel;
    private JLabel         userLabel;
    private JLabel         passLabel;

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
        titleLabel.setText(I18n.t("login.title"));
        userLabel.setText(I18n.t("login.username"));
        passLabel.setText(I18n.t("login.password"));
        loginBtn.setText(I18n.t("login.button"));
        errorLabel.setText(" ");
        refreshSubtitle();
    }

    private void refreshSubtitle() {
        boolean mn = "mn".equals(AppSettings.getLanguage());
        subtitleLabel.setText(mn
                ? "Системд нэвтрэхийн тулд мэдээллээ оруулна уу"
                : "Enter your credentials to access the system");
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildLeftPanel(),  BorderLayout.WEST);
        root.add(buildRightPanel(), BorderLayout.CENTER);
        return root;
    }

    // Dark branded panel — left 40 %
    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(
                        0, 0,          new Color(0x1E293B),
                        0, getHeight(), new Color(0x0F172A)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        left.setPreferredSize(new Dimension(460, 0));
        left.setBackground(new Color(0x1E293B));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        // Blue accent block behind "POS"
        JPanel logoBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoBlock.setOpaque(false);
        JLabel logoLbl = new JLabel("  POS  ");
        logoLbl.setFont(new Font("Dialog", Font.BOLD, 16));
        logoLbl.setForeground(Color.WHITE);
        logoLbl.setOpaque(true);
        logoLbl.setBackground(new Color(0x2563EB));
        logoLbl.setBorder(new EmptyBorder(8, 4, 8, 4));
        logoBlock.add(logoLbl);
        logoBlock.setAlignmentX(LEFT_ALIGNMENT);

        JLabel appName = new JLabel("NMIT");
        appName.setFont(new Font("Dialog", Font.BOLD, 58));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tagLine = new JLabel("Кассын удирдлагын систем");
        tagLine.setFont(new Font("Dialog", Font.PLAIN, 20));
        tagLine.setForeground(new Color(0x94A3B8));
        tagLine.setAlignmentX(LEFT_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x334155));
        sep.setMaximumSize(new Dimension(320, 1));
        sep.setAlignmentX(LEFT_ALIGNMENT);

        JLabel versionLbl = new JLabel("v1.0  —  2025");
        versionLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        versionLbl.setForeground(new Color(0x475569));
        versionLbl.setAlignmentX(LEFT_ALIGNMENT);

        inner.add(logoBlock);
        inner.add(Box.createVerticalStrut(20));
        inner.add(appName);
        inner.add(Box.createVerticalStrut(10));
        inner.add(tagLine);
        inner.add(Box.createVerticalStrut(36));
        inner.add(sep);
        inner.add(Box.createVerticalStrut(20));
        inner.add(versionLbl);

        left.add(inner);
        return left;
    }

    // Light form panel — right 60 %
    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(new Color(0xF8FAFC));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setMaximumSize(new Dimension(460, Integer.MAX_VALUE));
        form.setPreferredSize(new Dimension(460, 600));

        // Top row: title + settings gear
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        titleLabel = new JLabel(I18n.t("login.title"));
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 38));
        titleLabel.setForeground(new Color(0x0F172A));

        JButton settingsBtn = iconBtn("⚙");
        settingsBtn.setToolTipText("Тохиргоо / Settings");
        settingsBtn.addActionListener(e -> new SettingsDialog(this).setVisible(true));

        topRow.add(titleLabel,  BorderLayout.WEST);
        topRow.add(settingsBtn, BorderLayout.EAST);

        subtitleLabel = new JLabel();
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 15));
        subtitleLabel.setForeground(new Color(0x64748B));
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        refreshSubtitle();

        userLabel = fieldLabel(I18n.t("login.username"));
        passLabel = fieldLabel(I18n.t("login.password"));

        usernameField = styledTextField();
        passwordField = new JPasswordField();
        styleField(passwordField);
        passwordField.addActionListener(e -> doLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(0xDC2626));
        errorLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        loginBtn = new JButton(I18n.t("login.button"));
        loginBtn.setFont(new Font("Dialog", Font.BOLD, 18));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(new Color(0x2563EB));
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setPreferredSize(new Dimension(460, 56));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> doLogin());

        form.add(topRow);
        form.add(Box.createVerticalStrut(6));
        form.add(subtitleLabel);
        form.add(Box.createVerticalStrut(48));
        form.add(userLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(24));
        form.add(passLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(8));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(28));
        form.add(loginBtn);

        right.add(form);
        return right;
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
                loginBtn.setText(I18n.t("login.button"));
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 15));
        l.setForeground(new Color(0x374151));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JTextField styledTextField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Dialog", Font.PLAIN, 17));
        f.setPreferredSize(new Dimension(460, 52));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        f.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xCDD5E0), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
    }

    private JButton iconBtn(String icon) {
        JButton b = new JButton(icon);
        b.setFont(new Font("Dialog", Font.PLAIN, 24));
        b.setForeground(new Color(0x6B7280));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Font antialiasing — fixes blurry/thin text under XWayland and HiDPI displays
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
