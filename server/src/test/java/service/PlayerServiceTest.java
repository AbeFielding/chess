package service;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.service.PlayerService;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PlayerServiceTest {

    PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @Test
    void registerPositive() throws Exception {
        AuthData auth = playerService.register("bob", "pw", "bob@mail.com");
        assertNotNull(auth);
        assertEquals("bob", auth.username());
    }

    @Test
    void registerNegativeDuplicateUsername() throws Exception {
        playerService.register("bob", "pw", "bob@mail.com");
        Exception e = assertThrows(Exception.class, () ->
                playerService.register("bob", "pw2", "bob2@mail.com"));
        assertTrue(e.getMessage().contains("taken"));
    }

    @Test
    void registerNegativeMissingFields() {
        Exception e = assertThrows(Exception.class, () ->
                playerService.register(null, "pw", "bob@mail.com"));
        assertTrue(e.getMessage().contains("Missing"));

        Exception e2 = assertThrows(Exception.class, () ->
                playerService.register("bob", null, "bob@mail.com"));
        assertTrue(e2.getMessage().contains("Missing"));

        Exception e3 = assertThrows(Exception.class, () ->
                playerService.register("bob", "pw", null));
        assertTrue(e3.getMessage().contains("Missing"));
    }

    @Test
    void loginPositive() throws Exception {
        playerService.register("alice", "pw", "alice@mail.com");
        AuthData auth = playerService.login("alice", "pw");
        assertNotNull(auth);
        assertEquals("alice", auth.username());
    }

    @Test
    void loginNegativeWrongPassword() throws Exception {
        playerService.register("alice", "pw", "alice@mail.com");
        Exception e = assertThrows(Exception.class, () ->
                playerService.login("alice", "wrongpw"));
        assertTrue(e.getMessage().contains("Invalid"));
    }

    @Test
    void loginNegativeNonexistentUser() {
        Exception e = assertThrows(Exception.class, () ->
                playerService.login("nosuchuser", "pw"));
        assertTrue(e.getMessage().contains("Invalid"));
    }

    @Test
    void loginNegativeMissingFields() {
        Exception e = assertThrows(Exception.class, () ->
                playerService.login(null, "pw"));
        assertTrue(e.getMessage().contains("Missing"));

        Exception e2 = assertThrows(Exception.class, () ->
                playerService.login("alice", null));
        assertTrue(e2.getMessage().contains("Missing"));
    }
}
