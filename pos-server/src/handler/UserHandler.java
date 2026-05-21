package handler;

import dao.UserDAO;
import model.Request;
import model.Response;
import model.User;

import java.util.List;
import java.util.Map;

public class UserHandler {
    private static final UserDAO DAO = new UserDAO();

    @SuppressWarnings("unchecked")
    public static Response getAll(Request req, User caller) {
        if (!"admin".equals(caller.getRole())) return Response.error("Access denied");
        try {
            List<User> users = DAO.findAll();
            users.forEach(u -> u.setPasswordHash(null));
            return Response.ok(users);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response add(Request req, User caller) {
        if (!"admin".equals(caller.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            String username = (String) d.get("username");
            String password = (String) d.get("password");
            String fullName = (String) d.get("fullName");
            String role     = (String) d.get("role");
            if (username == null || username.isBlank() || password == null || password.isBlank()
                    || fullName == null || fullName.isBlank() || role == null)
                return Response.error("All fields are required");
            User u = new User();
            u.setUsername(username.trim());
            u.setPasswordHash(password);
            u.setFullName(fullName.trim());
            u.setRole(role);
            u.setCreatedBy(caller.getId());
            int id = DAO.create(u);
            return id > 0 ? Response.ok(id) : Response.error("Failed to create user");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response update(Request req, User caller) {
        if (!"admin".equals(caller.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            int    id       = ((Number) d.get("id")).intValue();
            String fullName = (String) d.get("fullName");
            String role     = (String) d.get("role");
            String password = (String) d.get("password");
            DAO.update(id, fullName, role, password);
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response setActive(Request req, User caller) {
        if (!"admin".equals(caller.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            int     id     = ((Number) d.get("id")).intValue();
            boolean active = (Boolean) d.get("active");
            if (id == caller.getId()) return Response.error("Cannot deactivate your own account");
            DAO.setActive(id, active);
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response delete(Request req, User caller) {
        if (!"admin".equals(caller.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            int id = ((Number) d.get("id")).intValue();
            if (id == caller.getId()) return Response.error("Cannot delete your own account");
            DAO.delete(id);
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
