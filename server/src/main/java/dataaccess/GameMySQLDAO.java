package dataaccess;

import model.Game;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameMySQLDAO implements GameDAO {

    @Override
    public int insertGame(Game game) throws DataAccessException {
        String sql = "INSERT INTO games (state, finished, white_user_id, black_user_id, game_name) VALUES (?, ?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, game.getState());
            ps.setBoolean(2, game.isFinished());
            if (game.getWhiteUserId() != null) ps.setInt(3, game.getWhiteUserId());
            else ps.setNull(3, java.sql.Types.INTEGER);
            if (game.getBlackUserId() != null) ps.setInt(4, game.getBlackUserId());
            else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, game.getGameName());
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new DataAccessException("Failed to get generated ID for game");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to insert game", ex);
        }
    }

    @Override
    public Game getGameById(int id) throws DataAccessException {
        String sql = "SELECT id, state, finished, white_user_id, black_user_id, game_name FROM games WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Game(
                            rs.getInt("id"),
                            rs.getString("state"),
                            rs.getBoolean("finished"),
                            (Integer) rs.getObject("white_user_id"),
                            (Integer) rs.getObject("black_user_id"),
                            rs.getString("game_name")
                    );
                }
                return null;
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to get game by id", ex);
        }
    }

    @Override
    public List<Game> listGames() throws DataAccessException {
        String sql = "SELECT id, state, finished, white_user_id, black_user_id, game_name FROM games";
        List<Game> games = new ArrayList<>();
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                games.add(new Game(
                        rs.getInt("id"),
                        rs.getString("state"),
                        rs.getBoolean("finished"),
                        (Integer) rs.getObject("white_user_id"),
                        (Integer) rs.getObject("black_user_id"),
                        rs.getString("game_name")
                ));
            }
            return games;
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to list games", ex);
        }
    }

    @Override
    public void updateGameState(int gameId, String state, boolean finished) throws DataAccessException {
        String sql = "UPDATE games SET state = ?, finished = ? WHERE id = ?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, state);
            ps.setBoolean(2, finished);
            ps.setInt(3, gameId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DataAccessException("Game not found for update");
            }
        } catch (SQLException ex) {
            throw new DataAccessException("Unable to update game state", ex);
        }
    }
}
