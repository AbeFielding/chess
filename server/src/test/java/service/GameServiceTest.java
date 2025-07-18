package service;

import model.GameData;
import chess.ChessGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.service.GameService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceTest {

    GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(new ConcurrentHashMap<>(), new AtomicInteger(1));
    }

    @Test
    void createGameSucceeds() throws Exception {
        GameData game = gameService.createGame("Test Game");
        assertNotNull(game);
        assertEquals("Test Game", game.gameName());
        assertNull(game.whiteUsername());
        assertNull(game.blackUsername());
        assertNotNull(game.game());
        assertTrue(game.game() instanceof ChessGame);
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
        assertEquals("G1", games.get(0).gameName());
        assertEquals("G2", games.get(1).gameName());
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
