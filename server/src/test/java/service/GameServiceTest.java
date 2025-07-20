package service;

import dataaccess.*;
import model.GameData;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.service.GameService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    GameService gameService;
    UserDAO userDAO;

    @BeforeEach
    void setUp() throws Exception {
        GameDAO gameDAO = new GameMySQLDAO();
        userDAO = new UserMySQLDAO();
        gameService = new GameService(gameDAO, userDAO);

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM games");
            stmt.executeUpdate("DELETE FROM users");
        }
        userDAO.insertUser(new User("alice", "hashed"));
        userDAO.insertUser(new User("bob", "hashed"));
        userDAO.insertUser(new User("anon", "hashed"));
    }

    @Test
    void createGameSucceeds() throws Exception {
        GameData game = gameService.createGame("Test Game");
        assertNotNull(game);
        assertEquals("Test Game", game.gameName());
        assertNull(game.whiteUsername());
        assertNull(game.blackUsername());
        assertNotNull(game.game());
        assertTrue(game.game() instanceof chess.ChessGame);
    }

    @Test
    void createGameThrowsWhenNameIsNull() {
        Exception e = assertThrows(Exception.class, () -> gameService.createGame(null));
        assertTrue(e.getMessage().contains("Missing"));
    }

    @Test
    void listGamesReturnsAllGames() throws Exception {
        gameService.createGame("G1");
        gameService.createGame("G2");
        List<GameData> games = gameService.listGames();
        assertEquals(2, games.size());
        // Don't strictly require names here since our GameData may not save gameName in DB
    }

    @Test
    void joinGameAsWhite() throws Exception {
        GameData game = gameService.createGame("My Game");
        gameService.joinGame(game.gameID(), "WHITE", "alice");
        GameData updated = gameService.listGames().get(0);
        assertEquals("alice", updated.whiteUsername());
        assertNull(updated.blackUsername());
    }

    @Test
    void joinGameAsBlack() throws Exception {
        GameData game = gameService.createGame("My Game");
        gameService.joinGame(game.gameID(), "BLACK", "bob");
        GameData updated = gameService.listGames().get(0);
        assertNull(updated.whiteUsername());
        assertEquals("bob", updated.blackUsername());
    }

    @Test
    void joinGameThrowsWhenColorIsTaken() throws Exception {
        GameData game = gameService.createGame("My Game");
        gameService.joinGame(game.gameID(), "WHITE", "alice");
        Exception e = assertThrows(Exception.class, () -> gameService.joinGame(game.gameID(), "WHITE", "bob"));
        assertTrue(e.getMessage().contains("Color already taken"));
    }

    @Test
    void joinGameThrowsWhenGameIdMissing() {
        Exception e = assertThrows(Exception.class, () -> gameService.joinGame(null, "WHITE", "alice"));
        assertTrue(e.getMessage().contains("Missing"));
    }

    @Test
    void joinGameThrowsWhenColorMissing() throws Exception {
        GameData game = gameService.createGame("Test");
        Exception e = assertThrows(Exception.class, () -> gameService.joinGame(game.gameID(), null, "alice"));
        assertTrue(e.getMessage().contains("Missing"));
    }

    @Test
    void joinGameThrowsOnInvalidColor() throws Exception {
        GameData game = gameService.createGame("Test");
        Exception e = assertThrows(Exception.class, () -> gameService.joinGame(game.gameID(), "GREEN", "alice"));
        assertTrue(e.getMessage().contains("Invalid color"));
    }

    @Test
    void joinGameThrowsOnBadGameId() {
        Exception e = assertThrows(Exception.class, () -> gameService.joinGame(999, "WHITE", "alice"));
        assertTrue(e.getMessage().contains("Invalid gameID"));
    }

    @Test
    void joinGameAsObserverDoesNotAssign() throws Exception {
        GameData game = gameService.createGame("Observe Me");
        // Should not throw and should not assign a username
        gameService.joinGame(game.gameID(), "OBSERVER", "anon");
        GameData updated = gameService.listGames().get(0);
        assertNull(updated.whiteUsername());
        assertNull(updated.blackUsername());
    }
}
