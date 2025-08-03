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

            if (game.isFinished()) {
                sendError(session, "Error: Game is already over");
                return;
            }

            ChessGame chessGame = gson.fromJson(game.getState(), ChessGame.class);

            String whiteUsername = getUsernameFromUserId(game.getWhiteUserId(), userDAO);
            String blackUsername = getUsernameFromUserId(game.getBlackUserId(), userDAO);

            boolean isWhite = username.equals(whiteUsername);
            boolean isBlack = username.equals(blackUsername);
            ChessGame.TeamColor playerColor = isWhite ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;

            if ((chessGame.getTeamTurn() == ChessGame.TeamColor.WHITE && !isWhite) ||
                    (chessGame.getTeamTurn() == ChessGame.TeamColor.BLACK && !isBlack)) {
                sendError(session, "Error: Not your turn");
                return;
            }

            chessGame.makeMove(command.getMove());
            game.setState(gson.toJson(chessGame));
            gameDAO.updateGameState(game.getId(), gson.toJson(chessGame), false);
            GameData updatedData = new GameData(
                    game.getId(),
                    game.getGameName(),
                    whiteUsername,
                    blackUsername,
                    chessGame
            );
            LoadGameMessage load = new LoadGameMessage(updatedData);
            String moveSummary = username + " moved " + command.getMove().toString();
            NotificationMessage notification = new NotificationMessage(moveSummary);

            Set<Session> allSessions = gameSessions.getOrDefault(gameID, Set.of());
            Set<Session> otherSessions = new HashSet<>(allSessions);
            otherSessions.remove(session);

            if (session.isOpen()) {
                session.getRemote().sendString(gson.toJson(load));
            }

            for (Session s : otherSessions) {
                if (s.isOpen()) {
                    s.getRemote().sendString(gson.toJson(load));
                    s.getRemote().sendString(gson.toJson(notification));
                }
            }

            ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK
                    : ChessGame.TeamColor.WHITE;

            if (chessGame.isInCheckmate(opponent)) {
                gameDAO.updateGameState(game.getId(), gson.toJson(chessGame), true); // ✅ mark game over
                broadcastToGame(gameID, gson.toJson(new NotificationMessage(opponent + " is in checkmate!")), Set.of());
            } else if (chessGame.isInCheck(opponent)) {
                broadcastToGame(gameID, gson.toJson(new NotificationMessage(opponent + " is in check.")), Set.of());
            }

        } catch (InvalidMoveException e) {
            sendError(session, "Error: Invalid move - " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to make move - " + e.getMessage());
        }
    }

    private void handleLeave(Session session, UserGameCommand command) {
        try {
            AuthTokenDAO authTokenDAO = new AuthTokenMySQLDAO();
            UserDAO userDAO = new UserMySQLDAO();
            GameDAO gameDAO = new GameMySQLDAO();

            AuthToken token = authTokenDAO.getToken(command.getAuthToken());
            if (token == null) {
                sendError(session, "Error: Invalid auth token");
                return;
            }

            int userId = token.getUserId();
            String username = userDAO.getUserById(userId).getUsername();
            int gameID = command.getGameID();

            userSessions.remove(session);
            gameSessions.getOrDefault(gameID, Set.of()).remove(session);

            Game game = gameDAO.getGameById(gameID);
            if (game != null) {
                boolean updated = false;
                if (Objects.equals(game.getWhiteUserId(), userId)) {
                    clearColorSlot("white_user_id", gameID);
                    updated = true;
                } else if (Objects.equals(game.getBlackUserId(), userId)) {
                    clearColorSlot("black_user_id", gameID);
                    updated = true;
                }

                String message = username + " left the game.";
                System.out.println("👋 " + message);
                broadcastToGame(gameID, gson.toJson(new NotificationMessage(message)), Set.of());
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to leave game - " + e.getMessage());
        }
    }

    private void clearColorSlot(String column, int gameID) throws DataAccessException {
        String sql = "UPDATE games SET " + column + " = NULL WHERE id = ?";
        try (var conn = dataaccess.DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameID);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Failed to clear color slot", e);
        }
    }


    private void handleResign(Session session, UserGameCommand command) {
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

            if (game.isFinished()) {
                sendError(session, "Error: Game is already over");
                return;
            }

            boolean isWhite = game.getWhiteUserId() != null && userId == game.getWhiteUserId();
            boolean isBlack = game.getBlackUserId() != null && userId == game.getBlackUserId();

            if (!isWhite && !isBlack) {
                sendError(session, "Error: Only players can resign");
                return;
            }

            gameDAO.updateGameState(game.getId(), game.getState(), true); // mark game finished

            String message = username + " resigned. Game over.";
            broadcastToGame(gameID, gson.toJson(new NotificationMessage(message)), Set.of());

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to resign - " + e.getMessage());
        }
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
