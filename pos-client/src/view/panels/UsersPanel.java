package view.panels;

import i18n.I18n;
import model.User;

import javax.swing.*;
import java.awt.*;

// TODO: User management table — create / activate / deactivate users (admin only)
public class UsersPanel extends JPanel {

    private final User user;

    public UsersPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF0F2F5));
        add(new JLabel(I18n.t("users.title") + " — TODO", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
