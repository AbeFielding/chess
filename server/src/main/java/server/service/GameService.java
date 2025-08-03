package server.service;

import dataaccess.GameDAO;
import dataaccess.UserDAO;
import dataaccess.DataAccessException;
import model.Game;
import model.User;
import model.GameData;
import chess.ChessGame;
import com.google.gson.Gson;
import java.util.List;
import java.util.stream.Collectors;

public class GameService {
    private final GameDAO gameDAO;
    private final UserDAO userDAO;
    private final Gson gson = new Gson();

    public GameService(GameDAO gameDAO, UserDAO userDAO) {
        this.gameDAO = gameDAO;
        this.userDAO = userDAO;
    }

    public GameData createGame(String gameName) throws Exception {
        if (gameName == null) {
            throw new Exception("Missing gameName");
        }
        ChessGame chessGame = new ChessGame();
        String stateJson = gson.toJson(chessGame);
        Game game = new Game(stateJson, false, null, null, gameName); // Always pass gameName!
        int gameId = gameDAO.insertGame(game);
        return toGameData(gameDAO.getGameById(gameId), gameName);
    }

    public List<GameData> listGames() throws DataAccessException {
        return gameDAO.listGames().stream()
                .map(g -> toGameData(g, null))
                .collect(Collectors.toList());
    }

    public void joinGame(Integer gameID, String playerColor, String username) throws Exception {
        if (gameID == null) {
            throw new Exception("Missing gameID");
        }

        Game game = gameDAO.getGameById(gameID);
        if (game == null) {
            throw new Exception("Invalid gameID");
        }

        if (playerColor == null || playerColor.isBlank()) {
            throw new Exception("Missing playerColor");
        }

        String color = playerColor.trim().toUpperCase();
        if (color.equals("OBSERVER")) {
            return;
        }

        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            throw new Exception("Invalid color");
        }

        int userId = userDAO.getUserByUsername(username).getId();

        if (color.equals("WHITE")) {
            Integer currentWhite = game.getWhiteUserId();
            if (currentWhite != null && !currentWhite.equals(userId)) {
                throw new Exception("Color already taken");
            }
            if (currentWhite == null) {
                updatePlayerSlot("white_user_id", userId, gameID);
            }
        } else if (color.equals("BLACK")) {
            Integer currentBlack = game.getBlackUserId();
            if (currentBlack != null && !currentBlack.equals(userId)) {
                throw new Exception("Color already taken");
            }
            if (currentBlack == null) {
                updatePlayerSlot("black_user_id", userId, gameID);
            }
        }
    }

    private void updatePlayerSlot(String column, int userId, int gameID) throws DataAccessException {
        String sql = "UPDATE games SET " + column + " = ? WHERE id = ?";
        try (var conn = dataaccess.DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameID);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Failed to update player slot", e);
        }
    }

    private GameData toGameData(Game game, String gameNameOverride) {
        ChessGame chessGame = gson.fromJson(game.getState(), ChessGame.class);
        String whiteUsername = null;
        String blackUsername = null;
        try {
            if (game.getWhiteUserId() != null) {
                User whiteUser = userDAO.getUserById(game.getWhiteUserId());
                if (whiteUser != null) {
                    whiteUsername = whiteUser.getUsername();
                }
            }
            if (game.getBlackUserId() != null) {
                User blackUser = userDAO.getUserById(game.getBlackUserId());
                if (blackUser != null) {
                    blackUsername = blackUser.getUsername();
                }
            }
        } catch (DataAccessException e) {
            //
        }
        return new GameData(
                game.getId(),
                game.getGameName(),
                whiteUsername,
                blackUsername,
                chessGame
        );
    }
}
