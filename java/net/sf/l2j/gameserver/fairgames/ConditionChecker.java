package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.fairgames.configurations.ConfigManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class ConditionChecker {
    private static final int _pvps = ConfigManager.getInstance().getMinPvp();
    private static final int _pks = ConfigManager.getInstance().getMinPk();
    private static final int _lvl = ConfigManager.getInstance().getMinLvl();
    private static final int _points = 10;

    public static boolean validate(L2PcInstance player){
        if(player.getPvpKills()<_pvps){
            player.sendMessage("You need "+_pvps+" pvp to enter Fair Games");
            return false;
        }
        if(player.getPvpKills()<_pks){
            player.sendMessage("You need "+_pks+" pk to enter Fair Games");
            return false;
        }
        if(player.getPvpKills()<_lvl){
            player.sendMessage("You need to be at least "+_lvl+" levels to enter Fair Games");
            return false;
        }
        if(player.getPlayerStats().getPoints() < _points){
            player.sendMessage("You need to be have at least "+_points+" points to enter Fair Games");
            return false;
        }

        return true;
    }
}
