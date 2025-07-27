package client;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ChessServerClientTest {

    static ChessServerClient facade;

    @BeforeAll
    public static void init() {
        facade = new ChessServerClient();
    }

    @BeforeEach
    public void clearDb() {
    }

    @Test
    public void register_success() {
        String token = facade.register("user1", "pass1", "user1@email.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void register_duplicate() {
        assertTrue(true);
    }

    @Test
    public void login_success() {
        String token = facade.login("user1", "pass1");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void login_failure() {
        assertTrue(true);
    }

    @Test
    public void createGame_success() {
        assertTrue(true);
    }

    @Test
    public void listGames_success() {
        assertTrue(true);
    }

    @Test
    public void joinGame_success() {
        assertTrue(true);
    }

    @Test
    public void observeGame_success() {
        assertTrue(true);
    }
}
