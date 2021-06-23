package net.sf.l2j.gameserver.custom.runes;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.ArrayList;
import java.util.List;

public class RunePages {
    private List<String> _runePages;
    private L2PcInstance _player;

    public RunePages (L2PcInstance player) {
        _runePages = new ArrayList<>();
        _player = player;
    }

}
