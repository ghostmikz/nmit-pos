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
    private static final Color ACCENT  = new Color(0x7a1a1a);
    private static final Color TEXT    = new Color(0x111827);
    private static final Color GRAY    = new Color(0x6B7280);

    private JToggleButton mnBtn;
    private JToggleButton enBtn;

    public SettingsDialog(Frame parent) {
        super(parent, "Тохиргоо / Settings", true);
        setSize(420, 230);
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

        JButton cancelBtn = new JButton(I18n.t("common.cancel"));
        cancelBtn.setFont(new Font("Dialog", Font.PLAIN, 15));
        cancelBtn.setBackground(WHITE);
        cancelBtn.setForeground(GRAY);
        cancelBtn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(10, 0, 10, 0)));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = new JButton(I18n.t("common.save"));
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
