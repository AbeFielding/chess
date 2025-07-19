package dataaccess;

import model.User;
import java.sql.*;

public class UserMySQLDAO implements UserDAO {

    @Override
    public void insertUser(User user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to insert user", ex);
        }
    }

    @Override
    public User getUserByUsername(String username) throws DataAccessException {
        String sql = "SELECT id, username, password_hash FROM users WHERE username = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get user by username", ex);
        }
    }

    @Override
    public User getUserById(int id) throws DataAccessException {
        String sql = "SELECT id, username, password_hash FROM users WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password_hash")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get user by id", ex);
        }
    }
}
