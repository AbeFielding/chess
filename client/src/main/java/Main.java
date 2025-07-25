import java.util.Scanner;

public class Main {
    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);

    private final ChessServerClient server = new ChessServerClient();
    private String authToken = null;

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
                System.out.println("Register command not implemented yet.");
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
                // Need Implement logout
                System.out.println("Logout command not implemented yet.");
            }
            case "create" -> {
                // Need Implement create game
                System.out.println("Create game command not implemented yet.");
            }
            case "list" -> {
                // Need Implement list games
                System.out.println("List games command not implemented yet.");
            }
            case "play" -> {
                // Need Implement play game
                System.out.println("Play game command not implemented yet.");
            }
            case "observe" -> {
                // Need Implement observe game
                System.out.println("Observe game command not implemented yet.");
            }
            case "quit" -> running = false;
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }
}
