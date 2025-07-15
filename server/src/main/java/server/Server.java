package server;

import static spark.Spark.*;
import com.google.gson.Gson;
import model.UserData;
import model.GameData;
import model.AuthData;
import chess.ChessGame;
import server.service.PlayerService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    static ConcurrentHashMap<String, UserData> users = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, AuthData> tokens = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, GameData> games = new ConcurrentHashMap<>();
    static AtomicInteger nextGameId = new AtomicInteger(1);
    static Gson gson = new Gson();

    // Add your PlayerService instance
    PlayerService playerService = new PlayerService(users, tokens);

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
    }

    public int run(int desiredPort) {
        port(desiredPort);
        staticFiles.location("/web");

        delete("/db", (req, res) -> {
            users.clear();
            tokens.clear();
            games.clear();
            nextGameId.set(1);
            res.status(200);
            return "{}";
        });

        // User registration
        post("/user", (req, res) -> {
            res.type("application/json");
            RegisterRequest body = gson.fromJson(req.body(), RegisterRequest.class);

            try {
                AuthData auth = playerService.register(body.username, body.password, body.email);
                res.status(200);
                return gson.toJson(auth);
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Username already taken".equals(msg)) {
                    res.status(403);
                } else if ("Missing fields".equals(msg)) {
                    res.status(400);
                } else {
                    res.status(500);
                }
                return gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });

        // User login
        post("/session", (req, res) -> {
            res.type("application/json");
            LoginRequest body = gson.fromJson(req.body(), LoginRequest.class);

            try {
                AuthData auth = playerService.login(body.username, body.password);
                res.status(200);
                return gson.toJson(auth);
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Missing fields".equals(msg)) {
                    res.status(400);
                } else if ("Invalid username or password".equals(msg)) {
                    res.status(401);
                } else {
                    res.status(500);
                }
                return gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });

        delete("/session", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            if (authHeader == null || !tokens.containsKey(authHeader)) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Invalid or missing auth token"));
            }
            tokens.remove(authHeader);
            res.status(200);
            return "{}";
        });

        post("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            GameRequest gameReq = gson.fromJson(req.body(), GameRequest.class);
            if (gameReq == null || gameReq.gameName == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing gameName"));
            }
            int gameID = nextGameId.getAndIncrement();
            ChessGame chessGame = new ChessGame();
            GameData game = new GameData(gameID, gameReq.gameName, null, null, chessGame);
            games.put(gameID, game);

            res.status(200);
            return gson.toJson(game);
        });

        get("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            List<GameData> allGames = new ArrayList<>(games.values());
            Map<String, Object> response = new HashMap<>();
            response.put("games", allGames);

            res.status(200);
            return gson.toJson(response);
        });

        put("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }

            JoinRequest joinReq = gson.fromJson(req.body(), JoinRequest.class);

            if (joinReq == null || joinReq.gameID == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing gameID"));
            }

            if (joinReq.playerColor == null || joinReq.playerColor.isBlank()) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing playerColor"));
            }
            String color = joinReq.playerColor.trim().toUpperCase();
            if (!color.equals("WHITE") && !color.equals("BLACK") && !color.equals("OBSERVER")) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Invalid color"));
            }

            GameData game = games.get(joinReq.gameID);
            if (game == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Invalid gameID"));
            }

            if (color.equals("WHITE")) {
                if (game.whiteUsername() != null && !game.whiteUsername().equals(auth.username())) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Error: Color already taken"));
                }
                games.put(joinReq.gameID, new GameData(
                        game.gameID(),
                        game.gameName(),
                        auth.username(),
                        game.blackUsername(),
                        game.game()
                ));
            } else if (color.equals("BLACK")) {
                if (game.blackUsername() != null && !game.blackUsername().equals(auth.username())) {
                    res.status(403);
                    return gson.toJson(new ErrorResponse("Error: Color already taken"));
                }
                games.put(joinReq.gameID, new GameData(
                        game.gameID(),
                        game.gameName(),
                        game.whiteUsername(),
                        auth.username(),
                        game.game()
                ));
            }

            res.status(200);
            return "{}";
        });

        awaitInitialization();
        return port();
    }
    static class RegisterRequest {
        String username;
        String password;
        String email;
    }
    static class LoginRequest {
        String username;
        String password;
    }
    static class GameRequest {
        String gameName;
    }
    static class ErrorResponse {
        String message;
        ErrorResponse(String message) { this.message = message; }
    }
    static class JoinRequest {
        String playerColor;
        Integer gameID;
    }
}
