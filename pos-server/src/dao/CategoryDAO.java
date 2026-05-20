package dao;

import model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name, description FROM categories ORDER BY name";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        }
        return list;
    }

    public Category add(String name) throws SQLException {
        String sql = "CALL sp_add_category(?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setString(1, name);
            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                Category c = new Category();
                c.setId(rs.getInt("category_id"));
                c.setName(rs.getString("name"));
                return c;
            }
        }
        return null;
    }

    public void update(int id, String name) throws SQLException {
        String sql = "CALL sp_update_category(?,?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, id);
            cs.setString(2, name);
            cs.execute();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "CALL sp_delete_category(?)";
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall(sql)) {
            cs.setInt(1, id);
            cs.execute();
        }
    }
}
