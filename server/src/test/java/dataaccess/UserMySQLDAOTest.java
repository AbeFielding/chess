package dataaccess;

import model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserMySQLDAOTest {

    private UserDAO userDAO;

    @BeforeEach
    public void setUp() throws DataAccessException {
        DatabaseManager.initializeTables();
        userDAO = new UserMySQLDAO();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM users");
        } catch (Exception e) {}
    }

    @Test
    public void insertAndRetrieveUser() throws DataAccessException {
        String username = "testuser";
        String hash = "hashedPassword";
        User user = new User(username, hash);
        userDAO.insertUser(user);

        User retrieved = userDAO.getUserByUsername(username);
        assertNotNull(retrieved);
        assertEquals(username, retrieved.getUsername());
        assertEquals(hash, retrieved.getPasswordHash());
    }

    @Test
    public void insertDuplicateUserFails() throws DataAccessException {
        String username = "testuser2";
        String hash = "hashedPassword";
        User user1 = new User(username, hash);
        User user2 = new User(username, hash + "X");

        userDAO.insertUser(user1);
        assertThrows(DataAccessException.class, () -> {
            userDAO.insertUser(user2);
        });
    }
}
