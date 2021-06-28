package net.sf.l2j.gameserver.fairgames.classes;

import java.util.ArrayList;
import java.util.List;

public class Assassin extends AbstractFGClass{
    private static final int[] SKILLS = {395, 396, 1374, 1375, 1376, 12504, 12503, 12502, 12505, 12501, 12506, 12511, 12509, 12508, 12510};

    @Override
    public int[] getAllSkills() {
        return SKILLS;
    }
}
