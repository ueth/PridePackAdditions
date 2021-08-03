package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class PlayerStats {
    private int _competitionWon;
    private int _competitionLost;
    private int _gamesDone;
    private int _points;
    private int _streak;
    private L2PcInstance _player;
    private static final String[] _classes = {"mage", "warrior", "assassin", "archer"};
    private final Map<String, PlayerStats> _classesStats = new HashMap<>();

    public PlayerStats(L2PcInstance player) {
        _player = player;
    }

    public void loadEverything(boolean charCreate) {
        for (int i = 0; i < _classes.length; i++)
            _classesStats.put(_classes[i], new PlayerStats());

        if(charCreate){
            setStreak(0);
            setGamesDone(0);
            setPoints(250);
            setCompetitionWon(0);
            setCompetitionLost(0);
            saveStatsInDB();
        }else
            loadStats();
    }

    public PlayerStats() {}

    public void saveStatsInDB() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            //PreparedStatement statement = con.prepareStatement("UPDATE fg_stats SET compWon=?,compLost=?,gamesDone=?,points=?,streak=?,classStats=? WHERE charId=?");
            PreparedStatement statement = con.prepareStatement("REPLACE INTO fg_stats (compWon,compLost,gamesDone,points,streak,classStats,charId) VALUES (?,?,?,?,?,?,?)");
            statement.setInt(1, _competitionWon);
            statement.setInt(2, _competitionLost);
            statement.setInt(3, _gamesDone);
            statement.setInt(4, _points);
            statement.setInt(5, _streak);

            String toSave = "";

            for (String className : _classesStats.keySet()) {
                PlayerStats ps = _classesStats.get(className);
                toSave += className + ";" + ps.getCompetitionWon() + ";" + ps.getCompetitionLost() + ";" + ps.getGamesDone() + ";" + ps.getStreak() + "/";
            }

            statement.setString(6, toSave);
            statement.setInt(7, _player.getObjectId());

            statement.execute();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStatsAfterGame(boolean won, String className) {
        _gamesDone++;
        PlayerStats ps = _classesStats.get(className);
        ps.setGamesDone(ps.getGamesDone() + 1);
        if (won) {
            _competitionWon++;
            _streak++;
            if (_streak >= 5)
                _points += 15;
            else if (_streak >= 10)
                _points += 20;
            else
                _points += 10;

            ps.setCompetitionWon(ps.getCompetitionWon() + 1);
            ps.setStreak(ps.getStreak() + 1);
        } else {
            _competitionLost++;
            _streak = 0;
            if (_points < 10)
                _points = 0;
            else
                _points -= 10;

            ps.setCompetitionLost(ps.getCompetitionLost() + 1);
            ps.setStreak(0);
        }
        saveStatsInDB();
    }

    public void loadStats() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {

            PreparedStatement statement = con.prepareStatement("SELECT * FROM fg_stats where charId=?");
            statement.setInt(1, _player.getObjectId());
            ResultSet rs = statement.executeQuery();

            //rset.getInt("targetid")

            if (rs.next()) {
                setCompetitionLost(rs.getInt("compLost"));
                setCompetitionWon(rs.getInt("compWon"));
                setPoints(rs.getInt("points"));
                setGamesDone(rs.getInt("gamesDone"));
                setStreak(rs.getInt("streak"));

                StringTokenizer st = new StringTokenizer(rs.getString("classStats"), "/");

                while (st.hasMoreTokens()) {
                    StringTokenizer st1 = new StringTokenizer(st.nextToken(), ";");

                    PlayerStats ps = null;
                    if (st1.hasMoreTokens())
                        ps = _classesStats.get(st1.nextToken());
                    if (st1.hasMoreTokens())
                        ps.setCompetitionWon(Integer.valueOf(st1.nextToken()));
                    if (st1.hasMoreTokens())
                        ps.setCompetitionLost(Integer.valueOf(st1.nextToken()));
                    if (st1.hasMoreTokens())
                        ps.setGamesDone(Integer.valueOf(st1.nextToken()));
                    if (st1.hasMoreTokens())
                        ps.setStreak(Integer.valueOf(st1.nextToken()));
                }
            }


            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getCompetitionWon() {
        return _competitionWon;
    }

    public void setCompetitionWon(int _competitionWon) {
        this._competitionWon = _competitionWon;
    }

    public int getGamesDone() {
        return _gamesDone;
    }

    public void setGamesDone(int _gamesDone) {
        this._gamesDone = _gamesDone;
    }

    public int getPoints() {
        return _points;
    }

    public void setPoints(int _points) {
        this._points = _points;
    }

    public int getCompetitionLost() {
        return _competitionLost;
    }

    public void setCompetitionLost(int _competitionLost) {
        this._competitionLost = _competitionLost;
    }

    public int getStreak() {
        return _streak;
    }

    public void setStreak(int streak) {
        _streak = streak;
    }
}
