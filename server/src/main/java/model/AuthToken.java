package model;

public class AuthToken {
    private String token;
    private int userId;

    public AuthToken(String token, int userId) {
        this.token = token;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public int getUserId() {
        return userId;
    }
}
