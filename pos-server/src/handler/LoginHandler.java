package handler;

import dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import model.Request;
import model.Response;
import model.User;
import server.SessionManager;

import java.util.HashMap;
import java.util.Map;

public class LoginHandler {

    public static Response handle(Request req) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> data = (Map<String, String>) req.getData();
            String username = data.get("username");
            String password = data.get("password");

            UserDAO dao = new UserDAO();
            User user = dao.findByUsername(username);

            if (user == null) return Response.error("Хэрэглэгч олдсонгүй");

            if (!BCrypt.checkpw(password, user.getPasswordHash())) return Response.error("Нууц үг буруу");

            String token = SessionManager.createSession(user);

            Map<String, Object> result = new HashMap<>();
            result.put("token",    token);
            result.put("id",       user.getId());
            result.put("username", user.getUsername());
            result.put("fullName", user.getFullName());
            result.put("role",     user.getRole());

            return Response.ok(result);
        } catch (Exception e) {
            return Response.error("Login failed: " + e.getMessage());
        }
    }

    public static Response logout(Request req) {
        SessionManager.removeSession(req.getToken());
        return Response.ok("Logged out");
    }
}
