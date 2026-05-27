package handler;

import dao.ReportDAO;
import dao.SaleDAO;
import model.Request;
import model.Response;
import model.User;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ReportHandler {

    private static final SaleDAO   SALE_DAO   = new SaleDAO();
    private static final ReportDAO REPORT_DAO = new ReportDAO();

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
            return Response.ok(SALE_DAO.getSalesReport(startDate, endDate));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response getDashboard(Request req, User user) {
        if ("cashier".equals(user.getRole())) return Response.error("Access denied");
        try {
            String startDate = null, endDate = null;
            if (req.getData() != null) {
                Map<String, Object> d = (Map<String, Object>) req.getData();
                startDate = (String) d.get("startDate");
                endDate   = (String) d.get("endDate");
            }
            if (startDate == null) startDate = LocalDate.now().minusDays(29).toString();
            if (endDate   == null) endDate   = LocalDate.now().toString();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("summary",     REPORT_DAO.getSummaryByPeriod(startDate, endDate));
            dashboard.put("dailyTrend",  REPORT_DAO.getDailyTrend(startDate, endDate));
            dashboard.put("topProducts", REPORT_DAO.getTopProducts());
            dashboard.put("lowStock",    REPORT_DAO.getLowStock());
            dashboard.put("recentSales", REPORT_DAO.getRecentSales(10));
            return Response.ok(dashboard);
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
