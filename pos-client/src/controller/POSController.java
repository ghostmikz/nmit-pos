package controller;

import model.User;
import view.panels.POSPanel;

public class POSController {

    private final POSPanel view;
    private final User     user;

    public POSController(POSPanel view, User user) {
        this.view = view;
        this.user = user;
    }
}
