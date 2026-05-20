package controller;

import client.SocketClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.User;
import view.panels.UsersPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UsersController {

    private static final Gson GSON = new Gson();

    private final UsersPanel view;
    private final User       currentUser;

    public UsersController(UsersPanel view, User currentUser) {
        this.view        = view;
        this.currentUser = currentUser;
        view.setUserSaver(this::saveUser);
        view.setActiveToggler(this::toggleActive);
        loadUsers();
    }

    private void loadUsers() {
        new SwingWorker<List<User>, Void>() {
            String error;

            @Override protected List<User> doInBackground() {
                try {
                    if (!SocketClient.getInstance().isConnected())
                        SocketClient.getInstance().connect();
                    var r = SocketClient.getInstance().send("GET_USERS", currentUser.getToken());
                    if ("OK".equals(r.get("status").getAsString()))
                        return GSON.fromJson(r.get("data"), new TypeToken<List<User>>(){}.getType());
                    error = r.get("message").getAsString();
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return new ArrayList<>();
            }

            @Override protected void done() {
                try {
                    List<User> users = get();
                    if (error != null) { view.showError(error); return; }
                    view.setUsers(users);
                } catch (Exception e) {
                    view.showError(e.getMessage());
                }
            }
        }.execute();
    }

    private void saveUser(User u, String password, boolean isNew, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        if (!isNew) data.put("id", u.getId());
        data.put("fullName", u.getFullName());
        data.put("username", u.getUsername());
        data.put("role",     u.getRole());
        if (!password.isBlank()) data.put("password", password);

        new SwingWorker<com.google.gson.JsonObject, Void>() {
            @Override protected com.google.gson.JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance().send(isNew ? "ADD_USER" : "UPDATE_USER", currentUser.getToken(), data);
            }
            @Override protected void done() {
                try {
                    var res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadUsers(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.get("message").getAsString()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    private void toggleActive(int userId, boolean active, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> data = new HashMap<>();
        data.put("id",     userId);
        data.put("active", active);

        new SwingWorker<com.google.gson.JsonObject, Void>() {
            @Override protected com.google.gson.JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance().send("SET_USER_ACTIVE", currentUser.getToken(), data);
            }
            @Override protected void done() {
                try {
                    var res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadUsers(); });
                    } else {
                        SwingUtilities.invokeLater(() -> onError.accept(res.get("message").getAsString()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
                }
            }
        }.execute();
    }
}
