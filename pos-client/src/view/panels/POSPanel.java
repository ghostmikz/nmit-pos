package view.panels;

import i18n.I18n;
import model.User;

import javax.swing.*;
import java.awt.*;

// TODO: Migrate and adapt POSSystem.java logic into this panel
// The existing src/POSSystem.java is the prototype — connect it to SocketClient here
public class POSPanel extends JPanel {

    private final User user;

    public POSPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF0F2F5));
        // TODO: Build POS UI (product grid + cart) and wire to server via SocketClient
        add(new JLabel(I18n.t("pos.title") + " — TODO", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
