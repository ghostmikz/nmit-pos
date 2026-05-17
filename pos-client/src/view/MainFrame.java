package view;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;
import view.panels.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements LanguageListener {

    private final User user;
    private JPanel     contentArea;
    private CardLayout cardLayout;

    // Sidebar nav buttons — kept so we can relabel them on language change
    private JButton posBtn, inventoryBtn, reportsBtn, dashboardBtn, usersBtn, settingsBtn, logoutBtn;
    private JLabel  roleLabel;

    private final List<LanguageListener> panelListeners = new ArrayList<>();
    private Runnable logoutListener;

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
        roleLabel.setText("  " + roleName(user.getRole()));
        posBtn.setText("  " + I18n.t("nav.pos"));
        if (inventoryBtn != null) inventoryBtn.setText("  " + I18n.t("nav.inventory"));
        if (reportsBtn   != null) reportsBtn.setText("  "   + I18n.t("nav.reports"));
        if (dashboardBtn != null) dashboardBtn.setText("  " + I18n.t("nav.dashboard"));
        if (usersBtn     != null) usersBtn.setText("  "     + I18n.t("nav.users"));
        settingsBtn.setText("  " + I18n.t("nav.settings"));
        logoutBtn.setText("  "   + I18n.t("nav.logout"));
        // Notify panels
        for (LanguageListener l : panelListeners) l.onLanguageChanged();
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(0x1E293B));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(24, 0, 24, 0));

        JLabel logo = new JLabel("  NMIT-POS");
        logo.setFont(new Font("Dialog", Font.BOLD, 22));
        logo.setForeground(new Color(0x2563EB));
        logo.setBorder(new EmptyBorder(0, 20, 20, 20));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(logo);

        roleLabel = new JLabel("  " + roleName(user.getRole()));
        roleLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        roleLabel.setForeground(new Color(0x94A3B8));
        roleLabel.setBorder(new EmptyBorder(0, 20, 14, 20));
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(roleLabel);

        sidebar.add(separator());
        sidebar.add(Box.createVerticalStrut(12));

        // Nav buttons
        posBtn = navBtn("  " + I18n.t("nav.pos"), true);
        posBtn.addActionListener(e -> switchPanel("POS", posBtn, sidebar));
        sidebar.add(posBtn);

        if (user.isManager()) {
            inventoryBtn = navBtn("  " + I18n.t("nav.inventory"), false);
            inventoryBtn.addActionListener(e -> switchPanel("INVENTORY", inventoryBtn, sidebar));
            sidebar.add(inventoryBtn);

            reportsBtn = navBtn("  " + I18n.t("nav.reports"), false);
            reportsBtn.addActionListener(e -> switchPanel("REPORTS", reportsBtn, sidebar));
            sidebar.add(reportsBtn);

            dashboardBtn = navBtn("  " + I18n.t("nav.dashboard"), false);
            dashboardBtn.addActionListener(e -> switchPanel("DASHBOARD", dashboardBtn, sidebar));
            sidebar.add(dashboardBtn);
        }

        if (user.isAdmin()) {
            usersBtn = navBtn("  " + I18n.t("nav.users"), false);
            usersBtn.addActionListener(e -> switchPanel("USERS", usersBtn, sidebar));
            sidebar.add(usersBtn);
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(separator());

        // Settings
        settingsBtn = navBtn("  " + I18n.t("nav.settings"), false);
        settingsBtn.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        sidebar.add(settingsBtn);

        // User name
        JLabel userLbl = new JLabel("  " + user.getFullName());
        userLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        userLbl.setForeground(new Color(0xCBD5E1));
        userLbl.setBorder(new EmptyBorder(10, 20, 4, 20));
        userLbl.setAlignmentX(LEFT_ALIGNMENT);
        sidebar.add(userLbl);

        // Logout
        logoutBtn = navBtn("  " + I18n.t("nav.logout"), false);
        logoutBtn.setForeground(new Color(0xF87171));
        logoutBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    I18n.t("nav.logout") + "?", I18n.t("common.confirm"),
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION && logoutListener != null)
                logoutListener.run();
        });
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JPanel buildContent() {
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        return contentArea;
    }

    // ── Public view API (called by MainController) ────────────────────────────

    public void addPanel(JPanel panel, String key) {
        contentArea.add(panel, key);
        if (panel instanceof LanguageListener ll) panelListeners.add(ll);
    }

    public void showPanel(String key) {
        cardLayout.show(contentArea, key);
    }

    public void setLogoutListener(Runnable listener) {
        this.logoutListener = listener;
    }

    public void close() {
        I18n.removeListener(this);
        dispose();
    }

    private void switchPanel(String key, JButton active, JPanel sidebar) {
        cardLayout.show(contentArea, key);
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton b) {
                boolean sel = b == active;
                b.setBackground(sel ? new Color(0x2563EB) : new Color(0x1E293B));
                if (!b.getForeground().equals(new Color(0xF87171)))
                    b.setForeground(sel ? Color.WHITE : new Color(0x94A3B8));
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JButton navBtn(String label, boolean selected) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Dialog", Font.PLAIN, 15));
        btn.setForeground(selected ? Color.WHITE : new Color(0x94A3B8));
        btn.setBackground(selected ? new Color(0x2563EB) : new Color(0x1E293B));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 46));
        btn.setPreferredSize(new Dimension(220, 46));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x334155));
        sep.setMaximumSize(new Dimension(220, 1));
        return sep;
    }

    private String roleName(String role) {
        return switch (role) {
            case "admin"   -> I18n.t("users.role.admin");
            case "manager" -> I18n.t("users.role.manager");
            default        -> I18n.t("users.role.cashier");
        };
    }
}
