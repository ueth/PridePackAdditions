package net.sf.l2j.gameserver.fairgames.classes;

public class Mage extends AbstractFGClass{
    protected static final int MAX_SKILLS = 10;
    protected static final int MAX_BUFFS = 7;

    public Mage() {
        _name = "mage";
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
