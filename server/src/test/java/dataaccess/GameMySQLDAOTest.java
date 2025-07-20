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
    public void insertGamePositive() throws DataAccessException {
        Game game = new Game("{}", false, null, null, "TestGame");
        int id = gameDAO.insertGame(game);
        assertTrue(id > 0);
        Game found = gameDAO.getGameById(id);
        assertNotNull(found);
        assertEquals("TestGame", found.getGameName());
    }

    @Test
    public void insertGameNegativeNullState() {
        Game badGame = new Game(null, false, null, null, "NullState");
        assertThrows(DataAccessException.class, () -> gameDAO.insertGame(badGame));
    }

    @Test
    public void getGameByIdPositive() throws DataAccessException {
        Game game = new Game("{}", false, null, null, "GetByIdGame");
        int id = gameDAO.insertGame(game);
        Game found = gameDAO.getGameById(id);
        assertNotNull(found);
        assertEquals("GetByIdGame", found.getGameName());
    }

    @Test
    public void getGameByIdNegative() throws DataAccessException {
        Game found = gameDAO.getGameById(-9999);
        assertNull(found);
    }

    @Test
    public void updateGameStatePositive() throws DataAccessException {
        Game game = new Game("{}", false, null, null, "ToUpdate");
        int id = gameDAO.insertGame(game);
        gameDAO.updateGameState(id, "{\"updated\":true}", true);
        Game updated = gameDAO.getGameById(id);
        assertEquals("{\"updated\":true}", updated.getState());
        assertTrue(updated.isFinished());
    }

    @Test
    public void updateGameStateNegative() {
        assertThrows(DataAccessException.class, () ->
                gameDAO.updateGameState(-12345, "{\"fail\":true}", true)
        );
    }

    @Test
    public void listGamesPositive() throws DataAccessException {
        Game game1 = new Game("{\"a\":1}", false, null, null, "A");
        Game game2 = new Game("{\"b\":2}", false, null, null, "B");
        gameDAO.insertGame(game1);
        gameDAO.insertGame(game2);
        List<Game> games = gameDAO.listGames();
        assertTrue(games.size() >= 2);
    }

    @Test
    public void listGamesNegative() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM games");
        } catch (Exception e) {}
        List<Game> games = gameDAO.listGames();
        assertNotNull(games);
        assertEquals(0, games.size());
    }
}
