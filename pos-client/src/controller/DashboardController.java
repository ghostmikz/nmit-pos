package controller;

import client.SocketClient;
import model.Response;
import model.User;
import view.panels.DashboardPanel;

import javax.swing.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    private static final long STALE_MS = 60_000; // reload on nav only if data is >60s old

    private final DashboardPanel view;
    private final User           currentUser;
    private String lastStart, lastEnd;
    private long   lastLoadTime = 0;

    public DashboardController(DashboardPanel view, User currentUser) {
        this.view        = view;
        this.currentUser = currentUser;
        LocalDate today = LocalDate.now();
        lastStart = today.minusDays(29).toString();
        lastEnd   = today.toString();
        view.setDataLoader(this::loadData);
        loadData(lastStart, lastEnd);
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && view.isShowing()
                    && System.currentTimeMillis() - lastLoadTime > STALE_MS) {
                loadData(lastStart, lastEnd);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void loadData(String start, String end) {
        lastStart = start;
        lastEnd   = end;
        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() throws Exception {
                Map<String, String> data = new HashMap<>();
                data.put("startDate", start);
                data.put("endDate",   end);
                Response resp = SocketClient.getInstance().send("GET_DASHBOARD", currentUser.getToken(), data);
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
