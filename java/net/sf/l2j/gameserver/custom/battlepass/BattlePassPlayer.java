package net.sf.l2j.gameserver.custom.battlepass;

import java.util.ArrayList;
import java.util.List;

public class BattlePassPlayer implements Cloneable{
    private List<BattlePass> _battlePasses = new ArrayList<>(BattlePassTable.getBattlePasses().size());

    BattlePassPlayer() {
        for(BattlePass battlePass : BattlePassTable.getBattlePasses()) {
            try {
                _battlePasses.add((BattlePass) battlePass.clone());
            } catch (CloneNotSupportedException e) { e.printStackTrace(); }
        }
    }
}
