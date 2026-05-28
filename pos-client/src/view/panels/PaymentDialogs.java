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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.awt.Component.CENTER_ALIGNMENT;
import static java.awt.Component.LEFT_ALIGNMENT;
import static view.AppColors.*;

class PaymentDialogs {

    private static final Font FONT_BTN  = new Font("Dialog", Font.BOLD, 14);
    private static final int  MAX_SPLIT = 4;

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
        if (methodId == 0)               showSplitDialog(total, snap, onSuccess);
        else if (methodId == 1)          showCashDialog(total, snap, onSuccess);
        else if (isCardMethod(methodId)) showTerminalDialog(methodNameFor(methodId), methodId, total, snap, onSuccess);
        else                             showQrDialog(methodNameFor(methodId), methodId, total, snap, onSuccess);
    }

    private boolean isCardMethod(int methodId) {
        String name = methodNameFor(methodId).toLowerCase();
        return name.contains("карт") || name.contains("card") || name.contains("visa")
            || name.contains("master") || name.contains("terminal");
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
            List<Map<String, Object>> payments = List.of(Map.of("payment_method_id", 1, "amount", total));
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(340, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── Terminal (card) ───────────────────────────────────────────────────────

    private void showTerminalDialog(String methodName, int methodId, BigDecimal total,
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
        body.add(Box.createVerticalStrut(12));

        JLabel hint = new JLabel(MessageFormat.format(I18n.t("pos.terminal.hint"), methodName));
        hint.setFont(new Font("Dialog", Font.PLAIN, 13));
        hint.setForeground(MUTED);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        body.add(hint);
        body.add(Box.createVerticalStrut(24));

        JButton confirm = dialogBtn(I18n.t("pos.terminal.done"), new Color(0x2563EB));
        body.add(confirm);
        body.add(Box.createVerticalStrut(8));

        JButton cancel = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        cancel.setForeground(MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        body.add(cancel);

        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;
            List<Map<String, Object>> payments = List.of(Map.of("payment_method_id", methodId, "amount", total));
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(320, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── QR payment ────────────────────────────────────────────────────────────

    private void showQrDialog(String methodName, int methodId, BigDecimal total,
                               List<CartItem> snap, Runnable onSuccess) {
        JDialog dlg = makeDialog(methodName);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(24, 32, 28, 32));

        JLabel amtLbl = new JLabel("₮" + mnt.format(total), SwingConstants.CENTER);
        amtLbl.setFont(new Font("Dialog", Font.BOLD, 28));
        amtLbl.setForeground(TXT);
        amtLbl.setAlignmentX(CENTER_ALIGNMENT);
        amtLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        body.add(amtLbl);
        body.add(Box.createVerticalStrut(16));

        // QR image centered
        ImageIcon qrIcon = view.MainFrame.assetSq("/assets/qr.png", 160);
        JLabel qrLabel = new JLabel(qrIcon, SwingConstants.CENTER);
        qrLabel.setAlignmentX(CENTER_ALIGNMENT);
        qrLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        body.add(qrLabel);
        body.add(Box.createVerticalStrut(14));

        JLabel hint = new JLabel(I18n.t("pos.qr.hint"), SwingConstants.CENTER);
        hint.setFont(new Font("Dialog", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        body.add(hint);
        body.add(Box.createVerticalStrut(20));

        JButton confirm = dialogBtn(I18n.t("pos.qr.done"), new Color(0x16A34A));
        JButton cancel  = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        cancel.setForeground(MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;
            List<Map<String, Object>> payments = List.of(Map.of("payment_method_id", methodId, "amount", total));
            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });
        JPanel confirmRow = centeredBtn(confirm);
        JPanel cancelRow  = centeredBtn(cancel);
        body.add(confirmRow);
        body.add(Box.createVerticalStrut(8));
        body.add(cancelRow);

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(280, dlg.getPreferredSize().height));
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

        // Total line
        JLabel totalLbl = new JLabel(I18n.t("pos.total") + ": ₮" + mnt.format(total));
        totalLbl.setFont(new Font("Dialog", Font.BOLD, 16));
        totalLbl.setForeground(TXT);
        totalLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(totalLbl);
        body.add(Box.createVerticalStrut(6));

        // Remaining tracker
        JLabel remainingLbl = new JLabel(I18n.t("pos.split.remaining") + ": ₮" + mnt.format(total));
        remainingLbl.setFont(new Font("Dialog", Font.BOLD, 13));
        remainingLbl.setForeground(ACCENT);
        remainingLbl.setAlignmentX(LEFT_ALIGNMENT);
        body.add(remainingLbl);
        body.add(Box.createVerticalStrut(14));

        JPanel rowsPanel = new JPanel();
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setBackground(Color.WHITE);
        rowsPanel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(rowsPanel);
        body.add(Box.createVerticalStrut(10));

        JButton addRowBtn = new JButton("+ " + I18n.t("pos.split.add_method"));
        addRowBtn.setFont(new Font("Dialog", Font.PLAIN, 12));
        addRowBtn.setForeground(ACCENT);
        addRowBtn.setContentAreaFilled(false);
        addRowBtn.setBorderPainted(false);
        addRowBtn.setFocusPainted(false);
        addRowBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addRowBtn.setAlignmentX(LEFT_ALIGNMENT);
        body.add(addRowBtn);
        body.add(Box.createVerticalStrut(16));

        JButton confirm = dialogBtn(I18n.t("pos.confirm"), ACCENT);
        JButton cancel  = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        cancel.setForeground(MUTED);
        cancel.addActionListener(e -> dlg.dispose());
        confirm.setEnabled(false);
        body.add(confirm);
        body.add(Box.createVerticalStrut(8));
        body.add(cancel);

        // Shared update logic
        Runnable updateRemaining = () -> {
            BigDecimal entered = BigDecimal.ZERO;
            for (JTextField tf : amountFields) {
                try { entered = entered.add(new BigDecimal(tf.getText().replaceAll("[^0-9.]", ""))); }
                catch (Exception ignored) {}
            }
            BigDecimal rem = total.subtract(entered);
            boolean exact = rem.compareTo(BigDecimal.ZERO) == 0;
            remainingLbl.setText(I18n.t("pos.split.remaining") + ": ₮" + mnt.format(rem));
            remainingLbl.setForeground(exact ? new Color(0x16A34A) : (rem.signum() < 0 ? new Color(0xEF4444) : ACCENT));
            confirm.setEnabled(exact);
        };

        DocumentListener amtListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { updateRemaining.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { updateRemaining.run(); }
            @Override public void changedUpdate(DocumentEvent e) { updateRemaining.run(); }
        };

        Runnable addRow = () -> {
            if (methodCombos.size() >= MAX_SPLIT) return;

            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setBackground(Color.WHITE);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            JComboBox<String> combo = new JComboBox<>(allMethodNames);
            combo.setFont(new Font("Dialog", Font.PLAIN, 12));
            combo.setMaximumSize(new Dimension(140, 34));
            methodCombos.add(combo);

            JTextField amtField = new JTextField();
            amtField.setFont(new Font("Dialog", Font.PLAIN, 13));
            amtField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            amtField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                    new EmptyBorder(4, 8, 4, 8)));
            amtField.getDocument().addDocumentListener(amtListener);
            amountFields.add(amtField);

            // Fill button — fills this field with the current remaining amount
            JButton fillBtn = new JButton("Fill") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getModel().isRollover() ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 30) : new Color(0,0,0,0));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
                @Override public boolean isOpaque() { return false; }
            };
            fillBtn.setFont(new Font("Dialog", Font.PLAIN, 11));
            fillBtn.setForeground(ACCENT);
            fillBtn.setPreferredSize(new Dimension(36, 34));
            fillBtn.setMaximumSize(new Dimension(36, 34));
            fillBtn.setContentAreaFilled(false);
            fillBtn.setBorderPainted(false);
            fillBtn.setFocusPainted(false);
            fillBtn.setRolloverEnabled(true);
            fillBtn.getModel().addChangeListener(ev -> fillBtn.repaint());
            fillBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            fillBtn.addActionListener(ev -> {
                BigDecimal entered = BigDecimal.ZERO;
                for (JTextField tf : amountFields) {
                    if (tf == amtField) continue;
                    try { entered = entered.add(new BigDecimal(tf.getText().replaceAll("[^0-9.]", ""))); }
                    catch (Exception ignored) {}
                }
                BigDecimal rem = total.subtract(entered);
                if (rem.signum() > 0) amtField.setText(rem.toPlainString());
            });

            // Remove button — only enabled when there are more than 1 row
            JButton removeBtn = new JButton("x") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color rc = new Color(0xEF4444);
                    g2.setColor(getModel().isRollover() ? new Color(rc.getRed(), rc.getGreen(), rc.getBlue(), 30) : new Color(0,0,0,0));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(getModel().isRollover() ? rc : MUTED);
                    g2.setFont(new Font("Dialog", Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = "x";
                    g2.drawString(s, (getWidth()-fm.stringWidth(s))/2, (getHeight()+fm.getAscent()-fm.getDescent())/2);
                    g2.dispose();
                }
                @Override public boolean isOpaque() { return false; }
            };
            removeBtn.setPreferredSize(new Dimension(26, 34));
            removeBtn.setMaximumSize(new Dimension(26, 34));
            removeBtn.setContentAreaFilled(false);
            removeBtn.setBorderPainted(false);
            removeBtn.setFocusPainted(false);
            removeBtn.setRolloverEnabled(true);
            removeBtn.getModel().addChangeListener(ev -> removeBtn.repaint());
            removeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            removeBtn.addActionListener(ev -> {
                if (methodCombos.size() <= 1) return;
                int idx = methodCombos.indexOf(combo);
                if (idx < 0) return;
                methodCombos.remove(idx);
                amountFields.remove(idx);
                rowsPanel.remove(row);
                rowsPanel.revalidate();
                rowsPanel.repaint();
                dlg.pack();
                updateRemaining.run();
                // refresh remove button state on remaining rows
                for (Component c : rowsPanel.getComponents()) {
                    if (c instanceof JPanel rp) {
                        Component[] cs = rp.getComponents();
                        if (cs.length >= 4 && cs[3] instanceof JButton rb) rb.setEnabled(methodCombos.size() > 1);
                    }
                }
                addRowBtn.setEnabled(methodCombos.size() < MAX_SPLIT);
            });
            removeBtn.setEnabled(methodCombos.size() > 1);

            row.add(combo);
            row.add(Box.createHorizontalStrut(6));
            row.add(amtField);
            row.add(Box.createHorizontalStrut(4));
            row.add(fillBtn);
            row.add(Box.createHorizontalStrut(2));
            row.add(removeBtn);
            rowsPanel.add(row);
            rowsPanel.add(Box.createVerticalStrut(6));
            rowsPanel.revalidate();
            rowsPanel.repaint();
            dlg.pack();
            addRowBtn.setEnabled(methodCombos.size() < MAX_SPLIT);

            // update remove-enabled state on all rows
            for (Component c : rowsPanel.getComponents()) {
                if (c instanceof JPanel rp) {
                    Component[] cs = rp.getComponents();
                    if (cs.length >= 4 && cs[3] instanceof JButton rb && rb != removeBtn)
                        rb.setEnabled(methodCombos.size() > 1);
                }
            }
        };

        addRowBtn.addActionListener(e -> addRow.run());
        addRow.run();
        addRow.run();

        confirm.addActionListener(e -> {
            if (paymentHandler == null) return;

            // Build payments, skip zero-amount rows
            List<Map<String, Object>> payments = new ArrayList<>();
            Set<Integer> usedIds = new HashSet<>();
            for (int i = 0; i < methodCombos.size(); i++) {
                BigDecimal amt;
                try { amt = new BigDecimal(amountFields.get(i).getText().replaceAll("[^0-9.]", "")); }
                catch (Exception ex) { continue; }
                if (amt.compareTo(BigDecimal.ZERO) <= 0) continue;
                int mid = allMethodIds[methodCombos.get(i).getSelectedIndex()];
                if (!usedIds.add(mid)) {
                    JOptionPane.showMessageDialog(dlg, I18n.t("pos.split.duplicate_method"),
                            I18n.t("common.error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Map<String, Object> p = new HashMap<>();
                p.put("payment_method_id", mid);
                p.put("amount", amt);
                payments.add(p);
            }
            if (payments.isEmpty()) return;

            // Per-method confirmation (cash needs no confirmation)
            for (Map<String, Object> p : payments) {
                int    mid  = ((Number) p.get("payment_method_id")).intValue();
                BigDecimal amt = (BigDecimal) p.get("amount");
                if (mid == 1) continue;
                String mName = methodNameFor(mid);
                if (isCardMethod(mid)) {
                    int ok = JOptionPane.showConfirmDialog(dlg,
                            MessageFormat.format(I18n.t("pos.terminal.confirm"), mName, mnt.format(amt)),
                            MessageFormat.format(I18n.t("pos.terminal.title"), mName),
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (ok != JOptionPane.OK_OPTION) return;
                } else {
                    if (!confirmQrInSplit(dlg, mName, amt)) return;
                }
            }

            dlg.dispose();
            paymentHandler.pay(snap, payments, total, onSuccess);
        });

        dlg.add(body);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(420, dlg.getPreferredSize().height));
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }

    // ── QR confirmation within split ──────────────────────────────────────────

    private boolean confirmQrInSplit(JDialog parentDlg, String methodName, BigDecimal amt) {
        boolean[] confirmed = {false};

        JDialog qrDlg = new JDialog(parentDlg, Dialog.ModalityType.APPLICATION_MODAL);
        qrDlg.setUndecorated(true);
        qrDlg.setBackground(Color.WHITE);
        qrDlg.setLayout(new BorderLayout());
        qrDlg.getRootPane().setBorder(BorderFactory.createLineBorder(new Color(0xCDD5E0)));

        // Header (reuse sidebar styling)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(SIDEBAR);
        header.setPreferredSize(new Dimension(0, 40));
        JLabel titleLbl = new JLabel(methodName);
        titleLbl.setFont(new Font("Dialog", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setBorder(new EmptyBorder(0, 16, 0, 0));
        JButton x = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2, r = 5;
                g2.setColor(getModel().isRollover() ? Color.WHITE : new Color(255,255,255,160));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx-r,cy-r,cx+r,cy+r); g2.drawLine(cx+r,cy-r,cx-r,cy+r);
                g2.dispose();
            }
            @Override public boolean isOpaque() { return false; }
        };
        x.setContentAreaFilled(false); x.setBorderPainted(false); x.setFocusPainted(false);
        x.setRolloverEnabled(true); x.setPreferredSize(new Dimension(32,32));
        x.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        x.getModel().addChangeListener(ev -> x.repaint());
        x.addActionListener(ev -> qrDlg.dispose());
        JPanel xWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        xWrap.setOpaque(false); xWrap.add(x);
        header.add(titleLbl, BorderLayout.CENTER);
        header.add(xWrap,    BorderLayout.EAST);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(20, 28, 24, 28));

        JLabel amtLbl = new JLabel("₮" + mnt.format(amt), SwingConstants.CENTER);
        amtLbl.setFont(new Font("Dialog", Font.BOLD, 26));
        amtLbl.setForeground(TXT);
        amtLbl.setAlignmentX(CENTER_ALIGNMENT);
        amtLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        body.add(amtLbl);
        body.add(Box.createVerticalStrut(14));

        ImageIcon qrIcon = view.MainFrame.assetSq("/assets/qr.png", 140);
        JLabel qrLabel = new JLabel(qrIcon, SwingConstants.CENTER);
        qrLabel.setAlignmentX(CENTER_ALIGNMENT);
        qrLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        body.add(qrLabel);
        body.add(Box.createVerticalStrut(12));

        JLabel hint = new JLabel(I18n.t("pos.qr.hint"), SwingConstants.CENTER);
        hint.setFont(new Font("Dialog", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        body.add(hint);
        body.add(Box.createVerticalStrut(18));

        JButton confirmBtn = dialogBtn(I18n.t("pos.qr.done"), new Color(0x16A34A));
        JButton cancelBtn  = dialogBtn(I18n.t("pos.cancel"), new Color(0xF1F5F9));
        confirmBtn.addActionListener(ev -> { confirmed[0] = true; qrDlg.dispose(); });
        cancelBtn.setForeground(MUTED);
        cancelBtn.addActionListener(ev -> qrDlg.dispose());
        body.add(centeredBtn(confirmBtn));
        body.add(Box.createVerticalStrut(8));
        body.add(centeredBtn(cancelBtn));

        qrDlg.add(header, BorderLayout.NORTH);
        qrDlg.add(body,   BorderLayout.CENTER);
        qrDlg.pack();
        qrDlg.setMinimumSize(new Dimension(260, qrDlg.getPreferredSize().height));
        qrDlg.setLocationRelativeTo(parentDlg);
        qrDlg.setVisible(true); // blocks until dismissed
        return confirmed[0];
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

    private static JPanel centeredBtn(JButton btn) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getFont().getSize() + 32));
        row.add(btn);
        return row;
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
