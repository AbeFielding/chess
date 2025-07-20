package dataaccess;

import model.Game;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GameMySQLDAOTest {

    private GameDAO gameDAO;

    @BeforeEach
    public void setUp() throws DataAccessException {
        DatabaseManager.initializeTables();
        gameDAO = new GameMySQLDAO();
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM games");
        } catch (Exception e) {}
    }

    @Test
    public void insertAndRetrieveGame_Positive() throws DataAccessException {
        Game game = new Game("{}", false, null, null, "MyGame");
        int id = gameDAO.insertGame(game);

        Game retrieved = gameDAO.getGameById(id);
        assertNotNull(retrieved);
        assertEquals("{}", retrieved.getState());
        assertFalse(retrieved.isFinished());
        assertEquals("MyGame", retrieved.getGameName());
    }

    @Test
    public void getMissingGame_Negative() throws DataAccessException {
        Game game = gameDAO.getGameById(-99999);
        assertNull(game);
    }

    @Test
    public void updateGameState_Positive() throws DataAccessException {
        Game game = new Game("{}", false, null, null, "Updatable");
        int id = gameDAO.insertGame(game);

        gameDAO.updateGameState(id, "{\"updated\":true}", true);
        Game updated = gameDAO.getGameById(id);

        assertEquals("{\"updated\":true}", updated.getState());
        assertTrue(updated.isFinished());
    }

    @Test
    public void updateGameState_Negative() {
        assertThrows(DataAccessException.class, () -> {
            gameDAO.updateGameState(-2222, "{\"fail\":true}", true);
        });
    }

    @Test
    public void listGames_Positive() throws DataAccessException {
        Game game1 = new Game("{\"a\":1}", false, null, null, "A");
        Game game2 = new Game("{\"b\":2}", false, null, null, "B");
        gameDAO.insertGame(game1);
        gameDAO.insertGame(game2);

        List<Game> games = gameDAO.listGames();
        assertTrue(games.size() >= 2);
    }
}
