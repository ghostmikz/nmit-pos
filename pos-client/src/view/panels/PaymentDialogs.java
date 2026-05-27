package view.panels;

import i18n.I18n;
import model.CartItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.awt.Component.LEFT_ALIGNMENT;
import static view.AppColors.*;

class PaymentDialogs {

    private static final Font FONT_BTN = new Font("Dialog", Font.BOLD, 14);

    private final Component    parent;
    private final NumberFormat mnt;

    private POSPanel.PaymentHandler   paymentHandler;
    private List<Map<String, Object>> paymentMethods = new ArrayList<>();

    PaymentDialogs(Component parent, NumberFormat mnt) {
        this.parent = parent;
        this.mnt    = mnt;
    }

    void setPaymentHandler(POSPanel.PaymentHandler h)           { paymentHandler = h; }
    void setPaymentMethods(List<Map<String, Object>> methods)   { paymentMethods = methods != null ? methods : new ArrayList<>(); }

    void showPaymentDialog(int methodId, BigDecimal total, List<CartItem> snap, Runnable onSuccess) {
        if (methodId == 0)      showSplitDialog(total, snap, onSuccess);
        else if (methodId == 1) showCashDialog(total, snap, onSuccess);
        else                    showDigitalDialog(methodNameFor(methodId), methodId, total, snap, onSuccess);
    }

    // ── Cash ─────────────────────────────────────────────────────────────────

    private void showCashDialog(BigDecimal total, List<CartItem> snap, Runnable onSuccess) {
        JDialog dlg = makeDialog(I18n.t("pos.cash.dialog.title"));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(24, 28, 28, 28));

        body.add(dialogLabel(I18n.t("pos.total"), 13, MUTED));
        JLabel totalAmt = new JLabel("₮" + mnt.format(total));
        totalAmt.setFont(new Font("Dialog", Font.BOLD, 28));
        totalAmt.setForeground(TXT);
        totalAmt.setAlignmentX(LEFT_ALIGNMENT);
        body.add(totalAmt);
        body.add(Box.createVerticalStrut(20));

