package view.panels;

import i18n.I18n;
import model.User;

import javax.swing.*;
import java.awt.*;

// TODO: KPI cards (today revenue, transactions), low stock list, JFreeChart weekly bar chart
// Calls: GET_DASHBOARD
public class DashboardPanel extends JPanel {

    private final User user;

    public DashboardPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF0F2F5));
        add(new JLabel(I18n.t("dashboard.title") + " — TODO", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
