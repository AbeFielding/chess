package dataaccess;

import model.AuthToken;
import model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthTokenMySQLDAOTest {

    private AuthTokenDAO dao;
    private int userId;

    @BeforeEach
    public void setUp() throws DataAccessException {
        DatabaseManager.initializeTables();
        dao = new AuthTokenMySQLDAO();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auth_tokens");
            stmt.executeUpdate("DELETE FROM users");
        } catch (Exception e) {}

        UserDAO userDAO = new UserMySQLDAO();
        User user = new User("tokenUser", "hashedPassword");
        userDAO.insertUser(user);
        this.userId = userDAO.getUserByUsername("tokenUser").getId();
    }

    @Test
    public void insertAndRetrieveToken_Positive() throws DataAccessException {
        AuthToken token = new AuthToken("token123", userId);
        dao.insertToken(token);

        AuthToken found = dao.getToken("token123");
        assertNotNull(found);
        assertEquals(userId, found.getUserId());
    }

    @Test
    public void retrieveMissingToken_Negative() throws DataAccessException {
        AuthToken found = dao.getToken("doesnotexist");
        assertNull(found);
    }

    @Test
    public void deleteToken_Positive() throws DataAccessException {
        AuthToken token = new AuthToken("tokenDelete", userId);
        dao.insertToken(token);

        dao.deleteToken("tokenDelete");
        AuthToken found = dao.getToken("tokenDelete");
        assertNull(found);
    }

    @Test
    public void deleteToken_Negative() throws DataAccessException {
        // Deleting a token that doesn't exist should not throw, but remain null
        dao.deleteToken("not_present_token");
        AuthToken found = dao.getToken("not_present_token");
        assertNull(found);
    }
}
