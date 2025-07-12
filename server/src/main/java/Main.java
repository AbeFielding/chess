import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    static ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, String> tokens = new ConcurrentHashMap<>();
    static Gson gson = new Gson();

    public static void main(String[] args) {
        port(8080);

        // Hello test
        get("/hello", (req, res) -> {
            res.type("application/json");
            return "{\"message\": \"Hello, Chess API!\"}";
        });

        // User registration
        post("/user", (req, res) -> {
            res.type("application/json");
            UserRequest body = gson.fromJson(req.body(), UserRequest.class);

            if (body == null || body.username == null || body.password == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing username or password"));
            }
            if (users.containsKey(body.username)) {
                res.status(403);
                return gson.toJson(new ErrorResponse("Username already taken"));
            }
            users.put(body.username, body.password);
            res.status(200);
            return gson.toJson(new UserResponse(body.username));
        });

        // User login
        post("/session", (req, res) -> {
            res.type("application/json");
            UserRequest body = gson.fromJson(req.body(), UserRequest.class);

            if (body == null || body.username == null || body.password == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing username or password"));
            }
            String correctPassword = users.get(body.username);
            if (correctPassword == null || !correctPassword.equals(body.password)) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Invalid username or password"));
            }
            String token = UUID.randomUUID().toString();
            tokens.put(token, body.username);

            res.status(200);
            return gson.toJson(new TokenResponse(token));
        });

        // User logout
        delete("/session", (req, res) -> {
            res.type("application/json");
            String authHeader = req.headers("Authorization");
            if (authHeader == null || !tokens.containsKey(authHeader)) {
                res.status(401);
                return gson.toJson(new ErrorResponse("Invalid or missing auth token"));
            }
            tokens.remove(authHeader);
            res.status(200);
            return "{}";
        });
    }

    static class UserRequest {
        String username;
        String password;
    }
    static class UserResponse {
        String username;
        UserResponse(String username) { this.username = username; }
    }
    static class TokenResponse {
        String authToken;
        TokenResponse(String authToken) { this.authToken = authToken; }
    }
    static class ErrorResponse {
        String message;
        ErrorResponse(String message) { this.message = message; }
    }
}
