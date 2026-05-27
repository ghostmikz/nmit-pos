package dao;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Sale;
import model.SaleItem;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    // sp_create_sale expects snake_case keys in the JSON arrays
    private static final Gson SP_GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public int createSale(Sale sale) throws SQLException {
        String itemsJson    = SP_GSON.toJson(sale.getItems());
        String paymentsJson = SP_GSON.toJson(sale.getPayments());
        String sql = "CALL sp_create_sale(?,?,?,?,?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setString(1, sale.getReceiptNumber());
            cs.setInt(2, sale.getUserId());
            cs.setBigDecimal(3, sale.getSubtotal());
            cs.setBigDecimal(4, sale.getTotal());
            cs.setString(5, itemsJson);
            cs.setString(6, paymentsJson);
            ResultSet rs = cs.executeQuery();
            return rs.next() ? rs.getInt("sale_id") : -1;
        }
    }

    public void processRefund(int saleId, int userId, String reason, BigDecimal amount) throws SQLException {
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
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_get_sales_report(?, ?)")) {
            cs.setString(1, startDate);
            cs.setString(2, endDate);
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setReceiptNumber(rs.getString("receipt_number"));
                    s.setCashierName(rs.getString("cashier_name"));
                    s.setPaymentMethod(rs.getString("payment_method"));
                    s.setSubtotal(rs.getBigDecimal("subtotal"));
                    s.setTotal(rs.getBigDecimal("total"));
                    s.setRefunded(rs.getBoolean("is_refunded"));
                    s.setCreatedAt(rs.getString("created_at"));
                    s.setNotes(rs.getString("notes"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
