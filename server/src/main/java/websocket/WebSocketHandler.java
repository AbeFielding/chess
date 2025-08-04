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

    private static final Gson GSON = new Gson();

    private static final Map<Integer, Set<Session>> GAME_SESSIONS = new HashMap<>();

    private static final Map<Session, String> USER_SESSIONS = new HashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println("Client connected: " + session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Client disconnected: " + session);
        USER_SESSIONS.remove(session);
        for (Set<Session> sessions : GAME_SESSIONS.values()) {
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
            UserGameCommand command = GSON.fromJson(message, UserGameCommand.class);
            switch (command.getCommandType()) {
                case CONNECT:
                    handleConnect(session, command);
                    break;
                case MAKE_MOVE:
                    MakeMoveCommand moveCommand = GSON.fromJson(message, MakeMoveCommand.class);
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

            USER_SESSIONS.put(session, username);
            GAME_SESSIONS.computeIfAbsent(gameID, k -> new HashSet<>()).add(session);

            String whiteUsername = getUsernameFromUserId(game.getWhiteUserId(), userDAO);
            String blackUsername = getUsernameFromUserId(game.getBlackUserId(), userDAO);

            ChessGame chessGame = GSON.fromJson(game.getState(), ChessGame.class);

            LoadGameMessage loadMsg = new LoadGameMessage(chessGame);
            session.getRemote().sendString(GSON.toJson(loadMsg));

            String role = getRole(username, whiteUsername, blackUsername);
            String msg = username + " joined the game as " + role;
            NotificationMessage notify = new NotificationMessage(msg);
            broadcastToGame(gameID, GSON.toJson(notify), except(session));

            System.out.println("✅ " + msg);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to connect: " + e.getMessage());
        }
    }


    private String getUsernameFromUserId(Integer userId, UserDAO userDAO) throws DataAccessException {
        if (userId == null) {
            return null;
        }
        User user = userDAO.getUserById(userId);
        return (user != null) ? user.getUsername() : null;
    }

    private String getRole(String username, String white, String black) {
        if (username.equals(white)) {
            return "White";
        }
        if (username.equals(black)) {
            return "Black";
        }
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

            ChessGame chessGame = GSON.fromJson(game.getState(), ChessGame.class);

            String whiteUsername = getUsernameFromUserId(game.getWhiteUserId(), userDAO);
            String blackUsername = getUsernameFromUserId(game.getBlackUserId(), userDAO);

            boolean isWhite = username.equals(whiteUsername);
            boolean isBlack = username.equals(blackUsername);
            ChessGame.TeamColor playerColor = isWhite ? ChessGame.TeamColor.WHITE : (isBlack ? ChessGame.TeamColor.BLACK : null);

            if ((chessGame.getTeamTurn() == ChessGame.TeamColor.WHITE && !isWhite) ||
                    (chessGame.getTeamTurn() == ChessGame.TeamColor.BLACK && !isBlack)) {
                sendError(session, "Error: Not your turn");
                return;
            }

            chessGame.makeMove(command.getMove());
            String newStateJson = GSON.toJson(chessGame);
            game.setState(newStateJson);
            gameDAO.updateGameState(game.getId(), newStateJson, false);

            LoadGameMessage load = new LoadGameMessage(chessGame);
            String moveSummary = username + " moved " + command.getMove();
            NotificationMessage notification = new NotificationMessage(moveSummary);

            Set<Session> allSessions = GAME_SESSIONS.getOrDefault(gameID, Set.of());
            Set<Session> otherSessions = new HashSet<>(allSessions);
            otherSessions.remove(session);

            if (session.isOpen()) {
                session.getRemote().sendString(GSON.toJson(load));
            }

            for (Session s : otherSessions) {
                if (s.isOpen()) {
                    s.getRemote().sendString(GSON.toJson(load));
                    s.getRemote().sendString(GSON.toJson(notification));
                }
            }

            ChessGame.TeamColor opponent = (playerColor == ChessGame.TeamColor.WHITE)
                    ? ChessGame.TeamColor.BLACK
                    : ChessGame.TeamColor.WHITE;

            if (chessGame.isInCheckmate(opponent)) {
                gameDAO.updateGameState(game.getId(), newStateJson, true);
                broadcastToGame(gameID, GSON.toJson(new NotificationMessage(opponent + " is in checkmate! Game Over")), Set.of());
            } else if (chessGame.isInCheck(opponent)) {
                broadcastToGame(gameID, GSON.toJson(new NotificationMessage(opponent + " is in check.")), Set.of());
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

            USER_SESSIONS.remove(session);
            GAME_SESSIONS.getOrDefault(gameID, Set.of()).remove(session);

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
                broadcastToGame(gameID, GSON.toJson(new NotificationMessage(message)), Set.of());
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

            boolean isWhite = (game.getWhiteUserId() != null) && (userId == game.getWhiteUserId());
            boolean isBlack = (game.getBlackUserId() != null) && (userId == game.getBlackUserId());

            if (!isWhite && !isBlack) {
                sendError(session, "Error: Only players can resign");
                return;
            }

            gameDAO.updateGameState(game.getId(), game.getState(), true); // mark game finished

            String message = username + " resigned. Game over.";
            broadcastToGame(gameID, GSON.toJson(new NotificationMessage(message)), Set.of());

        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error: Failed to resign - " + e.getMessage());
        }
    }


    private void broadcastToGame(int gameID, String json, Set<Session> skipSessions) {
        for (Session s : GAME_SESSIONS.getOrDefault(gameID, Set.of())) {
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
            session.getRemote().sendString(GSON.toJson(error));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AuthToken requireValidToken(String authTokenStr, Session session) throws IOException, DataAccessException {
        AuthTokenDAO authTokenDAO = new AuthTokenMySQLDAO();
        AuthToken token = authTokenDAO.getToken(authTokenStr);
        if (token == null) {
            sendError(session, "Error: Invalid auth token");
            return null;
        }
        return token;
    }

    private Game requireValidGame(int gameID, Session session, GameDAO gameDAO) throws IOException, DataAccessException {
        Game game = gameDAO.getGameById(gameID);
        if (game == null) {
            sendError(session, "Error: Game not found");
            return null;
        }
        return game;
    }

    private String requireValidUsername(int userId, Session session, UserDAO userDAO) throws IOException, DataAccessException {
        User user = userDAO.getUserById(userId);
        if (user == null) {
            sendError(session, "Error: User not found");
            return null;
        }
        return user.getUsername();
    }

}
