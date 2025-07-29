package client;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import server.Server;
import client.model.AuthData;

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
        AuthData auth = facade.register("logoutuser", "pass", "logout@email.com");
        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    public void logoutFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.logout("bogus-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("invalid or missing auth token"));
    }

    @Test
    public void createGameSuccess() throws Exception {
        AuthData auth = facade.register("gamecreator", "pass", "creator@email.com");
        assertDoesNotThrow(() -> facade.createGame(auth.authToken(), "My Game"));
    }

    @Test
    public void createGameFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.createGame("bogus-token", "My Game")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    public void listGamesSuccess() throws Exception {
        AuthData auth = facade.register("lister", "pass", "lister@email.com");
        facade.createGame(auth.authToken(), "My Game");
        String[] games = facade.listGames(auth.authToken());
        assertNotNull(games);
        assertTrue(games.length > 0);
    }

    @Test
    public void listGamesFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.listGames("bogus-token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    public void joinGameSuccess() throws Exception {
        AuthData auth = facade.register("joiner", "pass", "joiner@email.com");
        facade.createGame(auth.authToken(), "GameForJoin");

        String rawJson = facade.listGamesRaw(auth.authToken());
        com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(rawJson, com.google.gson.JsonObject.class);
        com.google.gson.JsonArray arr = obj.getAsJsonArray("games");
        int gameId = arr.get(0).getAsJsonObject().get("gameID").getAsInt();

        assertDoesNotThrow(() -> facade.joinGame(auth.authToken(), gameId, "white"));
    }

    @Test
    public void joinGameFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.joinGame("bogus-token", 1, "white")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    @Test
    public void observeGameSuccess() throws Exception {
        AuthData auth = facade.register("observer", "pass", "observer@email.com");
        facade.createGame(auth.authToken(), "GameForObserve");
        String rawJson = facade.listGamesRaw(auth.authToken());
        com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(rawJson, com.google.gson.JsonObject.class);
        com.google.gson.JsonArray arr = obj.getAsJsonArray("games");
        int gameId = arr.get(0).getAsJsonObject().get("gameID").getAsInt();
        assertDoesNotThrow(() -> facade.observeGame(auth.authToken(), gameId));
    }

    @Test
    public void observeGameFailure() throws Exception {
        Exception ex = assertThrows(Exception.class, () ->
                facade.observeGame("bogus-token", 1)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }
}
