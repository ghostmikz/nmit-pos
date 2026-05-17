package controller;

import client.SocketClient;
import model.User;
import view.LoginFrame;
import view.MainFrame;
import view.panels.*;

public class MainController {

    private final MainFrame view;
    private final User      user;

    public MainController(MainFrame view, User user) {
        this.view = view;
        this.user = user;

        buildPanels();
        view.setLogoutListener(this::doLogout);
    }

    private void buildPanels() {
        POSPanel pos = new POSPanel(user);
        new POSController(pos, user);
        view.addPanel(pos, "POS");

        if (user.isManager()) {
            InventoryPanel inv = new InventoryPanel(user);
            new InventoryController(inv, user);
            view.addPanel(inv, "INVENTORY");

            ReportsPanel rep = new ReportsPanel(user);
            new ReportsController(rep, user);
            view.addPanel(rep, "REPORTS");

            DashboardPanel dash = new DashboardPanel(user);
            new DashboardController(dash, user);
            view.addPanel(dash, "DASHBOARD");
        }

        if (user.isAdmin()) {
            UsersPanel usr = new UsersPanel(user);
            new UsersController(usr, user);
            view.addPanel(usr, "USERS");
        }

        view.showPanel("POS");
    }

    private void doLogout() {
        try {
            SocketClient.getInstance().send("LOGOUT", user.getToken());
            SocketClient.getInstance().disconnect();
        } catch (Exception ignored) {}
        view.close();
        LoginFrame login = new LoginFrame();
        new LoginController(login);
        login.setVisible(true);
    }
}
