package dataaccess;

import model.AuthToken;
import java.sql.*;

public class AuthTokenMySQLDAO implements AuthTokenDAO {

    @Override
    public void insertToken(AuthToken token) throws DataAccessException {
        String sql = "INSERT INTO auth_tokens (token, user_id) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.getToken());
            ps.setInt(2, token.getUserId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to insert token", ex);
        }
    }

    @Override
    public AuthToken getToken(String tokenStr) throws DataAccessException {
        String sql = "SELECT token, user_id FROM auth_tokens WHERE token = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenStr);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AuthToken(rs.getString("token"), rs.getInt("user_id"));
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get token", ex);
        }
    }

    @Override
    public void deleteToken(String tokenStr) throws DataAccessException {
        String sql = "DELETE FROM auth_tokens WHERE token = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenStr);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to delete token", ex);
        }
    }
}
