package dao;

import com.google.gson.Gson;
import model.Sale;
import model.SaleItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDAO {

    private final Gson gson = new Gson();

    public int createSale(Sale sale) throws SQLException {
        String itemsJson = gson.toJson(sale.getItems());
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

    public List<Sale> getSalesReport() throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT id, receipt_number, cashier_name, payment_method, discount_name, subtotal, discount_amount, total, is_refunded, created_at FROM view_sales_report ORDER BY created_at DESC";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Sale s = new Sale();
                s.setId(rs.getInt("id"));
                s.setReceiptNumber(rs.getString("receipt_number"));
                s.setTotal(rs.getBigDecimal("total"));
                s.setSubtotal(rs.getBigDecimal("subtotal"));
                s.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                s.setRefunded(rs.getBoolean("is_refunded"));
                list.add(s);
            }
        }
        return list;
    }
}
