package dao;

import model.Product;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, barcode, product_name, category_id, category_name, price, cost_price, stock_quantity, unit, expiry_date, is_active, has_image, low_stock_alert FROM view_product_stock ORDER BY product_name";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int create(Product p) throws SQLException {
        String sql = "CALL sp_add_product(?,?,?,?,?,?,?,?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setString(1, p.getBarcode());
            cs.setString(2, p.getName());
            cs.setInt(3, p.getCategoryId());
            cs.setBigDecimal(4, p.getPrice());
            cs.setBigDecimal(5, p.getCostPrice());
            cs.setInt(6, p.getStockQuantity());
            cs.setString(7, p.getUnit());
            cs.setObject(8, p.getExpiryDate());
            cs.setInt(9, p.getLowStockAlert() > 0 ? p.getLowStockAlert() : 10);
            ResultSet rs = cs.executeQuery();
            return rs.next() ? rs.getInt("product_id") : -1;
        }
    }

    public void update(Product p) throws SQLException {
        String sql = "CALL sp_update_product(?,?,?,?,?,?,?,?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, p.getId());
            cs.setString(2, p.getBarcode());
            cs.setString(3, p.getName());
            cs.setInt(4, p.getCategoryId());
            cs.setBigDecimal(5, p.getPrice());
            cs.setBigDecimal(6, p.getCostPrice());
            cs.setString(7, p.getUnit());
            cs.setObject(8, p.getExpiryDate());
            cs.setInt(9, p.getLowStockAlert() > 0 ? p.getLowStockAlert() : 10);
            cs.execute();
        }
    }

    public void delete(int productId) throws SQLException {
        String sql = "CALL sp_delete_product(?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, productId);
            cs.execute();
        }
    }

    public byte[] getImage(int productId) throws SQLException {
        String sql = "SELECT image FROM products WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes("image") : null;
            }
        }
    }

    public void updateImage(int productId, byte[] image) throws SQLException {
        String sql = "UPDATE products SET image=? WHERE id=?";
        try (PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(sql)) {
            ps.setBytes(1, image);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void updateStock(int productId, int quantity) throws SQLException {
        String sql = "CALL sp_update_stock(?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, productId);
            cs.setInt(2, quantity);
            cs.execute();
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setBarcode(rs.getString("barcode"));
        p.setName(rs.getString("product_name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setUnit(rs.getString("unit"));
        p.setExpiryDate(rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toString() : null);
        p.setActive(rs.getBoolean("is_active"));
        p.setHasImage(rs.getBoolean("has_image"));
        p.setLowStockAlert(rs.getInt("low_stock_alert"));
        return p;
    }
}
