package handler;

import dao.DatabaseConnection;
import model.Request;
import model.Response;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentMethodHandler {

    public static Response getAll(Request req, User user) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            try (CallableStatement cs = DatabaseConnection.getInstance()
                    .prepareCall("CALL sp_get_payment_methods()");
                 ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id",   rs.getInt("id"));
                    row.put("name", rs.getString("name"));
                    list.add(row);
                }
            }
            return Response.ok(list);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response add(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            String name = (String) d.get("name");
            if (name == null || name.isBlank()) return Response.error("Method name required");
            try (CallableStatement cs = DatabaseConnection.getInstance()
                    .prepareCall("CALL sp_add_payment_method(?)")) {
                cs.setString(1, name.trim());
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) return Response.ok(rs.getInt("id"));
                }
            }
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response delete(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            int id = ((Number) d.get("id")).intValue();
            try (CallableStatement cs = DatabaseConnection.getInstance()
                    .prepareCall("CALL sp_delete_payment_method(?)")) {
                cs.setInt(1, id);
                cs.execute();
            }
            return Response.ok(null);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
