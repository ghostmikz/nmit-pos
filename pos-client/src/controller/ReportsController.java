package controller;

import model.User;
import view.panels.ReportsPanel;

public class ReportsController {

    private final ReportsPanel view;
    private final User         user;

    public ReportsController(ReportsPanel view, User user) {
        this.view = view;
        this.user = user;
    }
}
