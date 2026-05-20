package controller;

import client.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.User;
import view.panels.DashboardPanel;

import javax.swing.*;
import java.util.Map;

public class DashboardController {

    private final DashboardPanel view;
    private final User           currentUser;
    private static final Gson    GSON = new Gson();

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

    private void loadData() {
        new SwingWorker<Map<String, Object>, Void>() {
            @Override
            @SuppressWarnings("unchecked")
            protected Map<String, Object> doInBackground() throws Exception {
                JsonObject resp = SocketClient.getInstance().send("GET_DASHBOARD", currentUser.getToken());
                if (!"OK".equals(resp.get("status").getAsString()))
                    throw new Exception(resp.has("message") ? resp.get("message").getAsString() : "Error");
                return GSON.fromJson(resp.get("data"), Map.class);
            }

            @Override
            protected void done() {
                try {
                    view.setDashboardData(get());
                } catch (Exception ex) {
                    view.showToast("Failed to load: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
