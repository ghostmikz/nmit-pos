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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UsersPanel extends JPanel implements LanguageListener {

    // ── functional interfaces ─────────────────────────────────────────────────
    @FunctionalInterface public interface UserSaver {
        void save(User u, String password, boolean isNew, Runnable onSuccess);
    }
    @FunctionalInterface public interface ActiveToggler {
        void toggle(int userId, boolean active, Runnable onSuccess, Consumer<String> onError);
    }
    @FunctionalInterface public interface UserDeleter {
        void delete(int userId, Runnable onSuccess, Consumer<String> onError);
    }

    // ── palette ───────────────────────────────────────────────────────────────
    private static final Color ACCENT   = new Color(0x7a1a1a);
    private static final Color BG       = new Color(0xF0F2F5);
    private static final Color TXT      = new Color(0x1E293B);
    private static final Color MUTED    = new Color(0x64748B);
    private static final Color SEP      = new Color(0xEEF0F3);
    private static final Color GREEN    = new Color(0x16A34A);
    private static final Color RED_CLR  = new Color(0xDC2626);
    private static final Color GREEN_BG = new Color(0xDCFCE7);
    private static final Color RED_BG   = new Color(0xFEE2E2);

    private static final Color[] AVATAR_COLORS = {
        new Color(0x7C3AED), new Color(0x2563EB), new Color(0x059669),
        new Color(0xD97706), new Color(0xDB2777), new Color(0x0891B2),
        new Color(0x7a1a1a), new Color(0x65A30D)
    };
    private static Color avatarColor(int id) {
        return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length];
    }

    // ── columns ───────────────────────────────────────────────────────────────
    private static final int ROW_H   = 58;
    private static final int COL_UN  = 160;
    private static final int COL_ROL = 110;
    private static final int COL_STS = 110;
    private static final int COL_ACT = 44;   // single kebab button
    private static final int PAD     = 20;

    // ── state ─────────────────────────────────────────────────────────────────
    private final User currentUser;
    private final List<User> allUsers = new ArrayList<>();

    private UserSaver    userSaver;
    private ActiveToggler activeToggler;
    private UserDeleter  userDeleter;
    private String       searchQuery = "";

    // ── UI refs ───────────────────────────────────────────────────────────────
    private JLabel     topTitle;
    private JButton    addUserBtn;
    private JTextField searchField;
    private String     searchPlaceholder = "";
    private JPanel     userListPanel;
    private JLabel     statTotal, statActive, statInactive;
    private JLabel     toastLabel;
    private Timer      toastTimer;

    public UsersPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        setBackground(BG);
        add(buildTopBar(),  BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ── public API ────────────────────────────────────────────────────────────
    public void setUsers(List<User> users) {
        allUsers.clear(); allUsers.addAll(users);
        updateStats(); refreshList();
    }
    public void setUserSaver(UserSaver h)       { userSaver     = h; }
    public void setActiveToggler(ActiveToggler h) { activeToggler = h; }
    public void setUserDeleter(UserDeleter h)   { userDeleter   = h; }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
    }
    public void showToast(String msg) {
        toastLabel.setText(msg);
        toastLabel.setVisible(true);
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(2400, e -> toastLabel.setVisible(false));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    @Override public void onLanguageChanged() {
        topTitle.setText(I18n.t("users.title"));
        addUserBtn.setText(I18n.t("users.add"));
        searchPlaceholder = I18n.t("users.search");
        if (searchField != null && searchQuery.isEmpty()) {
            searchField.setText(searchPlaceholder);
            searchField.setForeground(MUTED);
        }
        updateStats();
        refreshList();
    }

    // ── top bar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE8EAED)),
            new EmptyBorder(16, 24, 16, 24)));

        JPanel left = new JPanel(new BorderLayout(0, 5));
        left.setOpaque(false);

        topTitle = new JLabel(I18n.t("users.title"));
        topTitle.setFont(new Font("Dialog", Font.BOLD, 20));
        topTitle.setForeground(TXT);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.setOpaque(false);
        statTotal    = chip("0", MUTED,    new Color(0xF1F5F9));
        statActive   = chip("0", GREEN,    GREEN_BG);
        statInactive = chip("0", RED_CLR,  RED_BG);
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
        if (statTotal == null) return;
        long active   = allUsers.stream().filter(User::isActive).count();
        long inactive = allUsers.size() - active;
        statTotal   .setText(allUsers.size() + " " + I18n.t("users.stat.total"));
        statActive  .setText(active           + " " + I18n.t("users.stat.active"));
        statInactive.setText(inactive         + " " + I18n.t("users.stat.inactive"));
    }

    private JLabel chip(String text, Color fg, Color bg) {
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
        l.setBackground(bg); l.setForeground(fg);
        l.setFont(new Font("Dialog", Font.BOLD, 11));
        l.setBorder(new EmptyBorder(3, 9, 3, 9));
        return l;
    }

    // ── content ───────────────────────────────────────────────────────────────
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setBorder(new EmptyBorder(8, 12, 8, 12));
        searchWrap.setPreferredSize(new Dimension(300, 40));

        searchPlaceholder = I18n.t("users.search");
        searchField = new JTextField(searchPlaceholder);
        searchField.setForeground(MUTED);
        searchField.setFont(new Font("Dialog", Font.PLAIN, 13));
        searchField.setOpaque(false);
        searchField.setBorder(null);
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchPlaceholder.equals(searchField.getText())) {
                    searchField.setText(""); searchField.setForeground(TXT);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(searchPlaceholder); searchField.setForeground(MUTED);
                }
            }
        });
        searchField.getDocument().addDocumentListener(dl(() -> {
            String t = searchField.getText();
            searchQuery = searchPlaceholder.equals(t) ? "" : t.trim().toLowerCase();
            refreshList();
        }));

        searchWrap.add(new JLabel(view.MainFrame.assetSq("/assets/icons/search.png", 14)), BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);
        filterBar.add(searchWrap, BorderLayout.WEST);

        // Table
        userListPanel = new JPanel();
        userListPanel.setLayout(new BoxLayout(userListPanel, BoxLayout.Y_AXIS));
        userListPanel.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(userListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        view.MainFrame.modernScrollBar(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(0, 24, 16, 24),
            BorderFactory.createLineBorder(new Color(0xE8EAED))));
        tableCard.add(buildColHeader(), BorderLayout.NORTH);
        tableCard.add(scroll,           BorderLayout.CENTER);

        toastLabel = new JLabel("", SwingConstants.CENTER);
        toastLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        toastLabel.setForeground(new Color(0x1E293B));
        toastLabel.setOpaque(true);
        toastLabel.setBackground(new Color(0xF0FDF4));
        toastLabel.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(0, 24, 8, 24),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        toastLabel.setVisible(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        main.add(filterBar,  BorderLayout.NORTH);
        main.add(tableCard,  BorderLayout.CENTER);
        main.add(toastLabel, BorderLayout.SOUTH);
        return main;
    }

    // ── column header ─────────────────────────────────────────────────────────
    private JPanel buildColHeader() {
        JPanel hdr = new JPanel(null) {
            @Override public Dimension getPreferredSize() { return new Dimension(0, 34); }
            @Override public void doLayout() { layoutCells(this); }
        };
        hdr.setBackground(new Color(0xF8F9FA));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE8EAED)));
        hdr.add(colHdr(I18n.t("users.col.name")));
        hdr.add(colHdr(I18n.t("users.col.username")));
        hdr.add(colHdr(I18n.t("users.col.role")));
        hdr.add(colHdr(I18n.t("users.col.status")));
        hdr.add(new JLabel()); // spacer for actions col
        return hdr;
    }

    private JLabel colHdr(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Dialog", Font.BOLD, 10));
        l.setForeground(new Color(0xA0AABA));
        l.setBorder(new EmptyBorder(0, PAD, 0, 0));
        return l;
    }

    // ── list ─────────────────────────────────────────────────────────────────
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
                    JPanel sep = new JPanel();
                    sep.setBackground(SEP);
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

    // ── row ───────────────────────────────────────────────────────────────────
    private JPanel userRow(User u) {
        boolean isSelf  = u.getId() == currentUser.getId();
        boolean active  = u.isActive();

        JPanel row = new JPanel(null) {
            @Override public void doLayout() { layoutCells(this); }
            @Override public Dimension getPreferredSize() { return new Dimension(0, ROW_H); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, ROW_H); }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // avatar
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = 34, ax = PAD, ay = (ROW_H - d) / 2;
                Color av = avatarColor(u.getId());
                g2.setColor(new Color(av.getRed(), av.getGreen(), av.getBlue(), 200));
                g2.fillOval(ax, ay, d, d);
                String ini = initials(u.getFullName());
                g2.setFont(new Font("Dialog", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ini, ax + (d - fm.stringWidth(ini)) / 2, ay + (d + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        row.setBackground(Color.WHITE);
        row.setAlignmentX(LEFT_ALIGNMENT);

        // ── name column (text next to avatar) ────────────────────────────────
        JLabel nameLbl = new JLabel(u.getFullName());
        nameLbl.setFont(new Font("Dialog", Font.BOLD, 13));
        nameLbl.setForeground(TXT);

        JLabel selfLbl = new JLabel("you");
        selfLbl.setFont(new Font("Dialog", Font.PLAIN, 11));
        selfLbl.setForeground(new Color(0xA0AABA));

        JPanel nameCol = new JPanel(null) {
            @Override public void doLayout() {
                int h = getHeight();
                int tx = PAD + 34 + 10;
                if (isSelf) {
                    nameLbl.setBounds(tx, h/2 - 16, getWidth() - tx - 4, 16);
                    selfLbl.setBounds(tx, h/2,      getWidth() - tx - 4, 14);
                } else {
                    nameLbl.setBounds(tx, (h - 16) / 2, getWidth() - tx - 4, 16);
                }
            }
        };
        nameCol.setOpaque(false);
        nameCol.add(nameLbl);
        if (isSelf) nameCol.add(selfLbl);

        // ── username ─────────────────────────────────────────────────────────
        JLabel unLbl = centeredLabel("@" + u.getUsername(), new Font("Dialog", Font.PLAIN, 13), MUTED);

        // ── role ─────────────────────────────────────────────────────────────
        Color roleClr = switch (u.getRole() == null ? "" : u.getRole()) {
            case "admin"   -> new Color(0x7a1a1a);
            case "manager" -> new Color(0x1D4ED8);
            default        -> new Color(0x374151);
        };
        String roleText = switch (u.getRole() == null ? "" : u.getRole()) {
            case "admin"   -> I18n.t("users.role.admin");
            case "manager" -> I18n.t("users.role.manager");
            default        -> I18n.t("users.role.cashier");
        };
        JLabel roleLbl = centeredLabel(roleText, new Font("Dialog", Font.PLAIN, 13), roleClr);

        // ── status ────────────────────────────────────────────────────────────
        JLabel dotLbl = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? GREEN : RED_CLR);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        JLabel badgeLbl = new JLabel(active ? I18n.t("users.active") : I18n.t("users.inactive"));
        badgeLbl.setFont(new Font("Dialog", Font.PLAIN, 13));
        badgeLbl.setForeground(active ? GREEN : RED_CLR);

        JPanel statusCol = new JPanel(null) {
            @Override public void doLayout() {
                int dotD = 7, gap = 6;
                FontMetrics fm = badgeLbl.getFontMetrics(badgeLbl.getFont());
                int textW = fm.stringWidth(badgeLbl.getText());
                int totalW = dotD + gap + textW;
                int cx = PAD + (getWidth() - PAD - totalW) / 2;
                int cy = (getHeight() - dotD) / 2;
                dotLbl.setBounds(cx, cy, dotD, dotD);
                badgeLbl.setBounds(cx + dotD + gap, (getHeight() - fm.getHeight()) / 2, textW + 4, fm.getHeight());
            }
        };
        statusCol.setOpaque(false);
        statusCol.add(dotLbl);
        statusCol.add(badgeLbl);

        // ── kebab menu button ────────────────────────────────────────────────
        JPanel actionsCol = new JPanel(null) {
            @Override public void doLayout() {
                int bw = 28, bh = 28;
                int bx = (getWidth() - bw) / 2;
                int by = (getHeight() - bh) / 2;
                if (getComponentCount() > 0) getComponent(0).setBounds(bx, by, bw, bh);
            }
        };
        actionsCol.setOpaque(false);
        JButton kebab = kebabBtn();
        kebab.addActionListener(e -> showActionsMenu(kebab, u, isSelf, active));
        actionsCol.add(kebab);

        row.add(nameCol);
        row.add(unLbl);
        row.add(roleLbl);
        row.add(statusCol);
        row.add(actionsCol);

        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(0xFAFAFB)); row.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { row.setBackground(Color.WHITE);          row.repaint(); }
        };
        row.addMouseListener(hover);
        for (Component c : row.getComponents()) c.addMouseListener(hover);
        return row;
    }

    /** Common null-layout doLayout for 5-column rows: name | username | role | status | actions */
    private void layoutCells(JPanel p) {
        if (p.getComponentCount() < 5) return;
        int h = p.getHeight(), x = 0;
        int nameW = Math.max(120, p.getWidth() - COL_UN - COL_ROL - COL_STS - COL_ACT - PAD);
        p.getComponent(0).setBounds(x, 0, nameW,   h); x += nameW;
        p.getComponent(1).setBounds(x, 0, COL_UN,  h); x += COL_UN;
        p.getComponent(2).setBounds(x, 0, COL_ROL, h); x += COL_ROL;
        p.getComponent(3).setBounds(x, 0, COL_STS, h); x += COL_STS;
        p.getComponent(4).setBounds(x, 0, COL_ACT + PAD, h);
    }

    /** A JLabel that vertically centers itself — used for plain text cells. */
    private JLabel centeredLabel(String text, Font font, Color fg) {
        JLabel l = new JLabel(text) {
            @Override public void setBounds(int x, int y, int w, int h) {
                FontMetrics fm = getFontMetrics(getFont());
                int lh = fm.getHeight();
                super.setBounds(x + PAD, y + (h - lh) / 2, w - PAD, lh);
            }
        };
        l.setFont(font);
        l.setForeground(fg);
        return l;
    }

    // ── context menu ─────────────────────────────────────────────────────────
    private void showActionsMenu(Component anchor, User u, boolean isSelf, boolean active) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE8EAED), 1),
            new EmptyBorder(4, 0, 4, 0)));

        JMenuItem editItem = menuItem(I18n.t("users.ctx.edit"), TXT);
        editItem.addActionListener(e -> openUserDialog(u));
        menu.add(editItem);

        if (!isSelf) {
            menu.addSeparator();
            String toggleLabel = active ? I18n.t("users.ctx.deactivate") : I18n.t("users.ctx.activate");
            JMenuItem toggleItem = menuItem(toggleLabel, TXT);
            toggleItem.addActionListener(e -> confirmToggle(u));
            menu.add(toggleItem);

            menu.addSeparator();
            JMenuItem deleteItem = menuItem(I18n.t("users.ctx.delete"), RED_CLR);
            deleteItem.addActionListener(e -> confirmDelete(u));
            menu.add(deleteItem);
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    private JMenuItem menuItem(String text, Color fg) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Dialog", Font.PLAIN, 13));
        item.setForeground(fg);
        item.setBorder(new EmptyBorder(6, 16, 6, 40));
        item.setBackground(Color.WHITE);
        item.setOpaque(true);
        return item;
    }

    // ── dialogs ───────────────────────────────────────────────────────────────
    private void openUserDialog(User edit) {
        boolean isNew = edit == null;
        JDialog dlg = makeDialog(isNew ? I18n.t("users.form.add") : I18n.t("users.form.edit"));

        JTextField  fName = styledField(isNew ? "" : edit.getFullName());
        JTextField  fUser = styledField(isNew ? "" : edit.getUsername());
        fUser.setEditable(isNew);
        if (!isNew) { fUser.setForeground(MUTED); }

        JPasswordField fPass = styledPasswordField();

        String[] roleKeys   = {"admin", "manager", "cashier"};
        String[] roleLabels = {I18n.t("users.role.admin"), I18n.t("users.role.manager"), I18n.t("users.role.cashier")};
        JComboBox<String> fRole = new JComboBox<>(roleLabels);
        fRole.setFont(new Font("Dialog", Font.PLAIN, 13));
        if (!isNew && edit.getRole() != null)
            for (int i = 0; i < roleKeys.length; i++)
                if (roleKeys[i].equals(edit.getRole())) { fRole.setSelectedIndex(i); break; }

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 16, 24));
        body.add(formField(I18n.t("users.form.name"),     fName)); body.add(Box.createVerticalStrut(12));
        body.add(formField(I18n.t("users.form.username"), fUser)); body.add(Box.createVerticalStrut(12));
        body.add(formField(I18n.t("users.form.password"), fPass));
        if (!isNew) {
            body.add(Box.createVerticalStrut(4));
            JLabel hint = new JLabel(I18n.t("users.form.password.hint"));
            hint.setFont(new Font("Dialog", Font.PLAIN, 10));
            hint.setForeground(new Color(0xA0AABA));
            hint.setAlignmentX(LEFT_ALIGNMENT);
            body.add(hint);
        }
        body.add(Box.createVerticalStrut(12));
        body.add(formField(I18n.t("users.form.role"), fRole));

        JButton cancel = ghostBtn(I18n.t("common.cancel"));
        cancel.addActionListener(e -> dlg.dispose());
        JButton save   = accentBtn(I18n.t("common.save"));
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

        JScrollPane bodyScroll = new JScrollPane(body,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        bodyScroll.setBorder(null);
        dlg.add(bodyScroll,              BorderLayout.CENTER);
        dlg.add(dialogFooter(cancel, save), BorderLayout.SOUTH);
        dlg.setPreferredSize(new Dimension(420, 420));
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

    private void confirmDelete(User u) {
        int ok = JOptionPane.showConfirmDialog(this,
            MessageFormat.format(I18n.t("users.confirm.delete"), u.getFullName()),
            I18n.t("common.confirm"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION && userDeleter != null)
            userDeleter.delete(u.getId(),
                () -> showToast(I18n.t("users.toast.deleted")),
                this::showError);
    }

    // ── dialog helpers ────────────────────────────────────────────────────────
    private JDialog makeDialog(String title) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title,
            java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE8EAED)),
            new EmptyBorder(16, 24, 14, 16)));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Dialog", Font.BOLD, 15));
        t.setForeground(TXT);
        JButton x = new JButton("×");
        x.setFont(new Font("Dialog", Font.PLAIN, 18));
        x.setForeground(MUTED);
        x.setContentAreaFilled(false); x.setBorderPainted(false); x.setFocusPainted(false);
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.addActionListener(e -> dlg.dispose());
        hdr.add(t, BorderLayout.WEST); hdr.add(x, BorderLayout.EAST);
        dlg.add(hdr, BorderLayout.NORTH);
        return dlg;
    }

    private JPanel dialogFooter(JButton cancel, JButton save) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE8EAED)));
        p.add(cancel); p.add(save);
        return p;
    }

    private JPanel formField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Dialog", Font.BOLD, 10));
        l.setForeground(new Color(0xA0AABA));
        p.add(l, BorderLayout.NORTH); p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ── buttons ───────────────────────────────────────────────────────────────
    private JButton accentBtn(String label) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT.darker() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 20, 9, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton ghostBtn(String label) {
        JButton b = new JButton(label);
        b.setFont(new Font("Dialog", Font.PLAIN, 13));
        b.setForeground(TXT);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0)),
            new EmptyBorder(8, 18, 8, 18)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton kebabBtn() {
        JButton b = new JButton("···") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        b.setFont(new Font("Dialog", Font.BOLD, 14));
        b.setForeground(MUTED);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JTextField styledField(String val) {
        JTextField f = new JTextField(val) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(9, 11, 9, 11));
        return f;
    }

    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        f.setFont(new Font("Dialog", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(9, 11, 9, 11));
        return f;
    }

    // ── utilities ─────────────────────────────────────────────────────────────
    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
        return (p[0].charAt(0) + "" + p[p.length - 1].charAt(0)).toUpperCase();
    }

    private static DocumentListener dl(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }
}
