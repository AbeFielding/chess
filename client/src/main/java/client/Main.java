package client;

import java.util.Scanner;
import com.google.gson.*;
import client.model.AuthData;
import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import client.websocket.ClientWebSocket;
import websocket.commands.MakeMoveCommand;
import chess.ChessPiece;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class Main {
    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);

    private final ServerFacade server = new ServerFacade();
    private AuthData authData = null;
    private GameSummary[] lastGameList = new GameSummary[0];
    public static Main.GameplayContext context;
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
            ChessGame game = new ChessGame();
            ClientWebSocket socket = new ClientWebSocket(game);
            socket.connect(authData.authToken(), gameId, ChessGame.TeamColor.valueOf(color.toUpperCase()));
            context = new GameplayContext(authData.authToken(), gameId,
                    ChessGame.TeamColor.valueOf(color.toUpperCase()), game, socket);
            runGameplayUI(context);
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
            ChessGame game = new ChessGame();
            ClientWebSocket socket = new ClientWebSocket(game);
            socket.connect(authData.authToken(), gameId, null);
            context = new GameplayContext(authData.authToken(), gameId, null, game, socket);
            runGameplayUI(context);
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
        if (context.socket.isGameOver()) {
            System.out.println("The game is over. You cannot make any more moves.");
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
            ChessMove move = new ChessMove(from, to, null);
            MakeMoveCommand cmd = new MakeMoveCommand(context.authToken, context.gameID, move);
            context.socket.send(cmd);
            System.out.println("Move sent: " + parts[1] + " to " + parts[2]);
        } catch (Exception e) {
            System.out.println("Invalid move input. Format should be like: move e2 e4");
        }
    }
    private ChessPosition parsePosition(String text) {
        text = text.toLowerCase();
        if (text.length() != 2) {
            throw new IllegalArgumentException("Invalid position: " + text);
        }
        char colChar = text.charAt(0);
        char rowChar = text.charAt(1);
        int col = colChar - 'a' + 1;
        int row = Character.getNumericValue(rowChar);
        if (col < 1 || col > 8 || row < 1 || row > 8) {
            throw new IllegalArgumentException("Invalid board position: " + text);
        }
        return new ChessPosition(row, col);
    }
    private void runGameplayUI(GameplayContext context) {
        System.out.println("Type 'help' to see available gameplay commands.");
        while (!context.socket.isGameLoaded()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
        while (true) {
            System.out.print("(game)> ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("help")) {
                printGameplayHelp();
            } else if (input.equalsIgnoreCase("redraw")) {
                drawChessBoard(context.game, context.isWhitePerspective(), null, null);
            } else if (input.equalsIgnoreCase("leave")) {
                leaveGame(context);
                return;
            } else if (input.equalsIgnoreCase("resign")) {
                resignGame(context);
            } else if (input.toLowerCase().startsWith("move ")) {
                handleMove(input, context);
            } else if (input.toLowerCase().startsWith("highlight ")) {
                handleHighlight(input, context);
            } else {
                System.out.println("Unknown command. Type 'help' to see available options.");
            }
        }
    }
    private void printGameplayHelp() {
        System.out.println("""
        Commands:
        help           - Show available commands
        move           - Make a move
        highlight      - Show legal moves for a piece
        redraw         - Redraw the board
        resign         - Resign from the game
        leave          - Leave the game and return to lobby
    """);
    }
    private void leaveGame(GameplayContext context) {
        context.socket.send(new websocket.commands.UserGameCommand(
                websocket.commands.UserGameCommand.CommandType.LEAVE,
                context.authToken,
                context.gameID
        ));
        context.socket.close();
        System.out.println("Left game.");
    }
    private void resignGame(GameplayContext context) {
        System.out.print("Are you sure you want to resign? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("y")) {
            context.socket.send(new websocket.commands.UserGameCommand(
                    websocket.commands.UserGameCommand.CommandType.RESIGN,
                    context.authToken,
                    context.gameID
            ));
            System.out.println("You resigned.");
        } else {
            System.out.println("Canceled resignation.");
        }
    }
    private void handleHighlight(String input, GameplayContext context) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length != 2) {
            System.out.println("Usage: highlight e2");
            return;
        }
        try {
            ChessPosition from = parsePosition(parts[1]);
            ChessPiece piece = context.game.getBoard().getPiece(from);
            if (piece == null) {
                System.out.println("No piece at that position.");
                return;
            }
            Collection<ChessMove> moves = context.game.validMoves(from);
            if (moves.isEmpty()) {
                System.out.println("No legal moves for that piece.");
                return;
            }
            Set<ChessPosition> targets = new HashSet<>();
            for (ChessMove move : moves) {
                targets.add(move.getEndPosition());
            }
            drawChessBoard(context.game, context.isWhitePerspective(), targets, from);
        } catch (Exception e) {
            System.out.println("Invalid input. Try: highlight e2");
        }
    }
    private String formatPosition(ChessPosition pos) {
        char col = (char) ('a' + pos.getColumn() - 1);
        return "" + col + pos.getRow();
    }
    public static void drawChessBoard(ChessGame game, boolean whitePerspective, Set<ChessPosition> highlights, ChessPosition selected) {
        final String reset = "\u001B[0m";
        final String lightBg = "\u001B[48;5;250m";
        final String darkBg = "\u001B[48;5;21m";
        final String lightHighlight = "\u001B[48;5;120m";
        final String darkHighlight = "\u001B[48;5;28m";
        final String selectedHighlight = "\u001B[48;5;226m";
        final String whiteFg = "\u001B[38;5;102m";
        final String blackFg = "\u001B[38;5;0m";
        final String borderBg = "\u001B[48;5;236m";
        final String borderFg = "\u001B[38;5;15m";
        char[] columns = whitePerspective ? "abcdefgh".toCharArray() : "hgfedcba".toCharArray();
        System.out.print(borderBg + borderFg + "   ");
        for (char c : columns) {
            System.out.print(" " + c + " ");
        }
        System.out.println(" " + reset);
        for (int i = 0; i < 8; i++) {
            int rowIdx = whitePerspective ? 8 - i : i + 1;
            int boardRow = whitePerspective ? 7 - i : i;
            System.out.print(borderBg + borderFg + " " + rowIdx + " " + reset);
            for (int j = 0; j < 8; j++) {
                int boardCol = whitePerspective ? j : 7 - j;
                int gameRow = boardRow + 1;
                int gameCol = boardCol + 1;
                ChessPosition pos = new ChessPosition(gameRow, gameCol);
                ChessPiece piece = game.getBoard().getPiece(pos);
                String symbol = " ";
                if (piece != null) {
                    symbol = switch (piece.getPieceType()) {
                        case PAWN -> "P";
                        case ROOK -> "R";
                        case KNIGHT -> "N";
                        case BISHOP -> "B";
                        case QUEEN -> "Q";
                        case KING -> "K";
                    };
                    if (piece.getTeamColor() == ChessGame.TeamColor.BLACK) {
                        symbol = symbol.toLowerCase();
                    }
                }
                boolean isLight = (boardRow + boardCol) % 2 == 0;
                boolean isHighlight = highlights != null && highlights.contains(pos);
                boolean isSelected = selected != null && selected.equals(pos);
                String bg = isSelected ? selectedHighlight :
                        isHighlight ? (isLight ? lightHighlight : darkHighlight) :
                                isLight ? lightBg : darkBg;
                String fg = symbol.equals(" ") ? "" :
                        Character.isUpperCase(symbol.charAt(0)) ? whiteFg : blackFg;
                System.out.print(bg + fg + " " + symbol + " " + reset);
            }
            System.out.println(borderBg + borderFg + " " + rowIdx + " " + reset);
        }
        System.out.print(borderBg + borderFg + "   ");
        for (char c : columns) {
            System.out.print(" " + c + " ");
        }
        System.out.println(" " + reset);
        System.out.flush();
    }
    private volatile boolean gameLoaded = false;
    public boolean isGameLoaded() {
        return gameLoaded;
    }
    static class GameSummary {
        int gameId;
        String name;
        String whitePlayer;
        String blackPlayer;
    }
    public static class GameplayContext  {
        String authToken;
        int gameID;
        ChessGame.TeamColor playerColor;
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
