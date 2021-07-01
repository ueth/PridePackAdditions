package net.sf.l2j.gameserver.fairgames;

import cz.nxs.events.engine.base.Loc;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {
    private L2PcInstance _player;
    private int _instanceId;
    private Loc _oldLocPlayer;
    private Loc _locPlayer ;
    private List<L2Skill> _oldSkills = new ArrayList<>();
    private int _damage = 0;

    public PlayerHandler(L2PcInstance player, int instanceId) {
        _player = player;
        _instanceId = instanceId;
    }

    public boolean teleportPlayerToArena() {
        if (_player == null || _player.isOnline() != 1)
            return false;
        System.out.println("teleportPlayerToArena");

        prepPlayer();
        _player.setInstanceId(_instanceId);
        _player.teleToLocation(_locPlayer.getX(), _locPlayer.getY(), _locPlayer.getZ());

        return true;
    }

    public void teleportPlayerBack() {
        if(_player == null || _player.isOnline() != 1)
            return;

        for (L2Skill skill : _player.getAllSkills()) {
            _player.removeSkill(skill, false); //Remove added skills by the event
        }

        for(L2Skill skill : _oldSkills){
            if(skill != null)
                _player.addSkill(skill, false); //Give player his old skills
        }
        PlayerSaves.getInstance().removePreviousSkills(_player.getObjectId());

        _player.unEquipItems();

        for(int objectId : PlayerSaves.getInstance().getPreviousWear(_player.getObjectId())){
            L2ItemInstance item = _player.getInventory().getItemByObjectId(objectId);
            if(item!=null)
                _player.getInventory().equipItem(item);
        }
        PlayerSaves.getInstance().removePreviousWear(_player.getObjectId());

        for(int objectId : PlayerSaves.getInstance().getItemsToDelete(_player.getObjectId())){
            L2ItemInstance item = _player.getInventory().getItemByObjectId(objectId);
            if(item!=null){
                _player.getInventory().destroyItem("destroy", item, _player, null);
            }
        }
        PlayerSaves.getInstance().removeItemsToDelete(_player.getObjectId());

        _player.setTarget(null);
        _player.setIsRooted(false);
        _player.doRevive();
        _player.setInvisible(false);
        _player.setFairGame(false);
        _player.setInstanceId(0);
        _player.teleToLocation(_oldLocPlayer.getX(), _oldLocPlayer.getY(), _oldLocPlayer.getZ(), true);
        _player.sendSkillList();
    }

    /**
     * Called when the choose build phase is over and fighting has to start
     */
    public void fight(){
        if(_player.isOnline()!=1)
            return;

        _player.setIsRooted(false);
        _player.setInvisible(false);
        _player.broadcastUserInfo();
    }

    public void sendPlayerClock(int clock){
        _player.sendPacket(new ExShowScreenMessage(1, -1, 4, 0, 1, 0, 0, true, 1000, 0, clock+""));
    }

    public void increaseDamage(int damage){
        _damage += damage;
    }

    //public void

    /**
     * A method that prepares the player before teleporting him for battle
     */
    public synchronized void prepPlayer() {
        if(_player.isOnline()!=1)
            return;

        try{
            _oldLocPlayer = new Loc(_player.getX(), _player.getY(), _player.getZ()); // save player's old location

            for (L2Skill skill : _player.getAllSkills()) {
                _oldSkills.add(skill);
                _player.removeSkill(skill, false);
            }

            PlayerSaves.getInstance().addPreviousSkills(_player.getObjectId(), _oldSkills);
            PlayerSaves.getInstance().addPreviousWear(_player.getObjectId(), _player.unEquipItems());
            addItems();

            if (_player.getPet() != null)
                _player.getPet().unSummon(_player);

            _player.setFairGame(true);
            _player.addSkill(35100); //Adding the skill that equalizes player's stats
            _player.sendSkillList();

            if (_player.isSitting())
                _player.standUp();

            _locPlayer.addRadius(0);
            _player.setTarget(null);
            _player.setIsRooted(true);
            _player.doRevive();
            _player.setInvisible(true);
            _player.broadcastUserInfo();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addItems(){
        List<Integer> items = new ArrayList<>();

        L2ItemInstance item  = _player.addItem("asdf", 90000,1,null, false);
        items.add(item.getObjectId());
        _player.getInventory().equipItem(item);

        item  = _player.addItem("asdf", 71001,1,null, false);
        items.add(item.getObjectId());
        _player.getInventory().equipItem(item);

        item  = _player.addItem("asdf", 71002,1,null, false);
        items.add(item.getObjectId());
        _player.getInventory().equipItem(item);

        PlayerSaves.getInstance().addItemsToDelete(_player.getObjectId(), items);

    }

    public L2PcInstance getPlayer(){
        return _player;
    }

    public void setLoc(int x, int y, int z){
        _locPlayer = new Loc(x, y, z);
    }
}