        body.add(dialogLabel(I18n.t("pos.cash.given"), 13, MUTED));
        body.add(Box.createVerticalStrut(6));
        JTextField cashInput = new JTextField();
        cashInput.setFont(new Font("Dialog", Font.BOLD, 18));
        cashInput.setForeground(TXT);
        cashInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                new EmptyBorder(8, 12, 8, 12)));
        cashInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        cashInput.setAlignmentX(LEFT_ALIGNMENT);
        body.add(cashInput);
        body.add(Box.createVerticalStrut(16));

        body.add(dialogLabel(I18n.t("pos.cash.change"), 13, MUTED));
        JLabel changeAmt = new JLabel("₮0");
        changeAmt.setFont(new Font("Dialog", Font.BOLD, 22));
        changeAmt.setForeground(new Color(0x16A34A));
        changeAmt.setAlignmentX(LEFT_ALIGNMENT);
        body.add(changeAmt);
        body.add(Box.createVerticalStrut(24));

        JButton confirm = dialogBtn(I18n.t("pos.confirm"), new Color(0x16A34A));
        confirm.setEnabled(false);
        body.add(confirm);

        cashInput.getDocument().addDocumentListener(new DocumentListener() {
            void update() {
                try {
                    BigDecimal cash   = new BigDecimal(cashInput.getText().replaceAll("[^0-9.]", ""));
                    BigDecimal change = cash.subtract(total);
                    if (change.compareTo(BigDecimal.ZERO) >= 0) {
                        changeAmt.setText("₮" + mnt.format(change));
                        changeAmt.setForeground(new Color(0x16A34A));
                        confirm.setEnabled(true);
                    } else {
                        changeAmt.setText(I18n.t("pos.cash.short"));
                        changeAmt.setForeground(new Color(0xEF4444));
                        confirm.setEnabled(false);
                    }
                } catch (Exception ex) {
                    changeAmt.setText("₮0");
                    changeAmt.setForeground(new Color(0x16A34A));
                    confirm.setEnabled(false);
                }
            }
            @Override public void insertUpdate(DocumentEvent e)  { update(); }
            @Override public void removeUpdate(DocumentEvent e)  { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;
            List<Map<String, Object>> payments = List.of(Map.of("paymentMethodId", 1, "amount", total));
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(340, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── Digital ───────────────────────────────────────────────────────────────

    private void showDigitalDialog(String methodName, int methodId, BigDecimal total,
                                   List<CartItem> snap, Runnable onSuccess) {
        JDialog dlg = makeDialog(methodName);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(28, 32, 32, 32));

        JLabel title = new JLabel(methodName);
        title.setFont(new Font("Dialog", Font.BOLD, 18));
        title.setForeground(TXT);
        title.setAlignmentX(LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(8));

        JLabel amt = new JLabel("₮" + mnt.format(total));
        amt.setFont(new Font("Dialog", Font.BOLD, 30));
        amt.setForeground(ACCENT);
        amt.setAlignmentX(LEFT_ALIGNMENT);
        body.add(amt);
        body.add(Box.createVerticalStrut(28));

        JButton confirm = dialogBtn(I18n.t("pos.confirm"), new Color(0x2563EB));
        body.add(confirm);
        body.add(Box.createVerticalStrut(8));

        JButton cancel = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        cancel.setForeground(MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        body.add(cancel);

        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;
            List<Map<String, Object>> payments = List.of(Map.of("paymentMethodId", methodId, "amount", total));
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(320, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── Split ─────────────────────────────────────────────────────────────────

    private void showSplitDialog(BigDecimal total, List<CartItem> snap, Runnable onSuccess) {
        JDialog dlg = makeDialog(I18n.t("pos.payment.split"));

        List<Map<String, Object>> methods = paymentMethods.isEmpty()
                ? List.of(Map.of("id", 1, "name", I18n.t("pos.payment.cash")))
                : paymentMethods;
        String[] allMethodNames = methods.stream().map(m -> (String) m.get("name")).toArray(String[]::new);
        int[]    allMethodIds   = methods.stream().mapToInt(m -> ((Number) m.get("id")).intValue()).toArray();

        List<JComboBox<String>> methodCombos = new ArrayList<>();
        List<JTextField>        amountFields = new ArrayList<>();

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 24, 24, 24));

        JLabel totalLbl = new JLabel(I18n.t("pos.total") + ": ₮" + mnt.format(total));
        totalLbl.setFont(new Font("Dialog", Font.BOLD, 16));
        totalLbl.setForeground(TXT);
        totalLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(totalLbl);
        body.add(Box.createVerticalStrut(16));

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBackground(Color.WHITE);
        rowsPanel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(rowsPanel);
        body.add(Box.createVerticalStrut(12));

        Runnable addRow = () -> {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(Color.WHITE);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

            JComboBox<String> combo = new JComboBox<>(allMethodNames);
            combo.setFont(new Font("Dialog", Font.PLAIN, 13));
            combo.setMaximumSize(new Dimension(150, 36));
            methodCombos.add(combo);

            JTextField amtField = new JTextField();
            amtField.setFont(new Font("Dialog", Font.PLAIN, 13));
            amtField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            amtField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                    new EmptyBorder(4, 8, 4, 8)));
            amountFields.add(amtField);

            row.add(combo);
            row.add(Box.createHorizontalStrut(8));
            row.add(amtField);
            rowsPanel.add(row);
            rowsPanel.add(Box.createVerticalStrut(6));
            rowsPanel.revalidate();
            rowsPanel.repaint();
        };

        addRow.run();
        addRow.run();

        JButton addRowBtn = new JButton("+ " + I18n.t("pos.split.add_method"));
        addRowBtn.setFont(new Font("Dialog", Font.PLAIN, 12));
        addRowBtn.setForeground(ACCENT);
        addRowBtn.setContentAreaFilled(false);
        addRowBtn.setBorderPainted(false);
        addRowBtn.setFocusPainted(false);
        addRowBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addRowBtn.setAlignmentX(LEFT_ALIGNMENT);
        addRowBtn.addActionListener(e -> addRow.run());
        body.add(addRowBtn);
        body.add(Box.createVerticalStrut(16));

        JButton confirm = dialogBtn(I18n.t("pos.confirm"), ACCENT);
        JButton cancel  = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        cancel.setForeground(MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        body.add(confirm);
        body.add(Box.createVerticalStrut(8));
        body.add(cancel);

        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;
            List<Map<String, Object>> payments = new ArrayList<>();
            for (int i = 0; i < methodCombos.size(); i++) {
                BigDecimal amt;
                try { amt = new BigDecimal(amountFields.get(i).getText().replaceAll("[^0-9.]", "")); }
                catch (Exception ex) { continue; }
                if (amt.compareTo(BigDecimal.ZERO) > 0) {
                    int mid = allMethodIds[methodCombos.get(i).getSelectedIndex()];
                    if (mid != 1) {
                        String mName = methodNameFor(mid);
                        int ok = JOptionPane.showConfirmDialog(dlg,
                                MessageFormat.format(I18n.t("pos.terminal.confirm"), mName, mnt.format(amt)),
                                MessageFormat.format(I18n.t("pos.terminal.title"), mName),
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.INFORMATION_MESSAGE);
                        if (ok != JOptionPane.OK_OPTION) return;
                    }
                    Map<String, Object> p = new HashMap<>();
                    p.put("paymentMethodId", mid);
                    p.put("amount", amt);
                    payments.add(p);
                }
            }
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(380, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String methodNameFor(int methodId) {
        for (Map<String, Object> m : paymentMethods) {
            if (((Number) m.get("id")).intValue() == methodId)
                return (String) m.get("name");
        }
        return "Method " + methodId;
    }

    private JDialog makeDialog(String title) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);
        dlg.setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());
        dlg.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(0xCDD5E0)));
        dlg.getRootPane().setDefaultButton(null);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR);
        header.setPreferredSize(new Dimension(0, 40));

        JLabel titleLbl = new JLabel(title);
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
        x.addActionListener(e -> dlg.dispose());

        final int[] drag = {0, 0};
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { drag[0] = e.getX(); drag[1] = e.getY(); }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                Point p = dlg.getLocation();
                dlg.setLocation(p.x + e.getX() - drag[0], p.y + e.getY() - drag[1]);
            }
        });

        JPanel xWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        xWrap.setOpaque(false);
        xWrap.add(x);
        header.add(titleLbl, BorderLayout.CENTER);
        header.add(xWrap,    BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);
        return dlg;
    }

    private JLabel dialogLabel(String text, int size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Dialog", Font.PLAIN, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton dialogBtn(String label, Color bg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? getBackground().darker() : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(FONT_BTN);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }
}
