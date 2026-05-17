package controller;

import model.User;
import view.panels.UsersPanel;

public class UsersController {

    private final UsersPanel view;
    private final User       user;

    public UsersController(UsersPanel view, User user) {
        this.view = view;
        this.user = user;
    }
}
