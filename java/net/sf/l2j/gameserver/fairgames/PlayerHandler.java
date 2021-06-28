package net.sf.l2j.gameserver.fairgames;

import cz.nxs.events.engine.base.Loc;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {
    private L2PcInstance _player;
    private int _instanceId;
    private Loc _oldLocPlayer;
    private Loc _locPlayer = new Loc(0, 0, 0);
    private List<L2Skill> _oldSkills = new ArrayList<>();

    public PlayerHandler(L2PcInstance player, int instanceId) {
        _player = player;
        _instanceId = instanceId;
    }

    public boolean teleportPlayerToArena() {
        if (_player == null || _player.isOnline() != 1)
            return false;

        prepPlayer();
        _player.setInstanceId(_instanceId);
        _player.teleToLocation(_locPlayer.getX(), _locPlayer.getY(), _locPlayer.getZ());

        return true;
    }

    public void teleportPlayerBack() {
        if(_player == null)
            return;

        for (L2Skill skill : _player.getAllSkills()) {
            _player.removeSkill(skill, false); //Remove added skills by the event
        }

        for(L2Skill skill : _oldSkills){
            _player.addSkill(skill, false); //Give player his old skills
        }

        _player.setFairGame(false);
        _player.setInstanceId(0);
        _player.teleToLocation(_oldLocPlayer.getX(), _oldLocPlayer.getY(), _oldLocPlayer.getZ(), true);
        _player.sendSkillList();
    }

    /**
     * A method that prepares the player before teleporting him for battle
     */
    public void prepPlayer() {
        if(_player == null)
            return;

        _oldLocPlayer = new Loc(_player.getX(), _player.getY(), _player.getZ()); // save player's old location

        for (L2Skill skill : _player.getAllSkills()) {
            _oldSkills.add(skill);
            _player.removeSkill(skill, false);
        }

        _player.setFairGame(true);
        _player.addSkill(SkillTable.getInstance().getInfo(35150, 1), false); //Adding the skill that equalizes player's stats
        _player.sendSkillList();

        if (_player.isSitting())
            _player.standUp();

        _locPlayer.addRadius(0);
        _player.setTarget(null);
        _player.setIsRooted(true);
        _player.doRevive();
        _player.setInvisible(true);
    }

    public L2PcInstance getPlayer(){
        return _player;
    }
}
