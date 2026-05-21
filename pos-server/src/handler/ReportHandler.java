package handler;

import dao.ReportDAO;
import dao.SaleDAO;
import model.Request;
import model.Response;
import model.User;

import java.util.HashMap;
import java.util.Map;

public class ReportHandler {

    @SuppressWarnings("unchecked")
    public static Response getSalesReport(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            String startDate = null, endDate = null;
            if (req.getData() != null) {
                Map<String, Object> d = (Map<String, Object>) req.getData();
                startDate = (String) d.get("startDate");
                endDate   = (String) d.get("endDate");
            }
            return Response.ok(new SaleDAO().getSalesReport(startDate, endDate));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response getDashboard(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            ReportDAO dao = new ReportDAO();
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("daily",           dao.getDailySummary());
            dashboard.put("topProducts",     dao.getTopProducts());
            dashboard.put("lowStock",        dao.getLowStock());
            dashboard.put("categoryRevenue", dao.getCategoryRevenue());
            return Response.ok(dashboard);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
