import java.util.Scanner;

public class Main {
    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);

    private final ChessServerClient server = new ChessServerClient();
    private String authToken = null;
    private String[] lastGameList = new String[0];

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
                System.out.println("Login command not implemented yet.");
            }
            case "register" -> {
                System.out.print("Enter a username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter a password: ");
                String password = scanner.nextLine().trim();
                System.out.print("Enter an email: ");
                String email = scanner.nextLine().trim();

                try {
                    String token = server.register(username, password, email);
                    if (token != null && !token.isEmpty()) {
                        System.out.println("Registration successful! You are now logged in.");
                        authToken = token;
                        state = State.POSTLOGIN;
                    } else {
                        System.out.println("Registration failed. Please try again.");
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred during registration. Please try again.");
                }
            }
            case "quit" -> running = false;
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void handlePostloginCommand(String cmd) {
        switch (cmd) {
            case "help" -> System.out.println("""
                    Commands:
                    help      - Show this menu
                    logout    - Logout of your account
                    create    - Create a new game
                    list      - List all games
                    play      - Play a game
                    observe   - Observe a game
                    quit      - Exit the program
                    """);
            case "logout" -> {
                try {
                    server.logout(authToken);
                    authToken = null;
                    state = State.PRELOGIN;
                    System.out.println("You have been logged out.");
                } catch (Exception e) {
                    System.out.println("An error occurred during logout. Please try again.");
                }
            }
            case "create" -> {
                System.out.print("Enter a name for the new game: ");
                String gameName = scanner.nextLine().trim();
                try {
                    server.createGame(authToken, gameName);
                    System.out.println("Game created: " + gameName);
                } catch (Exception e) {
                    System.out.println("An error occurred while creating the game. Please try again.");
                }
            }
            case "list" -> {
                try {
                    lastGameList = server.listGames(authToken);
                    if (lastGameList.length == 0) {
                        System.out.println("No games found.");
                    } else {
                        System.out.println("Games:");
                        for (int i = 0; i < lastGameList.length; i++) {
                            System.out.printf("%d. %s%n", i + 1, lastGameList[i]);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("An error occurred while listing games. Please try again.");
                }
            }
            case "play" -> {
                if (lastGameList.length == 0) {
                    System.out.println("You must list games first.");
                    break;
                }
                System.out.print("Enter the number of the game to join: ");
                String numStr = scanner.nextLine().trim();
                int index = -1;
                try {
                    index = Integer.parseInt(numStr) - 1;
                    if (index < 0 || index >= lastGameList.length) {
                        System.out.println("Invalid game number.");
                        break;
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("Please enter a valid number.");
                    break;
                }

                System.out.print("Enter color to play (white/black): ");
                String color = scanner.nextLine().trim().toLowerCase();
                if (!color.equals("white") && !color.equals("black")) {
                    System.out.println("Invalid color. Please enter 'white' or 'black'.");
                    break;
                }

                try {
                    server.joinGame(authToken, index, color);
                    System.out.printf("Joined game '%s' as %s player.%n", lastGameList[index], color);
                    drawChessBoard(color.equals("white"));
                } catch (Exception e) {
                    System.out.println("An error occurred while joining the game. Please try again.");
                }
            }
            case "observe" -> {
                if (lastGameList.length == 0) {
                    System.out.println("You must list games first.");
                    break;
                }
                System.out.print("Enter the number of the game to observe: ");
                String numStr = scanner.nextLine().trim();
                int index = -1;
                try {
                    index = Integer.parseInt(numStr) - 1;
                    if (index < 0 || index >= lastGameList.length) {
                        System.out.println("Invalid game number.");
                        break;
                    }
                } catch (NumberFormatException ex) {
                    System.out.println("Please enter a valid number.");
                    break;
                }

                try {
                    server.observeGame(authToken, index);
                    System.out.printf("Now observing game '%s'.%n", lastGameList[index]);
                    drawChessBoard(true); // Always white perspective for observer
                } catch (Exception e) {
                    System.out.println("An error occurred while observing the game. Please try again.");
                }
            }
            case "quit" -> running = false;
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    // Chessboard
    private void drawChessBoard(boolean whitePerspective) {
        String[][] board = {
                {"r","n","b","q","k","b","n","r"},
                {"p","p","p","p","p","p","p","p"},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {" "," "," "," "," "," "," "," "},
                {"P","P","P","P","P","P","P","P"},
                {"R","N","B","Q","K","B","N","R"},
        };
        String cols = "  a b c d e f g h";
        if (!whitePerspective) cols = "  h g f e d c b a";
        System.out.println(cols);
        for (int i = 0; i < 8; i++) {
            int row = whitePerspective ? 8 - i : i + 1;
            System.out.print(row + " ");
            for (int j = 0; j < 8; j++) {
                int col = whitePerspective ? j : 7 - j;
                String piece = board[whitePerspective ? i : 7 - i][col];
                System.out.print(piece + " ");
            }
            System.out.println(row);
        }
        System.out.println(cols);
    }
}
