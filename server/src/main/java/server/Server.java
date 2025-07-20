package server;

import static spark.Spark.*;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import server.service.PlayerService;
import server.service.GameService;
import dataaccess.*;

import java.util.*;

public class Server {
    private static final Gson Gson = new Gson();
    private final UserDAO userDAO = new UserMySQLDAO();
    private final AuthTokenDAO authTokenDAO = new AuthTokenMySQLDAO();
    private final GameDAO gameDAO = new GameMySQLDAO();
    private final PlayerService playerService = new PlayerService(userDAO, authTokenDAO);
    private final GameService gameService = new GameService(gameDAO, userDAO);

    public void stop() {
        spark.Spark.stop();
        spark.Spark.awaitStop();
    }

    public int run(int desiredPort) {
        port(desiredPort);

        try {
            dataaccess.DatabaseManager.initializeTables();
        } catch (Exception ex) {
            System.err.println("Failed to initialize DB tables: " + ex.getMessage());
        }

        staticFiles.location("/web");

        exception(Exception.class, (ex, req, res) -> {
            res.type("application/json");
            res.status(500);
            res.body(Gson.toJson(new ErrorResponse("Error: " + ex.getMessage())));
        });

        registerDbEndpoint();
        registerUserEndpoints();
        registerGameEndpoints();

        awaitInitialization();
        return port();
    }

    private void registerDbEndpoint() {
        delete("/db", (req, res) -> {
            try {
                try (var conn = dataaccess.DatabaseManager.getConnection();
                     var stmt = conn.createStatement()) {
                    stmt.executeUpdate("DELETE FROM auth_tokens");
                    stmt.executeUpdate("DELETE FROM games");
                    stmt.executeUpdate("DELETE FROM users");
                    res.status(200);
                    return "{}";
                }
            } catch (Exception ex) {
                res.status(500);
                return Gson.toJson(new ErrorResponse("Error: " + ex.getMessage()));
            }
        });
    }

    private void registerUserEndpoints() {
        post("/user", (req, res) -> {
            res.type("application/json");
            RegisterRequest body = Gson.fromJson(req.body(), RegisterRequest.class);
            try {
                AuthData auth = playerService.register(body.username, body.password, body.email);
                res.status(200);
                return Gson.toJson(auth);
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Username already taken".equals(msg)) {
                    res.status(403);
                } else if ("Missing fields".equals(msg)) {
                    res.status(400);
                } else {
                    res.status(500);
                }
                return Gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });

        post("/session", (req, res) -> {
            res.type("application/json");
            LoginRequest body = Gson.fromJson(req.body(), LoginRequest.class);
            try {
                AuthData auth = playerService.login(body.username, body.password);
                res.status(200);
                return Gson.toJson(auth);
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Missing fields".equals(msg)) {
                    res.status(400);
                } else if ("Invalid username or password".equals(msg)) {
                    res.status(401);
                } else {
                    res.status(500);
                }
                return Gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });

        delete("/session", (req, res) -> {
            res.type("application/json");
            try {
                String authHeader = req.headers("Authorization");
                if (authHeader == null || authTokenDAO.getToken(authHeader) == null) {
                    res.status(401);
                    return Gson.toJson(new ErrorResponse("Error: Invalid or missing auth token"));
                }
                authTokenDAO.deleteToken(authHeader);
                res.status(200);
                return "{}";
            } catch (Exception ex) {
                res.status(500);
                return Gson.toJson(new ErrorResponse("Error: " + ex.getMessage()));
            }
        });
    }

    private void registerGameEndpoints() {
        post("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            var token = authTokenDAO.getToken(authHeader);
            if (authHeader == null || token == null) {
                res.status(401);
                return Gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }
            GameRequest gameReq = Gson.fromJson(req.body(), GameRequest.class);
            try {
                String username = userDAO.getUserById(token.getUserId()).getUsername();
                GameData game = gameService.createGame(gameReq.gameName);
                res.status(200);
                return Gson.toJson(game);
            } catch (Exception e) {
                String msg = e.getMessage();
                if ("Missing gameName".equals(msg)) {
                    res.status(400);
                } else {
                    res.status(500);
                }
                return Gson.toJson(new ErrorResponse("Error: " + msg));
            }
        });

        get("/game", (req, res) -> {
            res.type("application/json");
            try {
                String authHeader = req.headers("Authorization");
                var token = authTokenDAO.getToken(authHeader);
                if (authHeader == null || token == null) {
                    res.status(401);
                    return Gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                List<GameData> allGames = gameService.listGames();
                Map<String, Object> response = new HashMap<>();
                response.put("games", allGames);

                res.status(200);
                return Gson.toJson(response);
            } catch (Exception ex) {
                res.status(500);
                return Gson.toJson(new ErrorResponse("Error: " + ex.getMessage()));
            }
        });

        put("/game", (req, res) -> {
            res.type("application/json");
            try {
                String authHeader = req.headers("Authorization");
                var token = authTokenDAO.getToken(authHeader);
                if (authHeader == null || token == null) {
                    res.status(401);
                    return Gson.toJson(new ErrorResponse("Error: Unauthorized"));
                }
                JoinRequest joinReq = Gson.fromJson(req.body(), JoinRequest.class);
                String username = userDAO.getUserById(token.getUserId()).getUsername();
                gameService.joinGame(joinReq.gameID, joinReq.playerColor, username);
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
                return Gson.toJson(new ErrorResponse("Error: " + msg));
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
