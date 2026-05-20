package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    public Map<String, Object> getDailySummary() throws SQLException {
        Map<String, Object> result = new HashMap<>();
        String sql = "SELECT sale_date, total_transactions, total_revenue, total_discounts FROM view_daily_sales";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                result.put("date",              rs.getString("sale_date"));
                result.put("totalTransactions", rs.getInt("total_transactions"));
                result.put("totalRevenue",      rs.getBigDecimal("total_revenue"));
                result.put("totalDiscounts",    rs.getBigDecimal("total_discounts"));
            }
        }
        return result;
    }

    public List<Map<String, Object>> getWeeklySales() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT sale_date, total_transactions, total_revenue FROM view_weekly_sales ORDER BY sale_date ASC";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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
        String sql = "SELECT product_name, category_name, total_sold, total_revenue FROM view_top_products LIMIT 10";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("productName",  rs.getString("product_name"));
                row.put("categoryName", rs.getString("category_name"));
                row.put("totalSold",    rs.getInt("total_sold"));
                row.put("totalRevenue", rs.getBigDecimal("total_revenue"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getLowStock() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT product_name, category_name, stock_quantity, unit FROM view_low_stock LIMIT 10";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("productName",   rs.getString("product_name"));
                row.put("categoryName",  rs.getString("category_name"));
                row.put("stockQuantity", rs.getInt("stock_quantity"));
                row.put("unit",          rs.getString("unit"));
                list.add(row);
            }
        }
        return list;
    }

}
