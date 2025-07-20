package server.service;

import dataaccess.UserDAO;
import dataaccess.AuthTokenDAO;
import dataaccess.DataAccessException;
import model.User;
import model.AuthToken;
import model.AuthData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class PlayerService {
    private final UserDAO userDAO;
    private final AuthTokenDAO authTokenDAO;

    public PlayerService(UserDAO userDAO, AuthTokenDAO authTokenDAO) {
        this.userDAO = userDAO;
        this.authTokenDAO = authTokenDAO;
    }

    public AuthData register(String username, String password, String email) throws Exception {
        if (username == null || password == null || email == null) {
            throw new Exception("Missing fields");
        }
        if (userDAO.getUserByUsername(username) != null) {
            throw new Exception("Username already taken");
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, hash);
        userDAO.insertUser(user);
        User dbUser = userDAO.getUserByUsername(username);

        String token = UUID.randomUUID().toString();
        AuthToken authToken = new AuthToken(token, dbUser.getId());
        authTokenDAO.insertToken(authToken);

        return new AuthData(token, username);
    }

    public AuthData login(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new Exception("Missing fields");
        }
        User user = userDAO.getUserByUsername(username);
        if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new Exception("Invalid username or password");
        }
        String token = UUID.randomUUID().toString();
        AuthToken authToken = new AuthToken(token, user.getId());
        authTokenDAO.insertToken(authToken);

        return new AuthData(token, username);
    }
}
