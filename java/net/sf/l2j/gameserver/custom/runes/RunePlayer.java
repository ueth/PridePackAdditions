package net.sf.l2j.gameserver.custom.runes;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.custom.battlepass.BattlePass;
import net.sf.l2j.gameserver.custom.battlepass.BattlePassTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunePlayer {
    private Map<Integer, Rune> _runes = new HashMap<>();
    private List<Rune> _activeRunes = new ArrayList<>();
    private Rune _forbiddenRune;
    private L2PcInstance _player;

    public RunePlayer(L2PcInstance player){
        _player = player;
    }

    public void addRune(Rune rune){
        _runes.put(rune.getId(), rune);
    }

    public void wearRune(Rune rune){
        switch (rune.getMaxLevel()){
            case 10: {
                if(!_runes.containsKey(rune.getId()))
                    return;

                if (_activeRunes.size() >= 3) {
                    skillHandle(null, _activeRunes.get(2)); // remove the skill of the rune that is being replaced
                    _runes.put(_activeRunes.get(2).getId(), _activeRunes.get(2));
                    _activeRunes.remove(2);
                }
                if (_runes.remove(rune.getId(), rune)) {
                    skillHandle(rune, null);
                    _activeRunes.add(rune);
                }
                break;
            }
            case 15: {
                if(_forbiddenRune != null)
                    skillHandle(rune, _forbiddenRune);
                else
                    skillHandle(rune, null);

                _forbiddenRune = rune;
                break;
            }
        }
    }

    private void skillHandle(Rune runeIdToAdd, Rune runeIdToRemove){
        if(runeIdToAdd != null)
            _player.addSkill(SkillTable.getInstance().getInfo(runeIdToAdd.getSkillId(), runeIdToAdd.getLevel()), true);

        if(runeIdToRemove !=null)
            _player.removeSkill(SkillTable.getInstance().getInfo(runeIdToRemove.getSkillId(), runeIdToRemove.getLevel()), true);
    }

    public void saveRune(Rune rune){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO runes_player (playerId,runeId,runeLevel,runeExp) VALUES (?,?,?,?)");
            statement.setInt(1, _player.getObjectId());
            statement.setInt(2, rune.getId());
            statement.setInt(3, rune.getLevel());
            statement.setDouble(4, rune.getExp());
            statement.execute();
            statement.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
