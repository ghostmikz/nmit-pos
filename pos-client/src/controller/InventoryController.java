package controller;

import model.User;
import view.panels.InventoryPanel;

public class InventoryController {

    private final InventoryPanel view;
    private final User           user;

    public InventoryController(InventoryPanel view, User user) {
        this.view = view;
        this.user = user;
    }
}
