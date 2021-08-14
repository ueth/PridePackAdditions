package net.sf.l2j.gameserver.fairgames.classes;

public class Assassin extends AbstractFGClass{
    protected static final int MAX_SKILLS = 10;
    protected static final int MAX_BUFFS = 7;

    public Assassin() {
        _name = "assassin";
    }

    @Override
    public int getMaxBuffs() {
        return MAX_BUFFS;
    }

    @Override
    public int getMaxSkills() {
        return MAX_SKILLS;
    }


}
