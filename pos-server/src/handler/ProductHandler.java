package handler;

import com.google.gson.Gson;
import dao.CategoryDAO;
import dao.ProductDAO;
import model.Product;
import model.Request;
import model.Response;
import model.User;
import java.util.Base64;
import java.util.Map;

public class ProductHandler {

    private static final Gson gson = new Gson();

    public static Response getAll(Request req, User user) {
        try {
            return Response.ok(new ProductDAO().findAll());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response getCategories(Request req, User user) {
        try {
            return Response.ok(new CategoryDAO().findAll());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response add(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            Product p = gson.fromJson(gson.toJson(req.getData()), Product.class);
            int id = new ProductDAO().create(p);
            return Response.ok(id);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response update(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            Product p = gson.fromJson(gson.toJson(req.getData()), Product.class);
            new ProductDAO().update(p);
            return Response.ok("Updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response delete(Request req, User user) {
        if (!user.getRole().equals("admin"))
            return Response.error("Зөвхөн админ устгах боломжтой");
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) req.getData();
            int productId = ((Double) data.get("productId")).intValue();
            new ProductDAO().delete(productId);
            return Response.ok("Deleted");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response getImage(Request req, User user) {
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Double) data.get("productId")).intValue();
            byte[] bytes = new ProductDAO().getImage(productId);
            if (bytes == null) return Response.ok(null);
            return Response.ok(Base64.getEncoder().encodeToString(bytes));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response updateImage(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int productId = ((Double) data.get("productId")).intValue();
            String base64  = (String) data.get("image");
            new ProductDAO().updateImage(productId, Base64.getDecoder().decode(base64));
            return Response.ok("Image updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response updateStock(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) req.getData();
            int productId = ((Double) data.get("productId")).intValue();
            int quantity  = ((Double) data.get("quantity")).intValue();
            new ProductDAO().updateStock(productId, quantity);
            return Response.ok("Stock updated");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
