package controller;

import client.SocketClient;
import model.Response;
import model.User;
import view.panels.DashboardPanel;

import javax.swing.*;
import java.util.Map;

public class DashboardController {

    private final DashboardPanel view;
    private final User           currentUser;

    public DashboardController(DashboardPanel view, User currentUser) {
        this.view        = view;
        this.currentUser = currentUser;
        view.setDataLoader(this::loadData);
        loadData();
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && view.isShowing()) {
                loadData();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                Response resp = SocketClient.getInstance().send("GET_DASHBOARD", currentUser.getToken());
                if (!"OK".equals(resp.getStatus()))
                    throw new Exception(resp.getMessage() != null ? resp.getMessage() : "Error");
                return (Map<String, Object>) resp.getData();
            }

            @Override protected void done() {
                try {
                    view.setDashboardData(get());
                } catch (Exception ex) {
                    view.showToast("Failed to load: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
