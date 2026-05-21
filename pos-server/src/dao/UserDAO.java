package dao;

import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByUsername(String username) throws SQLException {
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_login(?)")) {
            cs.setString(1, username);
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, full_name, role, is_active FROM users ORDER BY role, full_name";
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int create(User u) throws SQLException {
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_add_user(?,?,?,?,?)")) {
            cs.setString(1, u.getUsername());
            cs.setString(2, u.getPasswordHash());
            cs.setString(3, u.getFullName());
            cs.setString(4, u.getRole());
            cs.setObject(5, u.getCreatedBy());
            cs.execute();
        }
        try (Statement st = DatabaseConnection.getInstance().createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    public void update(int id, String fullName, String role, String password) throws SQLException {
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_update_user(?,?,?,?)")) {
            cs.setInt(1, id);
            cs.setString(2, fullName);
            cs.setString(3, role);
            cs.setObject(4, (password != null && !password.isBlank()) ? password : null);
            cs.execute();
        }
    }

    public void delete(int userId) throws SQLException {
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_delete_user(?)")) {
            cs.setInt(1, userId);
            cs.execute();
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        try (CallableStatement cs = DatabaseConnection.getInstance().prepareCall("CALL sp_set_user_active(?,?)")) {
            cs.setInt(1, userId);
            cs.setBoolean(2, active);
            cs.execute();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("is_active"));
        return u;
    }
}
