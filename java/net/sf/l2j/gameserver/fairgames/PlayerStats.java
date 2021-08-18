package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PlayerStats {
    private int _competitionWon;
    private int _competitionLost;
    private int _gamesDone;
    private int _points;
    private int _streak;
    private List<String> _matchHistory = new ArrayList<>();
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
            PreparedStatement statement = con.prepareStatement("REPLACE INTO fg_stats (name,compWon,compLost,gamesDone,points,streak,classStats,charId) VALUES (?,?,?,?,?,?,?,?)");
            statement.setString(1, _player.getName());
            statement.setInt(2, _competitionWon);
            statement.setInt(3, _competitionLost);
            statement.setInt(4, _gamesDone);
            statement.setInt(5, _points);
            statement.setInt(6, _streak);

            String toSave = "";

            for (String className : _classesStats.keySet()) {
                PlayerStats ps = _classesStats.get(className);
                toSave += className + ";" + ps.getCompetitionWon() + ";" + ps.getCompetitionLost() + ";" + ps.getGamesDone() + ";" + ps.getStreak() + "/";
            }

            statement.setString(7, toSave);
            statement.setInt(8, _player.getObjectId());

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

    public void loadMatchHistory(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {

            PreparedStatement statement = con.prepareStatement("SELECT * FROM fg_match_history where playerId1=? OR playerId2=? ORDER BY realTime DESC LIMIT 0, 10");
            statement.setInt(1, _player.getObjectId());
            statement.setInt(2, _player.getObjectId());
            ResultSet rs = statement.executeQuery();

            String playerName1;
            String playerName2;
            String class1;
            String class2;
            int dmgDone1;
            int dmgDone2;
            String gameTime;
            String winner;
            String winHow;

            while (rs.next()) {
                String temp = "";

                playerName1 = rs.getString("playerName1");
                playerName2 = rs.getString("playerName2");
                class1 = rs.getString("class1");
                class2 = rs.getString("class2");
                dmgDone1 = rs.getInt("dmgDone1");
                dmgDone2 = rs.getInt("dmgDone2");
                gameTime = rs.getString("gameTime");
                winner = rs.getString("winner");
                winHow = rs.getString("winHow");

                temp += playerName1 + "(" + class1 + ")" + " Dmg - " + dmgDone1 + " vs " + playerName2 + "(" + class2 + ")" + " Dmg - " + dmgDone2 + " / Winner - " + winner + " / Won by: " + winHow + " / Time - " + gameTime;
                _matchHistory.add(temp);
            }


            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getMatchHistory(){
        return _matchHistory;
    }

    public void addMatch(String str){
        //First we shift the list and then we add the string
        List<String> temp = new ArrayList<>();
        temp.addAll(_matchHistory);

        _matchHistory.clear();
        _matchHistory.add(str);

        int size = temp.size();
        if (size>=10)
            size = 9;

        for(int i = 0; i<size; i++)
            _matchHistory.add(temp.get(i));
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
