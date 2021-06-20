package net.sf.l2j.gameserver.custom.runes;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class RuneItem implements IItemHandler {

    @Override
    public void useItem(L2Playable playable, L2ItemInstance item) {
        if (!(playable instanceof L2PcInstance))
            return;

        L2PcInstance player = (L2PcInstance) playable;

        player.getRunePlayer().addRune(RuneTable.getRune(item.getItemId()));
    }
}
