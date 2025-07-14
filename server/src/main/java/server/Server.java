package server;

import static spark.Spark.*;
import com.google.gson.Gson;
import model.UserData;
import model.GameData;
import model.AuthData;
import chess.ChessGame;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    static ConcurrentHashMap<String, UserData> users = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, AuthData> tokens = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, GameData> games = new ConcurrentHashMap<>();
    static AtomicInteger nextGameId = new AtomicInteger(1);
    static Gson gson = new Gson();

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

            if (body == null || body.username == null || body.password == null || body.email == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing username or password or email"));
            }
            if (users.containsKey(body.username)) {
                res.status(403);
                return gson.toJson(new ErrorResponse("Error: Username already taken"));
            }
            UserData newUser = new UserData(body.username, body.password, body.email);
            users.put(body.username, newUser);

            String token = UUID.randomUUID().toString();
            AuthData auth = new AuthData(token, body.username);
            tokens.put(token, auth);

            res.status(200);
            return gson.toJson(auth);
        });

        // User login
        post("/session", (req, res) -> {
            res.type("application/json");
            LoginRequest body = gson.fromJson(req.body(), LoginRequest.class);

            if (body == null || body.username == null || body.password == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing username or password"));
            }
            UserData user = users.get(body.username);
            if (user == null || !user.password().equals(body.password)) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Invalid username or password"));
            }
            String token = UUID.randomUUID().toString();
            AuthData auth = new AuthData(token, body.username);
            tokens.put(token, auth);

            res.status(200);
            return gson.toJson(auth);
        });

        // User logout
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

        // Create game
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

        // List games
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

        // Join game
        put("/game", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            AuthData auth = tokens.get(authHeader);
            if (authHeader == null || auth == null) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Error: Unauthorized"));
            }

            JoinRequest joinReq = gson.fromJson(req.body(), JoinRequest.class);

            // Validate request
            if (joinReq == null || joinReq.gameID == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Missing gameID"));
            }
            if (joinReq.playerColor != null &&
                    !("WHITE".equalsIgnoreCase(joinReq.playerColor) ||
                            "BLACK".equalsIgnoreCase(joinReq.playerColor) ||
                            "OBSERVER".equalsIgnoreCase(joinReq.playerColor))) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Invalid color"));
            }

            GameData game = games.get(joinReq.gameID);
            if (game == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Error: Invalid gameID"));
            }

            if ("WHITE".equalsIgnoreCase(joinReq.playerColor)) {
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
            } else if ("BLACK".equalsIgnoreCase(joinReq.playerColor)) {
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
