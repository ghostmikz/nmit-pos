package view.panels;

import i18n.I18n;
import model.User;

import javax.swing.*;
import java.awt.*;

// TODO: Sales report table with date filter
// Uses JFreeChart for charts, OpenPDF for export
// Calls: GET_REPORT
public class ReportsPanel extends JPanel {

    private final User user;

    public ReportsPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF0F2F5));
        add(new JLabel(I18n.t("report.title") + " — TODO", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
