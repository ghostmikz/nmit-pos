package handler;

import dao.ReportDAO;
import dao.SaleDAO;
import model.Request;
import model.Response;
import model.User;

import java.util.HashMap;
import java.util.Map;

public class ReportHandler {

    public static Response getSalesReport(Request req, User user) {
        if (user.getRole().equals("cashier"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            return Response.ok(new SaleDAO().getSalesReport());
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response getDashboard(Request req, User user) {
        if (user.getRole().equals("cashier"))
            return Response.error("Эрх хүрэлцэхгүй");
        try {
            ReportDAO dao = new ReportDAO();
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("daily",          dao.getDailySummary());
            // Weekly, top products, payment summary loaded separately to keep response small
            return Response.ok(dashboard);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
