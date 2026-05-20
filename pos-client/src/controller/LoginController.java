package controller;

import client.SocketClient;
import com.google.gson.JsonObject;
import i18n.I18n;
import model.User;
import view.LoginFrame;
import view.MainFrame;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class LoginController {

    private final LoginFrame view;

    public LoginController(LoginFrame view) {
        this.view = view;
        view.setLoginListener(this::doLogin);
    }

    private void doLogin(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            view.showError(I18n.t("login.error.empty"));
            return;
        }
        view.setLoading(true);

        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                if (!SocketClient.getInstance().isConnected())
                    SocketClient.getInstance().connect();
                Map<String, String> data = new HashMap<>();
                data.put("username", username);
                data.put("password", password);
                return SocketClient.getInstance().send("LOGIN", null, data);
            }
            @Override protected void done() {
                view.setLoading(false);
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        User user = parseUser(res.getAsJsonObject("data"));
                        view.close();
                        MainFrame main = new MainFrame(user);
                        new MainController(main, user);
                        main.setVisible(true);
                    } else {
                        view.showError(res.get("message").getAsString());
                    }
                } catch (Exception ex) {
                    view.showError(ex.getMessage());
                }
            }
        }.execute();
    }

    private static User parseUser(JsonObject d) {
        User user = new User();
        user.setId(d.get("id").getAsInt());
        user.setUsername(d.get("username").getAsString());
        user.setFullName(d.get("fullName").getAsString());
        user.setRole(d.get("role").getAsString());
        user.setToken(d.get("token").getAsString());
        return user;
    }
}
