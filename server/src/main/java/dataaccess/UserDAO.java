package dataaccess;

import model.User;

public interface UserDAO {
    void insertUser(User user) throws DataAccessException;
    User getUserByUsername(String username) throws DataAccessException;
    User getUserById(int id) throws DataAccessException;
}
