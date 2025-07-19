package dataaccess;

import model.AuthToken;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthTokenMySQLDAOTest {

    private AuthTokenDAO dao;

    @BeforeEach
    public void setUp() throws DataAccessException {
        DatabaseManager.initializeTables();
        dao = new AuthTokenMySQLDAO();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auth_tokens");
        } catch (Exception e) {}
    }

    @Test
    public void insertAndRetrieveToken() throws DataAccessException {
        AuthToken token = new AuthToken("token123", 1);
        dao.insertToken(token);

        AuthToken found = dao.getToken("token123");
        assertNotNull(found);
        assertEquals(1, found.getUserId());
    }

    @Test
    public void retrieveMissingTokenReturnsNull() throws DataAccessException {
        AuthToken found = dao.getToken("doesnotexist");
        assertNull(found);
    }

    @Test
    public void deleteTokenWorks() throws DataAccessException {
        AuthToken token = new AuthToken("tokenDelete", 2);
        dao.insertToken(token);

        dao.deleteToken("tokenDelete");
        AuthToken found = dao.getToken("tokenDelete");
        assertNull(found);
    }
}
