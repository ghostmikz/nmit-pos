package handler;

import dao.CategoryDAO;
import model.Category;
import model.Request;
import model.Response;
import model.User;

import java.util.Map;

public class CategoryHandler {

    public static Response getAll(Request req, User user) {
        try {
            return Response.ok(new CategoryDAO().findAll());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response add(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            String name = (String) data.get("name");
            if (name == null || name.isBlank()) return Response.error("Name is required");
            Category created = new CategoryDAO().add(name.trim());
            return Response.ok(created);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response update(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int id       = ((Double) data.get("id")).intValue();
            String name  = (String) data.get("name");
            if (name == null || name.isBlank()) return Response.error("Name is required");
            new CategoryDAO().update(id, name.trim());
            return Response.ok("Updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response delete(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int id = ((Double) data.get("id")).intValue();
            new CategoryDAO().delete(id);
            return Response.ok("Deleted");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
