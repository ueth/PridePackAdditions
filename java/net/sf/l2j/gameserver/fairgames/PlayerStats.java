package net.sf.l2j.gameserver.fairgames;

public class PlayerStats {
    private int _competitionWon;
    private int _competitionLost;
    private int _gamesDone;
    private int _points;

    public PlayerStats(int _competitionWon, int _gamesDone, int _points, int _competitionLost) {
        this._competitionWon = _competitionWon;
        this._gamesDone = _gamesDone;
        this._points = _points;
        this._competitionLost = _competitionLost;
    }

    public int getCompetitionWon() { return _competitionWon; }

    public void setCompetitionWon(int _competitionWon) { this._competitionWon = _competitionWon; }

    public int getGamesDone() { return _gamesDone; }

    public void setGamesDone(int _gamesDone) { this._gamesDone = _gamesDone; }

    public int getPoints() { return _points; }

    public void setPoints(int _points) { this._points = _points; }

    public int getCompetitionLost() { return _competitionLost; }

    public void setCompetitionLost(int _competitionLost) { this._competitionLost = _competitionLost; }
}
