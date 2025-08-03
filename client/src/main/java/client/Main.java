package client;

import java.util.Scanner;
import com.google.gson.*;
import client.model.AuthData;
import java.util.Map;
import java.util.HashMap;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import client.websocket.ClientWebSocket;
import websocket.commands.MakeMoveCommand;

public class Main {
    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);

    private final ServerFacade server = new ServerFacade();
    private AuthData authData = null;
    private GameSummary[] lastGameList = new GameSummary[0];

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        while (running) {
            if (state == State.PRELOGIN) {
                showPreloginMenu();
                handlePreloginCommand(prompt());
            } else {
                showPostloginMenu();
                handlePostloginCommand(prompt());
            }
        }
        System.out.println("Goodbye!");
    }

    private void showPreloginMenu() {
        System.out.println("\n=== Chess Client ===");
        System.out.println("Type 'help' to see options.");
    }

    private void showPostloginMenu() {
        System.out.println("\n=== Chess Client (Logged In) ===");
        System.out.println("Type 'help' to see options.");
    }

    private String prompt() {
        System.out.print("> ");
        return scanner.nextLine().trim().toLowerCase();
    }

    private void handlePreloginCommand(String cmd) {
        switch (cmd) {
            case "help" -> System.out.println("""
                Commands:
                help    - Show this menu
                quit    - Exit the program
                login   - Login to your account
                register- Register a new account
                """);
            case "login" -> {
                System.out.print("Enter a username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter a password: ");
                String password = scanner.nextLine().trim();

                try {
                    AuthData auth = server.login(username, password);
                    if (auth != null && auth.authToken() != null && !auth.authToken().isEmpty()) {
                        System.out.println("Login successful!");
                        authData = auth;
                        state = State.POSTLOGIN;
                    } else {
                        System.out.println("Login failed. Please try again.");
                    }
                } catch (Exception e) {
                    System.out.println("Login failed: " + e.getMessage());
                }
            }
            case "register" -> {
                System.out.print("Enter a username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter a password: ");
                String password = scanner.nextLine().trim();
                System.out.print("Enter an email: ");
                String email = scanner.nextLine().trim();

                try {
                    AuthData auth = server.register(username, password, email);
                    if (auth != null && auth.authToken() != null && !auth.authToken().isEmpty()) {
                        System.out.println("Registration successful! You are now logged in.");
                        authData = auth;
                        state = State.POSTLOGIN;
                    } else {
                        System.out.println("Registration failed. Please try again.");
                    }
                } catch (Exception e) {
                    System.out.println("Registration failed: " + e.getMessage());
                }
            }
            case "quit" -> running = false;
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void handlePostloginCommand(String cmd) {
        switch (cmd) {
            case "help" -> printPostloginHelp();
            case "logout" -> handleLogout();
            case "create" -> handleCreateGame();
            case "list" -> handleListGames();
            case "play" -> handlePlayGame();
            case "observe" -> handleObserveGame();
            case "quit" -> running = false;
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void printPostloginHelp() {
        System.out.println("""
            Commands:
            help      - Show this menu
            logout    - Logout of your account
            create    - Create a new game
            list      - List all games
            play      - Play a game
            observe   - Observe a game
            quit      - Exit the program
            """);
    }

    private void handleLogout() {
        try {
            server.logout(authData.authToken());
            authData = null;
            state = State.PRELOGIN;
            System.out.println("You have been logged out.");
        } catch (Exception e) {
            System.out.println("An error occurred during logout. Please try again.");
        }
    }

    private void handleCreateGame() {
        System.out.print("Enter a name for the new game: ");
        String gameName = scanner.nextLine().trim();
        try {
            server.createGame(authData.authToken(), gameName);
            System.out.println("Game created: " + gameName);
        } catch (Exception e) {
            System.out.println("An error occurred while creating the game. Please try again.");
        }
    }

    private void handleListGames() {
        try {
            String json = server.listGamesRaw(authData.authToken());
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            JsonArray arr = obj.getAsJsonArray("games");
            if (arr.isEmpty()) {
                System.out.println("No games found.");
                lastGameList = new GameSummary[0];
            } else {
                System.out.println("Games:");
                lastGameList = new GameSummary[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject g = arr.get(i).getAsJsonObject();
                    lastGameList[i] = new GameSummary();
                    lastGameList[i].gameId = g.get("gameID").getAsInt();
                    lastGameList[i].name = g.get("gameName").getAsString();

                    lastGameList[i].whitePlayer = g.has("whiteUsername") && !g.get("whiteUsername").isJsonNull()
                            ? g.get("whiteUsername").getAsString()
                            : "empty";
                    lastGameList[i].blackPlayer = g.has("blackUsername") && !g.get("blackUsername").isJsonNull()
                            ? g.get("blackUsername").getAsString()
                            : "empty";

                    System.out.printf("%d. Game Name: %s    White: %s    Black: %s%n",
                            i + 1,
                            lastGameList[i].name,
                            lastGameList[i].whitePlayer,
                            lastGameList[i].blackPlayer);
                }
            }
        } catch (Exception e) {
            System.out.println("An error occurred while listing games. Please try again.");
        }
    }

    private void handlePlayGame() {
        if (lastGameList.length == 0) {
            System.out.println("You must list games first.");
            return;
        }
        System.out.print("Enter the number of the game to join: ");
        String numStr = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(numStr) - 1;
            if (index < 0 || index >= lastGameList.length) {
                System.out.println("Invalid game number.");
                return;
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a valid number.");
            return;
        }
        int gameId = lastGameList[index].gameId;

        System.out.print("Enter color to play (white/black): ");
        String color = scanner.nextLine().trim().toLowerCase();
        if (!color.equals("white") && !color.equals("black")) {
            System.out.println("Invalid color. Please enter 'white' or 'black'.");
            return;
        }

        try {
            server.joinGame(authData.authToken(), gameId, color);
            System.out.printf("Joined game '%s' as %s player.%n", lastGameList[index].name, color);
            drawChessBoard(color.equals("white"));
        } catch (Exception e) {
            System.out.println("An error occurred while joining the game. Please try again.");
            System.out.println("Server message: " + e.getMessage());
        }
    }

    private void handleObserveGame() {
        if (lastGameList.length == 0) {
            System.out.println("You must list games first.");
            return;
        }
        System.out.print("Enter the number of the game to observe: ");
        String numStr = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(numStr) - 1;
            if (index < 0 || index >= lastGameList.length) {
                System.out.println("Invalid game number.");
                return;
            }
        } catch (NumberFormatException ex) {
            System.out.println("Please enter a valid number.");
            return;
        }
        int gameId = lastGameList[index].gameId;

        try {
            server.observeGame(authData.authToken(), gameId);
            System.out.printf("Now observing game '%s'.%n", lastGameList[index].name);
            drawChessBoard(true);
        } catch (Exception e) {
            System.out.println("An error occurred while observing the game. Please try again.");
            System.out.println("Server message: " + e.getMessage());
        }
    }

    private void handleMove(String input, GameplayContext context) {
        if (context.playerColor == null) {
            System.out.println("Observers cannot make moves.");
            return;
        }

        if (context.game == null) {
            System.out.println("Game not loaded.");
            return;
        }

        String[] parts = input.trim().split("\\s+");
        if (parts.length < 3) {
            System.out.println("Usage: move e2 e4");
            return;
        }

        try {
            ChessPosition from = parsePosition(parts[1]);
            ChessPosition to = parsePosition(parts[2]);

            ChessMove move = new ChessMove(from, to, null); // No promotion support yet
            MakeMoveCommand cmd = new MakeMoveCommand(context.authToken, context.gameID, move);
            context.socket.send(cmd);
            System.out.println("Move sent: " + parts[1] + " to " + parts[2]);

        } catch (Exception e) {
            System.out.println("Invalid move input. Format should be like: move e2 e4");
        }
    }

    private ChessPosition parsePosition(String text) {
        text = text.toLowerCase();
        if (text.length() != 2) throw new IllegalArgumentException("Invalid position: " + text);

        char colChar = text.charAt(0);
        char rowChar = text.charAt(1);

        int col = colChar - 'a' + 1;
        int row = Character.getNumericValue(rowChar);

        if (col < 1 || col > 8 || row < 1 || row > 8) {
            throw new IllegalArgumentException("Invalid board position: " + text);
        }

        return new ChessPosition(row, col);
    }

    private void drawChessBoard(boolean whitePerspective) {
        final String reset = "\u001B[0m";
        final String lightBg = "\u001B[48;5;250m";
        final String darkBg = "\u001B[48;5;21m";
        final String whiteFg = "\u001B[38;5;102m";
        final String blackFg = "\u001B[38;5;0m";
        final String borderBg = "\u001B[48;5;236m";
        final String borderFg = "\u001B[38;5;15m";

        Map<String, String> symbols = Map.ofEntries(
                Map.entry("P", "P"), Map.entry("R", "R"), Map.entry("N", "N"),
                Map.entry("B", "B"), Map.entry("Q", "Q"), Map.entry("K", "K"),
                Map.entry("p", "p"), Map.entry("r", "r"), Map.entry("n", "n"),
                Map.entry("b", "b"), Map.entry("q", "q"), Map.entry("k", "k")
        );

        String[][] board = {
                {"r", "n", "b", "q", "k", "b", "n", "r"},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };

        char[] columns = whitePerspective ? "abcdefgh".toCharArray() : "hgfedcba".toCharArray();
        System.out.print(borderBg + borderFg + "   ");
        for (char c : columns) {
            System.out.print(" " + c + " ");
        }
        System.out.println(" " + reset);

        for (int i = 0; i < 8; i++) {
            int rowIdx = whitePerspective ? 8 - i : i + 1;
            int boardRow = whitePerspective ? i : 7 - i;
            System.out.print(borderBg + borderFg + " " + rowIdx + " " + reset);

            for (int j = 0; j < 8; j++) {
                int boardCol = whitePerspective ? j : 7 - j;
                String piece = board[boardRow][boardCol];
                String symbol = symbols.getOrDefault(piece, " ");

                boolean isLight = (boardRow + boardCol) % 2 == 0;
                String bg = isLight ? lightBg : darkBg;
                String fg = piece.equals(" ") ? "" :
                        Character.isUpperCase(piece.charAt(0)) ? whiteFg : blackFg;

                System.out.print(bg + fg + " " + symbol + " " + reset);
            }
            System.out.println(borderBg + borderFg + " " + rowIdx + " " + reset);
        }
        System.out.print(borderBg + borderFg + "   ");
        for (char c : columns) {
            System.out.print(" " + c + " ");
        }
        System.out.println(" " + reset);
    }



    static class GameSummary {
        int gameId;
        String name;
        String whitePlayer;
        String blackPlayer;
    }

    static class GameplayContext {
        String authToken;
        int gameID;
        ChessGame.TeamColor playerColor; // null for observer
        ChessGame game;
        ClientWebSocket socket;

        public GameplayContext(String authToken, int gameID, ChessGame.TeamColor playerColor,
                               ChessGame game, ClientWebSocket socket) {
            this.authToken = authToken;
            this.gameID = gameID;
            this.playerColor = playerColor;
            this.game = game;
            this.socket = socket;
        }

        public boolean isWhitePerspective() {
            return playerColor != ChessGame.TeamColor.BLACK;
        }
    }
}
