import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    static ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();
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

            // Validate
            if (body == null || body.username == null || body.password == null) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Missing username or password"));
            }
            // is username taken
            if (users.containsKey(body.username)) {
                res.status(403);
                return gson.toJson(new ErrorResponse("Username already taken"));
            }
            // Register user (will improve later)
            users.put(body.username, body.password);

            res.status(200);
            return gson.toJson(new UserResponse(body.username));
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
    static class ErrorResponse {
        String message;
        ErrorResponse(String message) { this.message = message; }
    }
}