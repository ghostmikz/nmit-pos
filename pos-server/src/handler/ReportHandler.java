package handler;

import com.google.gson.Gson;
import dao.ReportDAO;
import dao.SaleDAO;
import model.Request;
import model.Response;
import model.User;

import java.util.HashMap;
import java.util.Map;

public class ReportHandler {

    private static final Gson GSON = new Gson();

    public static Response getSalesReport(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            String startDate = null, endDate = null;
            if (req.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> d = GSON.fromJson(req.getData().toString(), Map.class);
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
            dashboard.put("daily",       dao.getDailySummary());
            dashboard.put("weeklySales", dao.getWeeklySales());
            dashboard.put("topProducts", dao.getTopProducts());
            dashboard.put("lowStock",    dao.getLowStock());
            return Response.ok(dashboard);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
