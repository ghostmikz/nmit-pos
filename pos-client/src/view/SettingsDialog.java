package view;

import static view.AppColors.*;

import i18n.I18n;
import util.AppSettings;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class SettingsDialog extends JDialog {

    private static final Color WHITE  = Color.WHITE;
    private static final Color BORDER = new Color(0xCDD5E0);
    private static final Color TEXT   = new Color(0x111827);
    private static final Color GRAY   = new Color(0x6B7280);

    private JToggleButton mnBtn;
    private JToggleButton enBtn;

    public SettingsDialog(Frame parent) {
        super(parent, true);
        setUndecorated(true);
        setSize(420, 270);
        setLocationRelativeTo(parent);
        setResizable(false);
        getRootPane().setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(SIDEBAR);
        titleBar.setPreferredSize(new Dimension(0, 40));

        JLabel titleLbl = new JLabel("Тохиргоо / Settings");
        titleLbl.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setBorder(new EmptyBorder(0, 16, 0, 0));

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
        x.addActionListener(e -> dispose());

        final int[] drag = {0, 0};
        titleBar.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { drag[0] = e.getX(); drag[1] = e.getY(); }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point p = getLocation();
                setLocation(p.x + e.getX() - drag[0], p.y + e.getY() - drag[1]);
            }
        });

        JPanel xWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        xWrap.setOpaque(false);
        xWrap.add(x);
        titleBar.add(titleLbl, BorderLayout.CENTER);
        titleBar.add(xWrap,    BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(WHITE);
        root.add(titleBar,  BorderLayout.NORTH);
        root.add(buildUI(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);

        content.add(sectionLabel("Хэл / Language"));
        content.add(Box.createVerticalStrut(10));

        JPanel langRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        langRow.setBackground(BG);

        ButtonGroup bg = new ButtonGroup();
        mnBtn = langToggleBtn("Монгол", AppSettings.getLanguage().equals("mn"));
        enBtn = langToggleBtn("English", AppSettings.getLanguage().equals("en"));
        bg.add(mnBtn); bg.add(enBtn);
        langRow.add(mnBtn); langRow.add(enBtn);
        content.add(langRow);

        content.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setBackground(BG);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JButton cancelBtn = new JButton(I18n.t("common.cancel")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? new Color(0xF1F5F9) : WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        cancelBtn.setFont(new Font("Dialog", Font.PLAIN, 15));
        cancelBtn.setForeground(GRAY);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(I18n.t("common.save")) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? ACCENT.darker() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        saveBtn.setFont(new Font("Dialog", Font.BOLD, 15));
        saveBtn.setBackground(ACCENT);
        saveBtn.setForeground(WHITE);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> save());

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        content.add(btnRow);

        root.add(content, BorderLayout.CENTER);
        return root;
    }

    private void save() {
        String selectedLang = mnBtn.isSelected() ? "mn" : "en";
        boolean langChanged = !selectedLang.equals(AppSettings.getLanguage());
        AppSettings.setLanguage(selectedLang);
        AppSettings.save();
        if (langChanged) {
            I18n.setLocale(selectedLang);
        }
        dispose();
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 14));
        l.setForeground(TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JToggleButton langToggleBtn(String label, boolean selected) {
        JToggleButton btn = new JToggleButton(label) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(isSelected() ? SIDEBAR : WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFont(new Font("Dialog", Font.BOLD, 14));
        btn.setSelected(selected);
        btn.setForeground(selected ? WHITE : GRAY);
        btn.setBorder(new CompoundBorder(new LineBorder(selected ? SIDEBAR : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            mnBtn.setForeground(mnBtn.isSelected() ? WHITE : GRAY);
            mnBtn.setBorder(new CompoundBorder(new LineBorder(mnBtn.isSelected() ? SIDEBAR : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
            enBtn.setForeground(enBtn.isSelected() ? WHITE : GRAY);
            enBtn.setBorder(new CompoundBorder(new LineBorder(enBtn.isSelected() ? SIDEBAR : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
            mnBtn.repaint();
            enBtn.repaint();
        });
        return btn;
    }
}
