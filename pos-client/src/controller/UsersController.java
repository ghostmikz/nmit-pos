package controller;

import client.SocketClient;
import model.Response;
import model.User;
import view.panels.UsersPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UsersController {

    private final UsersPanel view;
    private final User       currentUser;

    public UsersController(UsersPanel view, User currentUser) {
        this.view        = view;
        this.currentUser = currentUser;
        view.setUserSaver(this::saveUser);
        view.setActiveToggler(this::toggleActive);
        view.setUserDeleter(this::deleteUser);
        loadUsers();
    }

    private void loadUsers() {
        new SwingWorker<List<User>, Void>() {
            String error;

            @Override protected List<User> doInBackground() {
                try {
                    if (!SocketClient.getInstance().isConnected())
                        SocketClient.getInstance().connect();
                    Response r = SocketClient.getInstance().send("GET_USERS", currentUser.getToken());
                    if ("OK".equals(r.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<User> u = (List<User>) r.getData();
                        return u;
                    }
                    error = r.getMessage();
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

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send(isNew ? "ADD_USER" : "UPDATE_USER", currentUser.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadUsers(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    private void deleteUser(int userId, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", userId);

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send("DELETE_USER", currentUser.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadUsers(); });
                    } else {
                        SwingUtilities.invokeLater(() -> onError.accept(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
                }
            }
        }.execute();
    }

    private void toggleActive(int userId, boolean active, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> data = new HashMap<>();
        data.put("id",     userId);
        data.put("active", active);

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send("SET_USER_ACTIVE", currentUser.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadUsers(); });
                    } else {
                        SwingUtilities.invokeLater(() -> onError.accept(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
                }
            }
        }.execute();
    }
}
