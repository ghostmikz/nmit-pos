package view.panels;

import i18n.I18n;
import i18n.LanguageListener;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UsersPanel extends JPanel implements LanguageListener {

    @FunctionalInterface public interface UserSaver {
        void save(User u, String password, boolean isNew, Runnable onSuccess);
    }
    @FunctionalInterface public interface ActiveToggler {
        void toggle(int userId, boolean active, Runnable onSuccess, Consumer<String> onError);
    }

    private static final Color ACCENT   = new Color(0x7a1a1a);
    private static final Color BG       = new Color(0xF0F2F5);
    private static final Color TXT      = new Color(0x1E293B);
    private static final Color MUTED    = new Color(0x64748B);
    private static final Color SEP      = new Color(0xF1F5F9);
    private static final Color GREEN    = new Color(0x16A34A);
    private static final Color GREEN_BG = new Color(0xDCFCE7);
    private static final Color RED_CLR  = new Color(0xDC2626);
    private static final Color RED_BG   = new Color(0xFEE2E2);

    // Avatar colors cycling per user
    private static final Color[] AVATAR_COLORS = {
        new Color(0x7C3AED), new Color(0x2563EB), new Color(0x059669),
        new Color(0xD97706), new Color(0xDB2777), new Color(0x0891B2),
        new Color(0x7a1a1a), new Color(0x65A30D)
    };

    private static Color roleColor(String role) {
        return switch (role == null ? "" : role) {
            case "admin"   -> new Color(0xB92B2B);
            case "manager" -> new Color(0x2563EB);
            default        -> new Color(0x059669);
        };
    }

    private static Color avatarColor(int id) {
        return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length];
    }

    // Column layout — first col includes avatar+name together
    private static final int ROW_H    = 64;
    private static final int AVATAR_W = 64;   // space reserved for avatar within name col
    private static final int COL_UN   = 170;
    private static final int COL_ROL  = 110;
    private static final int COL_STS  = 115;
    private static final int COL_ACT  = 170;
    private static final int ROW_PAD  = 20;

    private final User currentUser;
    private final List<User> allUsers = new ArrayList<>();

    private UserSaver     userSaver;
    private ActiveToggler activeToggler;
    private String        searchQuery = "";

    private JLabel     topTitle;
    private JButton    addUserBtn;
    private JTextField searchField;
    private String     searchPlaceholder = "";
    private JPanel     userListPanel;
    private JLabel     statTotal, statActive, statInactive;
    private JLabel     toastLabel;
    private javax.swing.Timer toastTimer;

    public UsersPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    public void setUsers(List<User> users) {
        allUsers.clear();
        allUsers.addAll(users);
        updateStats();
        refreshList();
    }

    public void setUserSaver(UserSaver h)         { userSaver      = h; }
    public void setActiveToggler(ActiveToggler h)  { activeToggler  = h; }

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
        topTitle.setText(I18n.t("users.title"));
        addUserBtn.setText(I18n.t("users.add"));
        searchPlaceholder = I18n.t("users.search");
        if (searchField != null && searchQuery.isEmpty()) {
            searchField.setText(searchPlaceholder);
            searchField.setForeground(new Color(0x64748B));
        }
        refreshList();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)),
            new EmptyBorder(16, 24, 16, 24)));

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setOpaque(false);
        topTitle = new JLabel(I18n.t("users.title"));
        topTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        topTitle.setForeground(TXT);

        // Stat chips: Total · Active · Inactive
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        statTotal    = statChip("0 total",    new Color(0x64748B), new Color(0xF1F5F9));
        statActive   = statChip("0 active",   GREEN,               GREEN_BG);
        statInactive = statChip("0 inactive", RED_CLR,             RED_BG);
        chips.add(statTotal); chips.add(statActive); chips.add(statInactive);

        left.add(topTitle, BorderLayout.NORTH);
        left.add(chips,    BorderLayout.CENTER);

        addUserBtn = accentBtn(I18n.t("users.add"));
        addUserBtn.addActionListener(e -> openUserDialog(null));

        bar.add(left,       BorderLayout.WEST);
        bar.add(addUserBtn, BorderLayout.EAST);
        return bar;
    }

    private void updateStats() {
        long active   = allUsers.stream().filter(User::isActive).count();
        long inactive = allUsers.size() - active;
        statTotal   .setText(allUsers.size() + " " + I18n.t("users.stat.total"));
        statActive  .setText(active           + " " + I18n.t("users.stat.active"));
        statInactive.setText(inactive         + " " + I18n.t("users.stat.inactive"));
    }

    private JLabel statChip(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        l.setForeground(fg);
        l.setBackground(bg);
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        return l;
    }

    // ── Content ───────────────────────────────────────────────────────────────
    private JPanel buildContent() {
        // Search bar
        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setBackground(BG);
        filterBar.setBorder(new EmptyBorder(14, 24, 10, 24));

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(9, 14, 9, 14));
        searchWrap.setPreferredSize(new Dimension(320, 42));
        searchPlaceholder = I18n.t("users.search");
        searchField = new JTextField(searchPlaceholder);
        searchField.setForeground(MUTED);
        searchField.setFont(new Font("Dialog", Font.PLAIN, 13));
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
            refreshList();
        }));
        searchWrap.add(new JLabel(view.MainFrame.assetSq("/assets/icons/search.png", 15)), BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);
        filterBar.add(searchWrap, BorderLayout.WEST);

        // Table card
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(userListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        view.MainFrame.modernScrollBar(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(20);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(0, 24, 0, 24),
            BorderFactory.createLineBorder(new Color(0xE2E8F0))));
        tableCard.add(buildColHeader(), BorderLayout.NORTH);
        tableCard.add(scroll,           BorderLayout.CENTER);

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        toastLabel.setForeground(Color.WHITE);
        toastLabel.setOpaque(true);
        toastLabel.setBackground(new Color(0x1E293B));
        toastLabel.setBorder(new EmptyBorder(10, 20, 10, 20));
        toastLabel.setVisible(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        main.add(filterBar,  BorderLayout.NORTH);
        main.add(tableCard,  BorderLayout.CENTER);
        main.add(toastLabel, BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildColHeader() {
        JPanel hdr = new JPanel(null) {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 36); }
            @Override public void doLayout() { layoutRow(this); }
        };
        hdr.setBackground(new Color(0xF8FAFC));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)));
        hdr.add(colHdr(I18n.t("users.col.name")));
        hdr.add(colHdr(I18n.t("users.col.username")));
        hdr.add(colHdr(I18n.t("users.col.role")));
        hdr.add(colHdr(I18n.t("users.col.status")));
        hdr.add(new JLabel());
        return hdr;
    }

    private JLabel colHdr(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Dialog", Font.BOLD, 10));
        l.setForeground(new Color(0x94A3B8));
        l.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        return l;
    }

    // ── List ──────────────────────────────────────────────────────────────────
    private void refreshList() {
        List<User> filtered = getFiltered();
        userListPanel.removeAll();
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel(I18n.t("users.empty"), SwingConstants.CENTER);
            empty.setFont(new Font("Dialog", Font.PLAIN, 14));
            empty.setForeground(MUTED);
            empty.setAlignmentX(CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(60, 0, 60, 0));
            userListPanel.add(empty);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                userListPanel.add(userRow(filtered.get(i)));
                if (i < filtered.size() - 1) {
                    JPanel sep = new JPanel(); sep.setBackground(SEP);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep.setAlignmentX(LEFT_ALIGNMENT);
                    userListPanel.add(sep);
                }
            }
        }
        userListPanel.revalidate();
        userListPanel.repaint();
    }

    private List<User> getFiltered() {
        List<User> out = new ArrayList<>();
        for (User u : allUsers) {
            if (!searchQuery.isEmpty()
                && !u.getFullName().toLowerCase().contains(searchQuery)
                && !u.getUsername().toLowerCase().contains(searchQuery)) continue;
            out.add(u);
        }
        return out;
    }

    // ── User row ──────────────────────────────────────────────────────────────
    private JPanel userRow(User u) {
        JPanel row = new JPanel(null) {
            @Override public void doLayout() { layoutRow(this); }
            @Override public Dimension getPreferredSize() { return new Dimension(0, ROW_H); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, ROW_H); }
        };
        row.setBackground(Color.WHITE);
        row.setAlignmentX(LEFT_ALIGNMENT);
        boolean isSelf = u.getId() == currentUser.getId();

        // Name col: avatar circle + name + (you) tag
        JPanel namePanel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw avatar circle
                int d = 36, x = ROW_PAD, y = (getHeight() - d) / 2;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color av = avatarColor(u.getId());
                g2.setColor(new Color(av.getRed(), av.getGreen(), av.getBlue(), 220));
                g2.fillOval(x, y, d, d);
                // Initials
                String initials = initials(u.getFullName());
                g2.setFont(new Font("Dialog", Font.BOLD, 13));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, x + (d - fm.stringWidth(initials)) / 2, y + (d + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        namePanel.setOpaque(false);
        int txtX = ROW_PAD + 36 + 10;
        JLabel nameLbl = new JLabel(u.getFullName());
        nameLbl.setFont(new Font("Dialog", Font.BOLD, 13));
        nameLbl.setForeground(TXT);
        nameLbl.setBounds(txtX, 12, 220, 18);
        namePanel.add(nameLbl);
        if (isSelf) {
            JLabel selfLbl = new JLabel("you");
            selfLbl.setFont(new Font("Dialog", Font.PLAIN, 10));
            selfLbl.setForeground(new Color(0x94A3B8));
            selfLbl.setBounds(txtX, 31, 40, 14);
            namePanel.add(selfLbl);
        }

        // Username
        JPanel unWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        unWrap.setOpaque(false);
        unWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        JLabel unLbl = new JLabel("@" + u.getUsername());
        unLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        unLbl.setForeground(MUTED);
        unWrap.add(unLbl);

        // Role badge
        JPanel roleWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        roleWrap.setOpaque(false);
        roleWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        Color rc = roleColor(u.getRole());
        String roleText = switch (u.getRole() == null ? "" : u.getRole()) {
            case "admin"   -> I18n.t("users.role.admin");
            case "manager" -> I18n.t("users.role.manager");
            default        -> I18n.t("users.role.cashier");
        };
        JLabel roleBadge = new JLabel(roleText) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        roleBadge.setForeground(rc);
        roleBadge.setFont(new Font("Dialog", Font.BOLD, 11));
        roleBadge.setBorder(new EmptyBorder(3, 9, 3, 9));
        roleWrap.add(roleBadge);

        // Status badge
        JPanel statusWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusWrap.setOpaque(false);
        statusWrap.setBorder(new EmptyBorder(0, ROW_PAD, 0, 0));
        boolean active = u.isActive();
        Color stFg = active ? GREEN : RED_CLR;
        Color stBg = active ? GREEN_BG : RED_BG;
        JLabel badge = new JLabel(active ? I18n.t("users.active") : I18n.t("users.inactive")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(stBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        badge.setForeground(stFg);
        badge.setFont(new Font("Dialog", Font.BOLD, 11));
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        // Active dot indicator
        JPanel dotBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dotBadge.setOpaque(false);
        JLabel dot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? GREEN : RED_CLR);
                g2.fillOval(0, (getHeight()-8)/2, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(8, 8));
        dotBadge.add(dot); dotBadge.add(badge);
        statusWrap.add(dotBadge);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        actions.setOpaque(false);
        JButton editBtn = outlineBtn(I18n.t("users.form.edit"), ACCENT);
        editBtn.addActionListener(e -> openUserDialog(u));
        actions.add(editBtn);
        if (!isSelf) {
            JButton toggleBtn = outlineBtn(
                active ? I18n.t("users.inactive") : I18n.t("users.active"),
                active ? RED_CLR : GREEN);
            toggleBtn.addActionListener(e -> confirmToggle(u));
            actions.add(toggleBtn);
        }

        row.add(namePanel); row.add(unWrap); row.add(roleWrap); row.add(statusWrap); row.add(actions);

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(0xFDF9F9)); row.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE);         row.repaint(); }
        };
        row.addMouseListener(hover);
        for (Component c : row.getComponents()) c.addMouseListener(hover);
        return row;
    }

    private void layoutRow(JPanel p) {
        Component[] cs = p.getComponents();
        if (cs.length < 5) return;
        int h = p.getHeight(), x = 0;
        int nameW = Math.max(120, p.getWidth() - COL_UN - COL_ROL - COL_STS - COL_ACT - ROW_PAD);
        cs[0].setBounds(x, 0, nameW,   h); x += nameW;
        cs[1].setBounds(x, 0, COL_UN,  h); x += COL_UN;
        cs[2].setBounds(x, 0, COL_ROL, h); x += COL_ROL;
        cs[3].setBounds(x, 0, COL_STS, h); x += COL_STS;
        cs[4].setBounds(x, 0, COL_ACT, h);
    }

    private static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    private void openUserDialog(User edit) {
        boolean isNew = edit == null;
        JDialog dlg = makeDialog(isNew ? I18n.t("users.form.add") : I18n.t("users.form.edit"));

        JTextField  fName = styledField(isNew ? "" : edit.getFullName());
        JTextField  fUser = styledField(isNew ? "" : edit.getUsername());
        fUser.setEditable(isNew);
        if (!isNew) { fUser.setForeground(MUTED); fUser.setBackground(new Color(0xF8FAFC)); }

        JPasswordField fPass = styledPasswordField();

        String[] roleKeys   = {"admin", "manager", "cashier"};
        String[] roleLabels = {I18n.t("users.role.admin"), I18n.t("users.role.manager"), I18n.t("users.role.cashier")};
        JComboBox<String> fRole = new JComboBox<>(roleLabels);
        fRole.setFont(new Font("Dialog", Font.PLAIN, 13));
        if (!isNew && edit.getRole() != null)
            for (int i = 0; i < roleKeys.length; i++) if (roleKeys[i].equals(edit.getRole())) { fRole.setSelectedIndex(i); break; }

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 16, 24));
        body.add(formField(I18n.t("users.form.name"),     fName)); body.add(vs(10));
        body.add(formField(I18n.t("users.form.username"), fUser)); body.add(vs(10));
        body.add(formField(I18n.t("users.form.password"), fPass));
        if (!isNew) {
            body.add(vs(3));
            JLabel hint = new JLabel(I18n.t("users.form.password.hint"));
            hint.setFont(new Font("Dialog", Font.PLAIN, 10));
            hint.setForeground(new Color(0x94A3B8));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            body.add(hint);
        }
        body.add(vs(10));
        body.add(formField(I18n.t("users.form.role"), fRole));

        JButton cancel = ghostBtn(I18n.t("common.cancel"));
        cancel.addActionListener(e -> dlg.dispose());
        JButton save = accentBtn(I18n.t("common.save"));
        save.addActionListener(e -> {
            String name  = fName.getText().trim();
            String uname = fUser.getText().trim();
            String pass  = new String(fPass.getPassword()).trim();
            if (name.isEmpty()) { fName.requestFocus(); return; }
            if (isNew && (uname.isEmpty() || pass.isEmpty())) return;
            User u = new User();
            if (!isNew) u.setId(edit.getId());
            u.setFullName(name);
            u.setUsername(isNew ? uname : edit.getUsername());
            u.setRole(roleKeys[fRole.getSelectedIndex()]);
            if (userSaver != null) {
                dlg.dispose();
                userSaver.save(u, pass, isNew,
                    () -> showToast(isNew ? I18n.t("users.toast.added") : I18n.t("users.toast.updated")));
            }
        });

        JScrollPane bodyScroll = new JScrollPane(body, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setBorder(null);
        dlg.add(bodyScroll,             BorderLayout.CENTER);
        dlg.add(dialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setPreferredSize(new Dimension(440, 430));
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void confirmToggle(User u) {
        boolean nowActive = u.isActive();
        int ok = JOptionPane.showConfirmDialog(this,
            MessageFormat.format(I18n.t(nowActive ? "users.confirm.deactivate" : "users.confirm.activate"), u.getFullName()),
            I18n.t("common.confirm"), JOptionPane.YES_NO_OPTION,
            nowActive ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        if (ok == JOptionPane.YES_OPTION && activeToggler != null)
            activeToggler.toggle(u.getId(), !nowActive,
                () -> showToast(I18n.t(!nowActive ? "users.toast.activated" : "users.toast.deactivated")),
                this::showError);
    }

    // ── Dialog helpers ────────────────────────────────────────────────────────
    private JDialog makeDialog(String title) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0)),
            new EmptyBorder(18, 24, 16, 16)));
        JLabel t = new JLabel(title); t.setFont(new Font("Dialog", Font.BOLD, 15)); t.setForeground(TXT);
        JButton x = new JButton("x"); x.setFont(new Font("Dialog", Font.PLAIN, 18)); x.setForeground(MUTED);
        x.setContentAreaFilled(false); x.setBorderPainted(false); x.setFocusPainted(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.addActionListener(e -> dlg.dispose());
        hdr.add(t, BorderLayout.WEST); hdr.add(x, BorderLayout.EAST);
        dlg.add(hdr, BorderLayout.NORTH);
        return dlg;
    }

    private JPanel dialogFooter(JButton cancel, JButton save) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 14));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)));
        p.add(cancel); p.add(save);
        return p;
    }

    private JTextField styledField(String val) {
        JTextField f = new JTextField(val) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(9, 12, 9, 12));
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(9, 12, 9, 12));
        return f;
    }

    private JPanel formField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Dialog", Font.BOLD, 10));
        l.setForeground(new Color(0x94A3B8));
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────
    private JButton accentBtn(String label) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBorderPainted(false); b.setFocusPainted(false); b.setContentAreaFilled(false);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton ghostBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Dialog", Font.PLAIN, 13));
        b.setBackground(Color.WHITE); b.setForeground(TXT);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0)),
            new EmptyBorder(9, 18, 9, 18)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton outlineBtn(String label, Color color) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 11));
        b.setForeground(color);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60)),
            new EmptyBorder(4, 10, 4, 10)));
        b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static Component vs(int h) { return Box.createVerticalStrut(h); }

    private static DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { r.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }
}
