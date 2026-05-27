package handler;

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
            try (CallableStatement cs = dao.DatabaseConnection.getInstance()
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
}
