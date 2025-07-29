package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import model.AuthData;

public class ServerFacade {
    public int port;
    private final Gson gson = new Gson();

    public int getPort() {
        return port;
    }

    public ServerFacade() {
        this.port = 8080;
    }

    public ServerFacade(int port) {
        this.port = port;
    }

    public AuthData register(String username, String password, String email) throws IOException {
        URL url = new URL("http://localhost:" + port + "/user");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JsonObject req = new JsonObject();
        req.addProperty("username", username);
        req.addProperty("password", password);
        req.addProperty("email", email);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(gson.toJson(req).getBytes());
        }

        int status = connection.getResponseCode();
        InputStream responseStream = (status == 200)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
            String response = br.lines().reduce("", (a, b) -> a + b);
            if (status == 200) {
                return gson.fromJson(response, AuthData.class);
            } else {
                throw new IOException("Register failed: " + response);
            }
        }
    }

    public AuthData login(String username, String password) throws IOException {
        URL url = new URL("http://localhost:" + port + "/session");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JsonObject req = new JsonObject();
        req.addProperty("username", username);
        req.addProperty("password", password);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(gson.toJson(req).getBytes());
        }

        int status = connection.getResponseCode();
        InputStream responseStream = (status == 200)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
            String response = br.lines().reduce("", (a, b) -> a + b);
            if (status == 200) {
                return gson.fromJson(response, AuthData.class);
            } else {
                throw new IOException("Login failed: " + response);
            }
        }
    }



    public void logout(String authToken) throws IOException {
        // Put real HTTP DELETE request to /session
    }

    public String[] listGames(String authToken) throws IOException {
        // Put real HTTP DELETE request to /session
        return new String[]{"Game 1", "Game 2"};
    }

    public void createGame(String authToken, String gameName) throws IOException {
        // Put real HTTP DELETE request to /session
    }

    public void joinGame(String authToken, int gameId, String color) throws IOException {
        // Put real HTTP DELETE request to /session
    }

    public void observeGame(String authToken, int gameId) throws IOException {
        // Put real HTTP DELETE request to /session
    }
}
