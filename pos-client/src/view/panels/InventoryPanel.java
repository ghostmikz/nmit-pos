package view.panels;

import i18n.I18n;
import model.User;

import javax.swing.*;
import java.awt.*;

// TODO: Product table with Add / Edit / Delete / Stock adjustment buttons
// Calls: GET_PRODUCTS, ADD_PRODUCT, UPDATE_PRODUCT, DELETE_PRODUCT, UPDATE_STOCK
public class InventoryPanel extends JPanel {

    private final User user;

    public InventoryPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF0F2F5));
        add(new JLabel(I18n.t("inventory.title") + " — TODO", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
