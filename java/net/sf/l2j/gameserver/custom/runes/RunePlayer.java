package net.sf.l2j.gameserver.custom.runes;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunePlayer {
    private Map<Integer, Rune> _runes = new HashMap<>();
    private List<Rune> _activeRunes = new ArrayList<>();
    private Rune _forbiddenRune;
    private L2PcInstance _player;
    private RunePages _runePages;

    public RunePlayer(L2PcInstance player){
        _player = player;
        _runePages = new RunePages(_player);
    }

    public void addRune(Rune rune){
        if(!_activeRunes.isEmpty()) {
            for (Rune rune1 : _activeRunes) {
                if (rune1.getId() == rune.getId()) {
                    _player.sendMessage("You already own this rune");
                    return;
                }
            }
        }

        if(!_runes.isEmpty()) {
            for (Rune rune1 : _runes.values()) {
                if (rune1.getId() == rune.getId()) {
                    _player.sendMessage("You already own this rune");
                    return;
                }
            }
        }

        if(_forbiddenRune!=null && _forbiddenRune.getId() == rune.getId()){
            _player.sendMessage("You already own this rune");
            return;
        }

        _runes.put(rune.getId(), rune);
        saveRune(rune, false);
    }

    public RunePages getRunePages(){
        return _runePages;
    }

    public List<Rune> getActiveRunes(){
        return _activeRunes;
    }

    public Map<Integer, Rune> getRunes(){
        return _runes;
    }

    public Rune getRune(int id){
        return _runes.get(id);
    }

    public Rune getForbiddenRune(){
        return _forbiddenRune;
    }

    public void wearRune(Rune rune){
        if(rune == null)
            return;

        if(!_runes.containsKey(rune.getId()))
            return;

        switch (rune.getMaxLevel()){
            case 10: {

                if (_activeRunes.size() == 3) {
                    updateRuneActiveness(_activeRunes.get(2), 0);
                    swapActivity(_activeRunes.get(2));
                }
                if (_runes.remove(rune.getId(), rune)) {
                    skillHandle(rune, null);
                    _activeRunes.add(rune);
                    updateRuneActiveness(rune, 1);
                }
                break;
            }
            case 15: {
                if (_runes.remove(rune.getId(), rune)) {
                    if (_forbiddenRune != null) {
                        skillHandle(rune, _forbiddenRune);
                        updateRuneActiveness(_forbiddenRune, 0);
                        _runes.put(_forbiddenRune.getId(), _forbiddenRune);
                    } else {
                        skillHandle(rune, null);
                    }
                    updateRuneActiveness(rune, 1);
                    _forbiddenRune = rune;
                }
                break;
            }
        }
    }

    public void removeRune(Rune rune){
        if(rune == null)
            return;

        switch (rune.getMaxLevel()){
            case 10: {
                for(int i=0; i<_activeRunes.size(); i++){
                    if(rune.getId() == _activeRunes.get(i).getId()){
                        skillHandle(null, rune);
                        updateRuneActiveness(rune, 0);
                        _activeRunes.remove(i);
                        break;
                    }
                }

                break;
            }
            case 15: {
                _forbiddenRune = null;
                skillHandle(null, rune);
                updateRuneActiveness(rune, 0);
                break;
            }
        }
        _runes.put(rune.getId(), rune);
    }

    private void skillHandle(Rune runeIdToAdd, Rune runeIdToRemove){
        if(runeIdToAdd != null)
            _player.addSkill(SkillTable.getInstance().getInfo(runeIdToAdd.getSkillId(), runeIdToAdd.getLevel()), true);

        if(runeIdToRemove !=null)
            _player.removeSkill(SkillTable.getInstance().getInfo(runeIdToRemove.getSkillId(), runeIdToRemove.getLevel()));
    }

    public void saveRune(Rune rune, boolean active){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO runes_player (playerId,runeId,runeLevel,runeExp,active) VALUES (?,?,?,?,?)");
            statement.setInt(1, _player.getObjectId());
            statement.setInt(2, rune.getId());
            statement.setInt(3, rune.getLevel());
            statement.setDouble(4, rune.getExp());
            if(active)
                statement.setInt(5, 1);
            else
                statement.setInt(5, 0);
            statement.execute();
            statement.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void updateRunes(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE runes_player SET runeLevel=?,runeExp=? WHERE playerId=? and runeId=?");

            stm.setInt(3, _player.getObjectId());

            if(_forbiddenRune != null){
                stm.setInt(1, _forbiddenRune.getLevel());
                stm.setDouble(2, _forbiddenRune.getExp());
                stm.setLong(4, _forbiddenRune.getId());
                stm.execute();
            }

            for(Rune rune : _activeRunes) {
                stm.setInt(1, rune.getLevel());
                stm.setDouble(2, rune.getExp());
                stm.setLong(4, rune.getId());
                stm.execute();
            }

            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }

    private void swapActivity(Rune rune){
        skillHandle(null, rune); // remove the skill of the rune that is being replaced
        _runes.put(rune.getId(), rune); // placing the rune that is being removed, back to the "inventory" of runes
        _activeRunes.remove(2); // remove the rune from the active runes list
    }

    public void updateRuneActiveness(Rune rune, int active){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE runes_player SET active=? WHERE playerId=? and runeId=?");

            stm.setInt(1, active);
            stm.setInt(2, _player.getObjectId());
            stm.setLong(3, rune.getId());
            stm.execute();
            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }

    public void loadRunes(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM runes_player where playerId=?");
            statement.setInt(1, _player.getObjectId());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int runeId = rs.getInt("runeId");
                int runeLevel = rs.getInt("runeLevel");
                int active = rs.getInt("active");
                double exp = rs.getDouble("runeExp");

                Rune rune = RuneTable.getRune(runeId);
                rune.setExp(exp);
                rune.setLevel(runeLevel);

                if(rune.getMaxLevel() >= 15) {
                    if(active == 1)
                        _forbiddenRune = rune;
                    else
                        _runes.put(runeId, rune);
                }
                else {
                    if (active == 1)
                        _activeRunes.add(rune);
                    else
                        _runes.put(runeId, rune);
                }
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }
}
