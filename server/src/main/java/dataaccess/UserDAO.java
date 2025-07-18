package dataaccess;

import model.User;

public interface UserDAO {
    void insertUser(User user) throws DataAccessException;
    User getUserByUsername(String username) throws DataAccessException;
}