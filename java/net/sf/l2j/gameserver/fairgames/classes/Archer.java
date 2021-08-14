package net.sf.l2j.gameserver.fairgames.classes;

public class Archer extends AbstractFGClass{
    protected static final int MAX_SKILLS = 10;
    protected static final int MAX_BUFFS = 7;

    public Archer() {
        _name = "archer";
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
