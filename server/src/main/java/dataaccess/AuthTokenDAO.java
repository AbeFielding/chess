package dataaccess;

import model.AuthToken;

public interface AuthTokenDAO {
    void insertToken(AuthToken token) throws DataAccessException;
    AuthToken getToken(String token) throws DataAccessException;
    void deleteToken(String token) throws DataAccessException;
}
