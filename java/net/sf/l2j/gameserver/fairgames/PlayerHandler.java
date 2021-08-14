package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.fairgames.build.ItemsPages;
import net.sf.l2j.gameserver.fairgames.classes.AbstractFGClass;
import net.sf.l2j.gameserver.fairgames.build.SkillsPages;
import net.sf.l2j.gameserver.fairgames.configurations.ConfigManager;
import net.sf.l2j.gameserver.fairgames.enums.BuildStage;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import java.util.ArrayList;
import java.util.List;

public class PlayerHandler {
    private L2PcInstance _player;
    private int _instanceId;
    private Location _oldLocPlayer;
    private Location _locPlayer ;
    private List<L2Skill> _oldSkills = new ArrayList<>();
    private int _damage = 0;
    private AbstractFGClass _class;
    private BuildStage _buildStage;

    private int _armorCounter = 0;
    private int _jewelCounter = 0;

    private SkillsPages _skillsPages;
    private ItemsPages _itemsPages;

    public PlayerHandler(L2PcInstance player, int instanceId) {
        _player = player;
        _instanceId = instanceId;
        _skillsPages = new SkillsPages(player);
        _itemsPages = new ItemsPages( player);
        _buildStage = BuildStage.SKILLS_CHOOSE;
        _class = player.getFGClass();
        _class.resetCounters();
        player.setPlayerHandler(this);
    }

    public boolean teleportPlayerToArena() {
        if (_player == null || _player.isOnline() != 1)
            return false;

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

        PlayerSaves.getInstance().doItAll(_player);

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

        _player.setProtection(false);
        _buildStage = BuildStage.NONE;
        _player.setIsRooted(false);
        _player.setInvisible(false);
        _player.setIsInvul(false);
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

        _player.sendPacket(new ExShowScreenMessage(1, -1, ConfigManager.getInstance().getClockPosition(), 0, 1, 0, 0, true, 2000, 0, time));
    }

    public void increaseDamage(int damage){
        _damage += damage;
    }

    /**
     * A method that prepares the player before teleporting him for battle
     */
    public synchronized void prepPlayer() {
        if(_player.isOnline()!=1)
            return;

        try{
            _oldLocPlayer = new Location(_player.getX(), _player.getY(), _player.getZ()); // save player's old location

            PlayerSaves.getInstance().initItemsToDelete(_player.getObjectId()); //Initializing player saves items to delete to avoid null pointer exceptions

            if(_player.isTransformed())
                _player.untransform();

            //Removes debuffs before entering fair games and before saving buffs
            for (L2Effect effect : _player.getAllEffects())
                if (effect != null && effect.getSkill() != null && !effect.getSkill().isPositive())
                    effect.exit();
            List<L2Skill> skillEffect = new ArrayList<>();

            //Saves player's buffs
            for (L2Effect effect : _player.getAllEffects())
                if (effect != null && effect.getSkill() != null) {
                    skillEffect.add(effect.getSkill());
                    effect.exit();
                }
            PlayerSaves.getInstance().addPreviousEffects(_player.getObjectId(), skillEffect);

            //Saves player's skills
            for (L2Skill skill : _player.getAllSkills()) {
                _oldSkills.add(skill);
                _player.removeSkill(skill, false);
            }

            PlayerSaves.getInstance().addPreviousSkills(_player.getObjectId(), _oldSkills);
            PlayerSaves.getInstance().addPreviousWear(_player.getObjectId(), _player.unEquipItems());//unequips items and save them

            PlayerSaves.getInstance().saveEverythingInDB(_player.getObjectId());

            if (_player.getPet() != null)
                _player.getPet().unSummon(_player);

            _player.setFairGame(true);
            _player.addSkill(35100); //Adding the skill that equalizes player's stats
            _player.sendSkillList();

            if (_player.isSitting())
                _player.standUp();

            _locPlayer.setHeading(0);
            _player.setTarget(null);
            _player.setIsRooted(true);
            _player.doRevive();
            _player.setInvisible(true);
            _player.broadcastUserInfo();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addItem(int id){
        L2ItemInstance item  = _player.addItem("Fair Games Item", id,1,null, false);
        _player.getInventory().equipItem(item);
        PlayerSaves.getInstance().addItemToDelete(_player.getObjectId(), item.getObjectId());
    }

//    public void addItems(){
//        List<Integer> items = new ArrayList<>();
//
//        L2ItemInstance item  = _player.addItem("asdf", 71001,1,null, false);
//        items.add(item.getObjectId());
//        _player.getInventory().equipItem(item);
//
//        item  = _player.addItem("asdf", 71002,1,null, false);
//        items.add(item.getObjectId());
//        _player.getInventory().equipItem(item);
//
//        PlayerSaves.getInstance().addItemsToDelete(_player.getObjectId(), items);
//    }

    public AbstractFGClass getFGClass(){
        return _class;
    }

    public String getClassName(){
        return _class.getName();
    }

    public void setClass(AbstractFGClass _class){this._class = _class;}

    public L2PcInstance getPlayer(){
        return _player;
    }

    public void setLoc(int x, int y, int z){
        _locPlayer = new Location(x, y, z);
    }

    public int getDamage(){
        return _damage;
    }

    public SkillsPages getSkillPages(){
        return _skillsPages;
    }

    public ItemsPages getItemPages(){
        return _itemsPages;
    }

    public BuildStage getBuildStage() {
        return _buildStage;
    }

    public int getArmorCounter(){ return _armorCounter;}

    public int getJewelCounter(){ return _jewelCounter;}

    public void switchBuildStage(){
        switch (_buildStage){
//            case CLASS_CHOOSE:
//                _buildStage = BuildStage.SKILLS_CHOOSE;
//                break;

            case SKILLS_CHOOSE:
                _buildStage = BuildStage.WEAPON_CHOOSE;
                break;

            case WEAPON_CHOOSE:
                if(_player.getSecondaryWeaponInstance() != null)
                    _buildStage = BuildStage.ARMOR_CHOOSE;
                else
                    _buildStage = BuildStage.SHIELD_CHOOSE;

                break;

            case SHIELD_CHOOSE:
                _buildStage = BuildStage.ARMOR_CHOOSE;
                break;

            case ARMOR_CHOOSE:
                _armorCounter++;
                if(_armorCounter == 5)
                    _buildStage = BuildStage.JEWELS_CHOOSE;
                break;

            case JEWELS_CHOOSE:
                _jewelCounter++;
                if(_jewelCounter == 5)
                    _buildStage = BuildStage.TATTOO_CHOOSE;
                break;

            case TATTOO_CHOOSE:
                _buildStage = BuildStage.BUFFS_CHOOSE;
                break;

            case BUFFS_CHOOSE:
                _buildStage = BuildStage.NONE;
                break;
        }
    }
}
