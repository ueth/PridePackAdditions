package net.sf.l2j.gameserver.custom.battlepass;

import java.util.ArrayList;
import java.util.List;

/**
 * This class loads all the battle passes from the sql and stores
 * them in a list
 * If more than 1 id is found more battle passes will be created
 */
public class BattlePassTable {

    private static List<BattlePass> _battlePasses = new ArrayList<>();

    public static List<BattlePass> getBattlePasses() {
        return _battlePasses;
    }

    public void setBattlePasses(List<BattlePass> battlePasses) {
        _battlePasses = battlePasses;
    }

    //TODO
    /**
     * Create an sql table that will have all the battlepasses by id and name
     *
     */
}
