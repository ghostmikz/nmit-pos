package controller;

import client.SocketClient;
import i18n.I18n;
import model.Response;
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

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
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
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> d = (Map<String, Object>) res.getData();
                        User user = new User();
                        user.setId(((Number) d.get("id")).intValue());
                        user.setUsername((String) d.get("username"));
                        user.setFullName((String) d.get("fullName"));
                        user.setRole((String) d.get("role"));
                        user.setToken((String) d.get("token"));
                        view.close();
                        MainFrame main = new MainFrame(user);
                        new MainController(main, user);
                        main.setVisible(true);
                    } else {
                        view.showError(I18n.t("login.error.failed"));
                    }
                } catch (Exception ex) {
                    view.showError(I18n.t("login.error.connection"));
                }
            }
        }.execute();
    }
}
