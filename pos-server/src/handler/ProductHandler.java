package handler;

import dao.ProductDAO;
import model.Product;
import model.Request;
import model.Response;
import model.User;
import java.util.Base64;
import java.util.Map;

public class ProductHandler {

    private static final ProductDAO DAO = new ProductDAO();

    public static Response getAll(Request req, User user) {
        try {
            return Response.ok(DAO.findAll());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response add(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Product p = (Product) req.getData();
            int id = DAO.create(p);
            return Response.ok(id);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response update(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Product p = (Product) req.getData();
            DAO.update(p);
            return Response.ok("Updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response delete(Request req, User user) {
        if (!user.getRole().equals("admin"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Number) data.get("productId")).intValue();
            DAO.delete(productId);
            return Response.ok("Deleted");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response getImage(Request req, User user) {
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Number) data.get("productId")).intValue();
            byte[] bytes = DAO.getImage(productId);
            if (bytes == null) return Response.ok(null);
            return Response.ok(Base64.getEncoder().encodeToString(bytes));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response updateImage(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Number) data.get("productId")).intValue();
            String base64  = (String) data.get("image");
            DAO.updateImage(productId, Base64.getDecoder().decode(base64));
            return Response.ok("Image updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response updateStock(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Number) data.get("productId")).intValue();
            int quantity  = ((Number) data.get("quantity")).intValue();
            DAO.updateStock(productId, quantity);
            return Response.ok("Stock updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
