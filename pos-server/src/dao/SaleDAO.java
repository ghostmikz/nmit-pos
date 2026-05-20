package dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import model.Sale;
import model.SaleItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    // sp_create_sale expects product_id / unit_price (snake_case) in the JSON items array
    private static final Gson SP_GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public int createSale(Sale sale) throws SQLException {
        String itemsJson = SP_GSON.toJson(sale.getItems());
        String sql = "CALL sp_create_sale(?,?,?,?,?,?,?,?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setString(1, sale.getReceiptNumber());
            cs.setInt(2, sale.getUserId());
            cs.setInt(3, sale.getPaymentMethodId());
            cs.setObject(4, sale.getDiscountId());
            cs.setBigDecimal(5, sale.getSubtotal());
            cs.setBigDecimal(6, sale.getDiscountAmount());
            cs.setBigDecimal(7, sale.getTotal());
            cs.setString(8, sale.getNotes());
            cs.setString(9, itemsJson);
            ResultSet rs = cs.executeQuery();
            return rs.next() ? rs.getInt("sale_id") : -1;
        }
    }

    public void processRefund(int saleId, int userId, String reason, java.math.BigDecimal amount) throws SQLException {
        String sql = "CALL sp_process_refund(?,?,?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, saleId);
            cs.setInt(2, userId);
            cs.setString(3, reason);
            cs.setBigDecimal(4, amount);
            cs.execute();
        }
    }

    public List<Sale> getSalesReport(String startDate, String endDate) throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT id, receipt_number, cashier_name, payment_method, discount_name, " +
                     "subtotal, discount_amount, total, is_refunded, created_at " +
                     "FROM view_sales_report " +
                     "WHERE (? IS NULL OR DATE(created_at) >= ?) " +
                     "  AND (? IS NULL OR DATE(created_at) <= ?) " +
                     "ORDER BY created_at DESC";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setString(1, startDate); ps.setString(2, startDate);
            ps.setString(3, endDate);   ps.setString(4, endDate);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Sale s = new Sale();
                s.setId(rs.getInt("id"));
                s.setReceiptNumber(rs.getString("receipt_number"));
                s.setCashierName(rs.getString("cashier_name"));
                s.setPaymentMethod(rs.getString("payment_method"));
                s.setDiscountName(rs.getString("discount_name"));
                s.setSubtotal(rs.getBigDecimal("subtotal"));
                s.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                s.setTotal(rs.getBigDecimal("total"));
                s.setRefunded(rs.getBoolean("is_refunded"));
                s.setCreatedAt(rs.getString("created_at"));
                list.add(s);
            }
        }
        return list;
    }
}
