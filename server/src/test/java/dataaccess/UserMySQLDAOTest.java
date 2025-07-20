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
    public void insertUserPositive() throws DataAccessException {
        User user = new User("insertuser", "hash1");
        userDAO.insertUser(user);
        User found = userDAO.getUserByUsername("insertuser");
        assertNotNull(found);
        assertEquals("insertuser", found.getUsername());
    }

    @Test
    public void insertUserNegativeDuplicate() throws DataAccessException {
        User user = new User("dupeuser", "hash1");
        userDAO.insertUser(user);
        User dupe = new User("dupeuser", "hash2");
        assertThrows(DataAccessException.class, () -> userDAO.insertUser(dupe));
    }

    @Test
    public void getUserByUsernamePositive() throws DataAccessException {
        User user = new User("userbyusername", "hash3");
        userDAO.insertUser(user);
        User found = userDAO.getUserByUsername("userbyusername");
        assertNotNull(found);
        assertEquals("userbyusername", found.getUsername());
    }

    @Test
    public void getUserByUsernameNegative() throws DataAccessException {
        User found = userDAO.getUserByUsername("notthere");
        assertNull(found);
    }

    @Test
    public void getUserByIdPositive() throws DataAccessException {
        User user = new User("userbyid", "hash4");
        userDAO.insertUser(user);
        User foundByUsername = userDAO.getUserByUsername("userbyid");
        assertNotNull(foundByUsername);
        User foundById = userDAO.getUserById(foundByUsername.getId());
        assertNotNull(foundById);
        assertEquals("userbyid", foundById.getUsername());
    }

    @Test
    public void getUserByIdNegative() throws DataAccessException {
        User found = userDAO.getUserById(-10000);
        assertNull(found);
    }
}
