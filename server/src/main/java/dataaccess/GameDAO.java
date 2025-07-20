package dataaccess;

import model.Game;
import java.util.List;

public interface GameDAO {
    int insertGame(Game game) throws DataAccessException;
    Game getGameById(int id) throws DataAccessException;
    List<Game> listGames() throws DataAccessException;
    void updateGameState(int gameId, String state, boolean finished) throws DataAccessException;
}
