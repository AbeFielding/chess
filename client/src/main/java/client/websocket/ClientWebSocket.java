package client.websocket;

import chess.ChessGame;
import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.*;
import websocket.commands.UserGameCommand.CommandType;
import websocket.commands.MakeMoveCommand;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class ClientWebSocket {
    private Session session;
    private final Gson gson = new Gson();
    private final ChessGame game;
    private boolean gameOver = false;

    public ClientWebSocket(ChessGame game) {
        this.game = game;
    }

    public void connect(String authToken, int gameID, ChessGame.TeamColor color) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        URI uri = new URI("ws://localhost:8080/ws");
        container.connectToServer(this, uri);

        UserGameCommand connectCmd = new UserGameCommand(CommandType.CONNECT, authToken, gameID);
        this.send(connectCmd);
    }

    public void send(UserGameCommand cmd) {
        if (session != null && session.isOpen()) {
            String json = gson.toJson(cmd);
            session.getAsyncRemote().sendText(json);
        }
    }

    public void send(MakeMoveCommand moveCmd) {
        if (session != null && session.isOpen()) {
            String json = gson.toJson(moveCmd);
            session.getAsyncRemote().sendText(json);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[WebSocket] Connected to game server.");
    }

    @OnMessage
    public void onMessage(String message) {
        ServerMessage msg = gson.fromJson(message, ServerMessage.class);
        switch (msg.getServerMessageType()) {
            case LOAD_GAME -> {
                LoadGameMessage load = gson.fromJson(message, LoadGameMessage.class);
                game.copyFrom(load.getGame());
                gameLoaded = true;
                System.out.println("\n[Game updated]");

                if (load.getGame().isInCheckmate(game.getTeamTurn()) ||
                        load.getGame().isInStalemate(game.getTeamTurn())) {
                    gameOver = true;
                }
            }
            case NOTIFICATION -> {
                NotificationMessage note = gson.fromJson(message, NotificationMessage.class);
                System.out.println("\n[Notification] " + note.getMessage());
            }
            case ERROR -> {
                ErrorMessage error = gson.fromJson(message, ErrorMessage.class);
                System.out.println("\n[Error] " + error.getErrorMessage());
            }
        }
    }



    public void close() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            System.out.println("Error closing WebSocket: " + e.getMessage());
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public ChessGame getGame() {
        return game;
    }

    private volatile boolean gameLoaded = false;

    public boolean isGameLoaded() {
        return gameLoaded;
    }

}
