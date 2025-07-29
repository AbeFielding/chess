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
        URL url = new URL("http://localhost:" + port + "/session");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", authToken);
        connection.connect();

        int status = connection.getResponseCode();
        InputStream responseStream = (status == 200)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
            String response = br.lines().reduce("", (a, b) -> a + b);
            if (status == 200) {
                return;
            } else {
                throw new IOException("Logout failed: " + response);
            }
        }
    }

    public String[] listGames(String authToken) throws IOException {
        URL url = new URL("http://localhost:" + port + "/game");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.connect();

        int status = connection.getResponseCode();
        InputStream responseStream = (status == 200)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
            String response = br.lines().reduce("", (a, b) -> a + b);
            if (status == 200) {
                com.google.gson.JsonObject obj = gson.fromJson(response, com.google.gson.JsonObject.class);
                com.google.gson.JsonArray arr = obj.getAsJsonArray("games");
                String[] games = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    games[i] = arr.get(i).getAsJsonObject().get("gameName").getAsString();
                }
                return games;
            } else {
                throw new IOException("List games failed: " + response);
            }
        }
    }

    public void createGame(String authToken, String gameName) throws IOException {
        URL url = new URL("http://localhost:" + port + "/game");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", authToken);
        connection.setDoOutput(true);

        JsonObject req = new JsonObject();
        req.addProperty("gameName", gameName);

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
                return;
            } else {
                throw new IOException("Create game failed: " + response);
            }
        }
    }

    public void joinGame(String authToken, int gameId, String color) throws IOException {
        URL url = new URL("http://localhost:" + port + "/game");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", authToken);
        connection.setDoOutput(true);

        JsonObject req = new JsonObject();
        req.addProperty("gameID", gameId);
        req.addProperty("playerColor", color);

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
                return;
            } else {
                throw new IOException("Join game failed: " + response);

            }
        }
    }

    public String listGamesRaw(String authToken) throws IOException {
        java.net.URL url = new java.net.URL("http://localhost:" + port + "/game");
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.connect();

        int status = connection.getResponseCode();
        InputStream responseStream = (status == 200)
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(responseStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            if (status == 200) {
                return sb.toString();
            } else {
                throw new IOException("List games failed: " + sb);
            }
        }
    }

    public void observeGame(String authToken, int gameId) throws IOException {
        // Put real HTTP DELETE request to /session
    }
}
