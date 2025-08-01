package websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import websocket.commands.*;
import websocket.messages.*;

import java.io.IOException;
import java.util.*;

@WebSocket
public class WebSocketHandler {

    private static final Gson gson = new Gson();

    private static final Map<Integer, Set<Session>> gameSessions = new HashMap<>();

    private static final Map<Session, String> userSessions = new HashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected: " + session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Client disconnected: " + session);
        userSessions.remove(session);
        for (Set<Session> sessions : gameSessions.values()) {
            sessions.remove(session);
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        System.out.println("WebSocket error: " + throwable.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
            switch (command.getCommandType()) {
                case CONNECT:
                    handleConnect(session, command);
                    break;
                case MAKE_MOVE:
                    MakeMoveCommand moveCommand = gson.fromJson(message, MakeMoveCommand.class);
                    handleMakeMove(session, moveCommand);
                    break;
                case LEAVE:
                    handleLeave(session, command);
                    break;
                case RESIGN:
                    handleResign(session, command);
                    break;
            }
        } catch (Exception e) {
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleConnect(Session session, UserGameCommand command) {
        // TODO: validate token, gameID, get username, etc.

        userSessions.put(session, "TODO-username");
        gameSessions.computeIfAbsent(command.getGameID(), k -> new HashSet<>()).add(session);

        // TODO: send LOAD_GAME to this client
        // TODO: broadcast NOTIFICATION to others
    }

    private void handleMakeMove(Session session, MakeMoveCommand command) {
        // TODO: validate move, update game, broadcast LOAD_GAME + NOTIFICATION
    }

    private void handleLeave(Session session, UserGameCommand command) {
        gameSessions.getOrDefault(command.getGameID(), Set.of()).remove(session);
        userSessions.remove(session);
        // TODO: notify other players
    }

    private void handleResign(Session session, UserGameCommand command) {
        // TODO: mark game over, broadcast notification
    }

    private void sendError(Session session, String errorMsg) {
        try {
            ErrorMessage error = new ErrorMessage(errorMsg);
            session.getRemote().sendString(gson.toJson(error));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
