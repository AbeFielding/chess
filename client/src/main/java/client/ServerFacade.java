package client;

public class ServerFacade {
    private int port;

    public ServerFacade(int port) {
        this.port = port;
    }

    public String register(String username, String password, String email) {
        // Replace with real call
        return "dummy-auth-token";
    }

    public String login(String username, String password) {
        // Replace with real call
        return "dummy-auth-token";
    }

    public void logout(String authToken) {
        // Replace with real call
    }

    public String[] listGames(String authToken) {
        // Replace with real call
        return new String[]{"Game 1", "Game 2"};
    }

    public void createGame(String authToken, String gameName) {
        // Replace with real call
    }

    public void joinGame(String authToken, int gameId, String color) {
        // Replace with real call
    }

    public void observeGame(String authToken, int gameId) {
        // Replace with real call
    }
}