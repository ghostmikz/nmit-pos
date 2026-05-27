package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    private static final int REPORT_LIMIT = 10;

    public Map<String, Object> getDailySummary() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        result.put("date",              java.time.LocalDate.now().toString());
        result.put("totalTransactions", 0);
        result.put("totalRevenue",      java.math.BigDecimal.ZERO);
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_daily_summary()");
             ResultSet rs = cs.executeQuery()) {
            if (rs.next()) {
                result.put("date",              rs.getString("sale_date"));
                result.put("totalTransactions", rs.getInt("total_transactions"));
                result.put("totalRevenue",      rs.getBigDecimal("total_revenue"));
            }
        }
        return result;
    }

    public List<Map<String, Object>> getWeeklySales() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_weekly_sales()");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("saleDate",          rs.getString("sale_date"));
                row.put("totalTransactions", rs.getInt("total_transactions"));
                row.put("totalRevenue",      rs.getBigDecimal("total_revenue"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getTopProducts() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_top_products(?)")) {
            cs.setInt(1, REPORT_LIMIT);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productName",  rs.getString("product_name"));
                    row.put("categoryName", rs.getString("category_name"));
                    row.put("totalSold",    rs.getInt("total_sold"));
                    row.put("totalRevenue", rs.getBigDecimal("total_revenue"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getLowStock() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_low_stock(?)")) {
            cs.setInt(1, REPORT_LIMIT);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productName",   rs.getString("product_name"));
                    row.put("categoryName",  rs.getString("category_name"));
                    row.put("stockQuantity", rs.getInt("stock_quantity"));
                    row.put("unit",          rs.getString("unit"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getSummaryByPeriod(String start, String end) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_dashboard_summary(?, ?)")) {
            cs.setString(1, start);
            cs.setString(2, end);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    result.put("totalTransactions", rs.getInt("total_transactions"));
                    result.put("totalRevenue",      rs.getBigDecimal("total_revenue"));
                    result.put("avgSaleValue",      rs.getBigDecimal("avg_sale_value"));
                }
            }
        }
        return result;
    }

    public List<Map<String, Object>> getDailyTrend(String start, String end) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_daily_trend(?, ?)")) {
            cs.setString(1, start);
            cs.setString(2, end);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("saleDate",          rs.getString("sale_date"));
                    row.put("totalTransactions", rs.getInt("total_transactions"));
                    row.put("totalRevenue",      rs.getBigDecimal("total_revenue"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getRecentSales(int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_recent_sales(?)")) {
            cs.setInt(1, limit);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("receiptNumber",  rs.getString("receipt_number"));
                    row.put("cashierName",    rs.getString("cashier_name"));
                    row.put("paymentMethod",  rs.getString("payment_method"));
                    row.put("total",          rs.getBigDecimal("total"));
                    row.put("isRefunded",     rs.getBoolean("is_refunded"));
                    row.put("createdAt",      rs.getString("created_at"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getCategoryRevenue() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_category_revenue()");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("categoryName", rs.getString("category_name"));
                row.put("totalRevenue", rs.getBigDecimal("totalRevenue"));
                list.add(row);
            }
        }
        return list;
    }

}
