package net.sf.l2j.gameserver.fairgames;

import cz.nxs.events.engine.base.Loc;
import net.sf.l2j.gameserver.fairgames.classes.AbstractFGClass;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {
    private L2PcInstance _player;
    private int _instanceId;
    private Loc _oldLocPlayer;
    private Loc _locPlayer ;
    private List<L2Skill> _oldSkills = new ArrayList<>();
    private int _damage = 0;
    private AbstractFGClass _class;

    public PlayerHandler(L2PcInstance player, int instanceId) {
        _player = player;
        _instanceId = instanceId;
        player.setPlayerHandler(this);
    }

    public boolean teleportPlayerToArena() {
        if (_player == null || _player.isOnline() != 1)
            return false;
        System.out.println("teleportPlayerToArena");

        prepPlayer();
        InstanceManager.getInstance().getInstance(_instanceId).addPlayer(_player.getObjectId());
        _player.setInstanceId(_instanceId);
        _player.teleToLocation(_locPlayer.getX(), _locPlayer.getY(), _locPlayer.getZ());

        return true;
    }

    public void teleportPlayerBack() {
        if(_player == null || _player.isOnline() != 1)
            return;

        for (L2Skill skill : _player.getAllSkills()) {
            _player.removeSkill(skill, false); //Remove all added skills by the event
        }

        for (L2Effect effect : _player.getAllEffects())
            if (effect != null && effect.getSkill() != null) //Remove all added effects
                effect.exit();

        for(L2Skill skill : PlayerSaves.getInstance().getPreviousEffects(_player.getObjectId()))
            if(skill!=null)
                skill.getEffects(_player,_player);
        PlayerSaves.getInstance().clearPreviousEffects(_player.getObjectId());

        for(L2Skill skill : _oldSkills){
            if(skill != null)
                _player.addSkill(skill, false); //Give player his old skills
        }
        PlayerSaves.getInstance().clearPreviousSkills(_player.getObjectId());

        _player.unEquipItems();

        for(int objectId : PlayerSaves.getInstance().getPreviousWear(_player.getObjectId())){
            L2ItemInstance item = _player.getInventory().getItemByObjectId(objectId);
            if(item!=null)
                _player.getInventory().equipItem(item);
        }
        PlayerSaves.getInstance().clearPreviousWear(_player.getObjectId());

        for(int objectId : PlayerSaves.getInstance().getItemsToDelete(_player.getObjectId())){
            L2ItemInstance item = _player.getInventory().getItemByObjectId(objectId);
            if(item!=null){
                _player.getInventory().destroyItem("destroy", item, _player, null);
            }
        }
        PlayerSaves.getInstance().clearItemsToDelete(_player.getObjectId());

        PlayerSaves.getInstance().deleteEverythingFromDB(_player.getObjectId());

        _player.setTarget(null);
        _player.setIsRooted(false);
        _player.doRevive();
        _player.setInvisible(false);
        _player.setFairGame(false);
        _player.setInstanceId(0);
        _player.teleToLocation(_oldLocPlayer.getX(), _oldLocPlayer.getY(), _oldLocPlayer.getZ(), true);
        _player.sendSkillList();
        _player.broadcastUserInfo();
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
        int minutes = clock/60;
        int seconds = clock%60;

        String minutesString;
        String secondsString;

        if(minutes<10)
            minutesString = "0"+minutes;
        else
            minutesString = minutes+"";


        if(seconds<10)
            secondsString = "0"+seconds;
        else
            secondsString = seconds+"";


        String time = minutesString+":"+secondsString;

        _player.sendPacket(new ExShowScreenMessage(1, -1, 4, 0, 1, 0, 0, true, 2000, 0, time));
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

            if(_player.isTransformed())
                _player.untransform();

            for (L2Effect effect : _player.getAllEffects())
                if (effect != null && effect.getSkill() != null && !effect.getSkill().isPositive())
                    effect.exit();

            List<L2Skill> skillEffect = new ArrayList<>();
            for (L2Effect effect : _player.getAllEffects())
                if (effect != null && effect.getSkill() != null) {
                    skillEffect.add(effect.getSkill());
                    effect.exit();
                }
            PlayerSaves.getInstance().addPreviousEffects(_player.getObjectId(), skillEffect);

            for (L2Skill skill : _player.getAllSkills()) {
                _oldSkills.add(skill);
                _player.removeSkill(skill, false);
            }

            PlayerSaves.getInstance().addPreviousSkills(_player.getObjectId(), _oldSkills);
            PlayerSaves.getInstance().addPreviousWear(_player.getObjectId(), _player.unEquipItems());

            addItems();

            PlayerSaves.getInstance().saveEverythingInDB(_player.getObjectId());

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

    public int getDamage(){
        return _damage;
    }
}
