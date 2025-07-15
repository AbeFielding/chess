package server.service;

import model.AuthData;
import model.GameData;
import chess.ChessGame;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class GameService {
    private final Map<Integer, GameData> games;
    private final AtomicInteger nextGameId;

    public GameService(Map<Integer, GameData> games, AtomicInteger nextGameId) {
        this.games = games;
        this.nextGameId = nextGameId;
    }

    public GameData createGame(String gameName) throws Exception {
        if (gameName == null) {
            throw new Exception("Missing gameName");
        }
        int gameID = nextGameId.getAndIncrement();
        ChessGame chessGame = new ChessGame();
        GameData game = new GameData(gameID, gameName, null, null, chessGame);
        games.put(gameID, game);
        return game;
    }

    public List<GameData> listGames() {
        return new ArrayList<>(games.values());
    }

    public void joinGame(Integer gameID, String playerColor, String username) throws Exception {
        if (gameID == null) {
            throw new Exception("Missing gameID");
        }
        if (playerColor == null || playerColor.isBlank()) {
            throw new Exception("Missing playerColor");
        }
        String color = playerColor.trim().toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK") && !color.equals("OBSERVER")) {
            throw new Exception("Invalid color");
        }
        GameData game = games.get(gameID);
        if (game == null) {
            throw new Exception("Invalid gameID");
        }
        if (color.equals("WHITE")) {
            if (game.whiteUsername() != null && !game.whiteUsername().equals(username)) {
                throw new Exception("Color already taken");
            }
            games.put(gameID, new GameData(
                    game.gameID(),
                    game.gameName(),
                    username,
                    game.blackUsername(),
                    game.game()
            ));
        } else if (color.equals("BLACK")) {
            if (game.blackUsername() != null && !game.blackUsername().equals(username)) {
                throw new Exception("Color already taken");
            }
            games.put(gameID, new GameData(
                    game.gameID(),
                    game.gameName(),
                    game.whiteUsername(),
                    username,
                    game.game()
            ));
        }
    }
}
