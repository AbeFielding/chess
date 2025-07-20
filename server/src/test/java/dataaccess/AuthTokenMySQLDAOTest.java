package dataaccess;

import model.AuthToken;
import model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthTokenMySQLDAOTest {

    private AuthTokenDAO dao;
    private UserDAO userDAO;
    private int userId;

    @BeforeEach
    public void setUp() throws DataAccessException {
        DatabaseManager.initializeTables();
        dao = new AuthTokenMySQLDAO();
        userDAO = new UserMySQLDAO();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auth_tokens");
            stmt.executeUpdate("DELETE FROM users");
        } catch (Exception e) {}
        User user = new User("tokenUser", "hashedPassword");
        userDAO.insertUser(user);
        this.userId = userDAO.getUserByUsername("tokenUser").getId();
    }

    @Test
    public void insertAndRetrieveToken() throws DataAccessException {
        AuthToken token = new AuthToken("token123", userId);
        dao.insertToken(token);

        AuthToken found = dao.getToken("token123");
        assertNotNull(found);
        assertEquals(userId, found.getUserId());
    }

    @Test
    public void retrieveMissingTokenReturnsNull() throws DataAccessException {
        AuthToken found = dao.getToken("doesnotexist");
        assertNull(found);
    }

    @Test
    public void deleteTokenWorks() throws DataAccessException {
        AuthToken token = new AuthToken("tokenDelete", userId);
        dao.insertToken(token);

        dao.deleteToken("tokenDelete");
        AuthToken found = dao.getToken("tokenDelete");
        assertNull(found);
    }
}
