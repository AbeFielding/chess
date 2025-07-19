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
}

