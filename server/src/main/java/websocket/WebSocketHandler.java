package websocket;

import com.google.gson.Gson;
import dataaccess.*;
import model.AuthToken;
import model.Game;
import model.GameData;
import model.User;
import chess.ChessGame;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.*;
import chess.InvalidMoveException;

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
        try {
            AuthTokenDAO authTokenDAO = new AuthTokenMySQLDAO();
            GameDAO gameDAO = new GameMySQLDAO();
            UserDAO userDAO = new UserMySQLDAO();

            AuthToken token = authTokenDAO.getToken(command.getAuthToken());
            if (token == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }

            int userId = token.getUserId();
            User user = userDAO.getUserById(userId);
            if (user == null) {
                sendError(session, "Error: User not found");
                return;
            }

            String username = user.getUsername();
            int gameID = command.getGameID();

            Game game = gameDAO.getGameById(gameID);
            if (game == null) {
                sendError(session, "Error: Game not found");
                return;
            }

            userSessions.put(session, username);
            gameSessions.computeIfAbsent(gameID, k -> new HashSet<>()).add(session);

            String whiteUsername = getUsernameFromUserId(game.getWhiteUserId(), userDAO);
            String blackUsername = getUsernameFromUserId(game.getBlackUserId(), userDAO);

            ChessGame chessGame = gson.fromJson(game.getState(), ChessGame.class);

            GameData gameData = new GameData(
                    game.getId(),
                    game.getGameName(),
                    whiteUsername,
                    blackUsername,
                    chessGame
            );

            LoadGameMessage loadMsg = new LoadGameMessage(gameData);
            session.getRemote().sendString(gson.toJson(loadMsg));

            String role = getRole(username, whiteUsername, blackUsername);
            String msg = username + " joined the game as " + role;
            NotificationMessage notify = new NotificationMessage(msg);
            broadcastToGame(gameID, gson.toJson(notify), except(session));

            System.out.println("✅ " + msg);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to connect: " + e.getMessage());
        }
    }


    private String getUsernameFromUserId(Integer userId, UserDAO userDAO) throws DataAccessException {
        if (userId == null) return null;
        User user = userDAO.getUserById(userId);
        return user != null ? user.getUsername() : null;
    }

    private String getRole(String username, String white, String black) {
        if (username.equals(white)) return "White";
        if (username.equals(black)) return "Black";
        return "Observer";
    }

    private void handleMakeMove(Session session, MakeMoveCommand command) {
        // TODO: validate move, update game, broadcast LOAD_GAME + NOTIFICATION
        try {
            AuthTokenDAO authTokenDAO = new AuthTokenMySQLDAO();
            GameDAO gameDAO = new GameMySQLDAO();
            UserDAO userDAO = new UserMySQLDAO();

            AuthToken token = authTokenDAO.getToken(command.getAuthToken());
            if (token == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }

            int userId = token.getUserId();
            String username = userDAO.getUserById(userId).getUsername();
            int gameID = command.getGameID();

            Game game = gameDAO.getGameById(gameID);
            if (game == null) {
                sendError(session, "Error: Game not found");
                return;
            }
    }

    private void handleLeave(Session session, UserGameCommand command) {
        gameSessions.getOrDefault(command.getGameID(), Set.of()).remove(session);
        userSessions.remove(session);
        // TODO: notify other players
    }

    private void handleResign(Session session, UserGameCommand command) {
        // TODO: mark game over, broadcast notification
    }

    private void broadcastToGame(int gameID, String json, Set<Session> skipSessions) {
        for (Session s : gameSessions.getOrDefault(gameID, Set.of())) {
            if (!skipSessions.contains(s) && s.isOpen()) {
                try {
                    s.getRemote().sendString(json);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Set<Session> except(Session... sessions) {
        return new HashSet<>(Arrays.asList(sessions));
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
