package server.service;

import model.AuthData;
import model.UserData;
import java.util.Map;
import java.util.UUID;

public class PlayerService {
    private final Map<String, UserData> users;
    private final Map<String, AuthData> tokens;

    public PlayerService(Map<String, UserData> users, Map<String, AuthData> tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        if (username == null || password == null || email == null) {
            throw new Exception("Missing fields");
        }
        if (users.containsKey(username)) {
            throw new Exception("Username already taken");
        }
        UserData user = new UserData(username, password, email);
        users.put(username, user);

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        tokens.put(token, auth);

        return auth;
    }

    public AuthData login(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new Exception("Missing fields");
        }
        UserData user = users.get(username);
        if (user == null || !user.password().equals(password)) {
            throw new Exception("Invalid username or password");
        }
        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);
        tokens.put(token, auth);

        return auth;
    }
}
