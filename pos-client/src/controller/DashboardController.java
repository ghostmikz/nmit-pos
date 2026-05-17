package controller;

import model.User;
import view.panels.DashboardPanel;

public class DashboardController {

    private final DashboardPanel view;
    private final User           user;

    public DashboardController(DashboardPanel view, User user) {
        this.view = view;
        this.user = user;
    }
}
