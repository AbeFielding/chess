import java.util.Scanner;

public class Main {
    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;
    private boolean running = true;
    private final Scanner scanner = new Scanner(System.in);

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
            case "quit" -> running = false;
            // login and register will be added in next steps
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
            case "quit" -> running = false;
            // Other commands will be added in next steps
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }
}
