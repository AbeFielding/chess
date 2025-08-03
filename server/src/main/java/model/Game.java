package model;

public class Game {
    private int id;
    private String state;
    private boolean finished;
    private Integer whiteUserId;
    private Integer blackUserId;
    private String gameName;


    public Game(int id, String state, boolean finished, Integer whiteUserId, Integer blackUserId, String gameName) {
        this.id = id;
        this.state = state;
        this.finished = finished;
        this.whiteUserId = whiteUserId;
        this.blackUserId = blackUserId;
        this.gameName = gameName;
    }

    public Game(String state, boolean finished, Integer whiteUserId, Integer blackUserId, String gameName) {
        this.state = state;
        this.finished = finished;
        this.whiteUserId = whiteUserId;
        this.blackUserId = blackUserId;
        this.gameName = gameName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setWhiteUserId(Integer whiteUserId) {
        this.whiteUserId = whiteUserId;
    }

    public void setBlackUserId(Integer blackUserId) {
        this.blackUserId = blackUserId;
    }

    public int getId() { return id; }
    public String getState() { return state; }
    public boolean isFinished() { return finished; }
    public Integer getWhiteUserId() { return whiteUserId; }
    public Integer getBlackUserId() { return blackUserId; }
    public String getGameName() { return gameName; }
}
