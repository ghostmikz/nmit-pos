package dao;

import java.sql.*;
import java.util.HashMap;
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

    public ResultSet getWeeklySales() throws SQLException {
        String sql = "SELECT sale_date, total_transactions, total_revenue FROM view_weekly_sales";
        Statement st = DatabaseConnection.getInstance().createStatement();
        return st.executeQuery(sql);
    }

    public ResultSet getTopProducts() throws SQLException {
        String sql = "SELECT product_name, category_name, total_sold, total_revenue FROM view_top_products LIMIT 10";
        Statement st = DatabaseConnection.getInstance().createStatement();
        return st.executeQuery(sql);
    }

    public ResultSet getPaymentSummary() throws SQLException {
        String sql = "SELECT payment_method, total_transactions, total_revenue FROM view_payment_summary";
        Statement st = DatabaseConnection.getInstance().createStatement();
        return st.executeQuery(sql);
    }

    public ResultSet getDiscountUsage() throws SQLException {
        String sql = "SELECT discount_name, type, value, times_used, total_saved FROM view_discount_usage";
        Statement st = DatabaseConnection.getInstance().createStatement();
        return st.executeQuery(sql);
    }
}
