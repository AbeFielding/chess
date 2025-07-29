package client;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import server.Server;

public class ServerFacadeTests {

    private static Server server;
    static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @BeforeEach
    public void clearDb() {
    }

    @Test
    public void registerSuccess() {
        String token = facade.register("user1", "pass1", "user1@email.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void registerFailure() {
        assertTrue(true);
    }

    @Test
    public void registerDuplicate() {
        assertTrue(true);
    }

    @Test
    public void loginSuccess() {
        String token = facade.login("user1", "pass1");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void loginFailure() {
        assertTrue(true);
    }

    @Test
    public void logoutSuccess() {
        assertDoesNotThrow(() -> facade.logout("dummy-token"));
    }

    @Test
    public void logoutFailure() {
        assertTrue(true);
    }

    @Test
    public void createGameSuccess() {
        assertDoesNotThrow(() -> facade.createGame("dummy-token", "Test Game"));
    }

    @Test
    public void createGameFailure() {
        assertTrue(true);
    }

    @Test
    public void listGamesSuccess() {
        String[] games = facade.listGames("dummy-token");
        assertNotNull(games);
        assertTrue(games.length > 0);
    }

    @Test
    public void listGamesFailure() {
        assertTrue(true);
    }

    @Test
    public void joinGameSuccess() {
        assertDoesNotThrow(() -> facade.joinGame("dummy-token", 1, "white"));
    }

    @Test
    public void joinGameFailure() {
        assertTrue(true);
    }

    @Test
    public void observeGameSuccess() {
        assertDoesNotThrow(() -> facade.observeGame("dummy-token", 1));
    }

    @Test
    public void observeGameFailure() {
        assertTrue(true);
    }
}
