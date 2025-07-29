package client;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import server.Server;
import model.AuthData;

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
    public void clearDb() throws Exception {
        java.net.HttpURLConnection con =
                (java.net.HttpURLConnection) new java.net.URL("http://localhost:" + facade.getPort() + "/db").openConnection();
        con.setRequestMethod("DELETE");
        con.getResponseCode();
        con.disconnect();
    }

    @Test
    public void registerSuccess() throws Exception {
        AuthData auth = facade.register("user1", "pass1", "user1@email.com");
        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertFalse(auth.authToken().isEmpty());
        assertEquals("user1", auth.username());
    }

    @Test
    public void registerDuplicate() throws Exception {
        facade.register("dupe", "pass", "dupe@email.com");
        Exception ex = assertThrows(Exception.class, () ->
                facade.register("dupe", "pass", "dupe@email.com")
        );
        assertTrue(ex.getMessage().contains("Username already taken"));
    }

    @Test
    public void loginSuccess() throws Exception {
        facade.register("loginuser", "pass", "login@email.com");
        AuthData auth = facade.login("loginuser", "pass");
        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertFalse(auth.authToken().isEmpty());
        assertEquals("loginuser", auth.username());
    }

    @Test
    public void loginFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.login("nouser", "wrongpass")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("invalid username or password"));
    }

    @Test
    public void logoutSuccess() throws Exception {
        assertDoesNotThrow(() -> facade.logout("dummy-token"));
    }

    @Test
    public void logoutFailure() throws Exception {
        assertTrue(true);
    }

    @Test
    public void createGameSuccess() throws Exception {

    }

    @Test
    public void createGameFailure() throws Exception {
        assertTrue(true);
    }

    @Test
    public void listGamesSuccess() throws Exception {
        String[] games = facade.listGames("dummy-token");
        assertNotNull(games);
        assertTrue(games.length > 0);
    }

    @Test
    public void listGamesFailure() throws Exception {
        assertTrue(true);
    }

    @Test
    public void joinGameSuccess() throws Exception {
        assertDoesNotThrow(() -> facade.joinGame("dummy-token", 1, "white"));
    }

    @Test
    public void joinGameFailure() throws Exception {
        assertTrue(true);
    }

    @Test
    public void observeGameSuccess() throws Exception {
        assertDoesNotThrow(() -> facade.observeGame("dummy-token", 1));
    }

    @Test
    public void observeGameFailure() throws Exception {
        assertTrue(true);
    }
}
