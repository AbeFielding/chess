package server;

import static spark.Spark.*;
import com.google.gson.Gson;
import model.UserData;
import model.GameData;
import model.AuthData;
import chess.ChessGame;
import server.service.PlayerService;
import server.service.GameService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    static ConcurrentHashMap<String, UserData> users = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, AuthData> tokens = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, GameData> games = new ConcurrentHashMap<>();
    static AtomicInteger nextGameId = new AtomicInteger(1);
    static Gson gson = new Gson();

    PlayerService playerService = new PlayerService(users, tokens);
    GameService gameService = new GameService(games, nextGameId);

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
    }

    public int run(int desiredPort) {
        port(desiredPort);
        staticFiles.location("/web");

        registerDbEndpoint();
        registerUserEndpoints();
        registerGameEndpoints();

        awaitInitialization();
        return port();
    }

    private void registerDbEndpoint() {
        delete("/db", (req, res) -> {
            users.clear();
            tokens.clear();
            games.clear();
            nextGameId.set(1);
            res.status(200);
            return "{}";
        });
    }

    private void registerUserEndpoints() {
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
    }

    private void registerGameEndpoints() {
        post("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            GameRequest gameReq = gson.fromJson(req.body(), GameRequest.class);
            try {
                GameData game = gameService.createGame(gameReq.gameName);
                res.status(200);
                return gson.toJson(game);
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: " + e.getMessage()));
            }
        });

        get("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            List<GameData> allGames = gameService.listGames();
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
            try {
                gameService.joinGame(joinReq.gameID, joinReq.playerColor, auth.username());
                res.status(200);
                return "{}";
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Missing gameID".equals(msg) || "Missing playerColor".equals(msg) ||
                        "Invalid gameID".equals(msg) || "Invalid color".equals(msg)) {
                    res.status(400);
                } else if ("Color already taken".equals(msg)) {
                    res.status(403);
                } else {
                    res.status(500);
                }
                return gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });
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
