package view;

import i18n.I18n;
import util.AppSettings;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class SettingsDialog extends JDialog {

    private static final Color BG      = new Color(0xF0F2F5);
    private static final Color WHITE   = Color.WHITE;
    private static final Color BORDER  = new Color(0xCDD5E0);
    private static final Color ACCENT  = new Color(0x2563EB);
    private static final Color TEXT    = new Color(0x111827);
    private static final Color GRAY    = new Color(0x6B7280);

    private JTextField hostField;
    private JTextField portField;
    private JToggleButton mnBtn;
    private JToggleButton enBtn;

    public SettingsDialog(Frame parent) {
        super(parent, "Тохиргоо / Settings", true);
        setSize(420, 360);
        setLocationRelativeTo(parent);
        setResizable(false);
        setContentPane(buildUI());
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG);

        // ── Language ─────────────────────────────────────────────
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

        content.add(Box.createVerticalStrut(24));

        // ── Server ───────────────────────────────────────────────
        content.add(sectionLabel("Серверийн тохиргоо / Server"));
        content.add(Box.createVerticalStrut(10));

        JPanel serverGrid = new JPanel(new GridLayout(2, 2, 12, 10));
        serverGrid.setBackground(BG);
        serverGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        serverGrid.add(fieldLabel("IP хаяг / Host"));
        serverGrid.add(fieldLabel("Порт / Port"));

        hostField = styledField(AppSettings.getServerHost());
        portField = styledField(String.valueOf(AppSettings.getServerPort()));
        serverGrid.add(hostField);
        serverGrid.add(portField);
        content.add(serverGrid);

        content.add(Box.createVerticalStrut(8));

        JLabel hint = new JLabel("Өөр PC-д сервер ажиллаж байвал тухайн IP оруулна уу");
        hint.setFont(new Font("Dialog", Font.PLAIN, 12));
        hint.setForeground(GRAY);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        content.add(hint);

        // ── Buttons ──────────────────────────────────────────────
        content.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setBackground(BG);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JButton cancelBtn = new JButton("Болих");
        cancelBtn.setFont(new Font("Dialog", Font.PLAIN, 15));
        cancelBtn.setBackground(WHITE);
        cancelBtn.setForeground(GRAY);
        cancelBtn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(10, 0, 10, 0)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton("Хадгалах");
        saveBtn.setFont(new Font("Dialog", Font.BOLD, 15));
        saveBtn.setBackground(ACCENT);
        saveBtn.setForeground(WHITE);
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
        // Server settings
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "IP хаяг хоосон байна", "Алдаа", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            AppSettings.setServerHost(host);
            AppSettings.setServerPort(port);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Порт буруу байна (1–65535)", "Алдаа", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Language — apply live via I18n listener system
        String selectedLang = mnBtn.isSelected() ? "mn" : "en";
        boolean langChanged = !selectedLang.equals(AppSettings.getLanguage());

        AppSettings.save();

        if (langChanged) {
            I18n.setLocale(selectedLang); // notifies all registered LanguageListeners
        }

        dispose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.BOLD, 14));
        l.setForeground(TEXT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Dialog", Font.PLAIN, 13));
        l.setForeground(GRAY);
        return l;
    }

    private JTextField styledField(String value) {
        JTextField f = new JTextField(value);
        f.setFont(new Font("Dialog", Font.PLAIN, 14));
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 10, 8, 10)));
        return f;
    }

    private JToggleButton langToggleBtn(String label, boolean selected) {
        JToggleButton btn = new JToggleButton(label);
        btn.setFont(new Font("Dialog", Font.BOLD, 14));
        btn.setSelected(selected);
        btn.setForeground(selected ? WHITE : GRAY);
        btn.setBackground(selected ? ACCENT : WHITE);
        btn.setBorder(new CompoundBorder(new LineBorder(selected ? ACCENT : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            mnBtn.setForeground(mnBtn.isSelected() ? WHITE : GRAY);
            mnBtn.setBackground(mnBtn.isSelected() ? ACCENT : WHITE);
            mnBtn.setBorder(new CompoundBorder(new LineBorder(mnBtn.isSelected() ? ACCENT : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
            enBtn.setForeground(enBtn.isSelected() ? WHITE : GRAY);
            enBtn.setBackground(enBtn.isSelected() ? ACCENT : WHITE);
            enBtn.setBorder(new CompoundBorder(new LineBorder(enBtn.isSelected() ? ACCENT : BORDER, 1), new EmptyBorder(10, 28, 10, 28)));
        });
        return btn;
    }
}
