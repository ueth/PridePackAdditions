package net.sf.l2j.gameserver.fairgames.classes;

public class Warrior extends AbstractFGClass{
    protected static final int MAX_SKILLS = 10;
    protected static final int MAX_BUFFS = 7;
    protected static final int MAX_DANCESONGS = 3;

    public Warrior() {
        _name = "warrior";
    }

    @Override
    public int getMaxBuffs() {
        return MAX_BUFFS;
    }

    @Override
    public int getMaxSkills() {
        return MAX_SKILLS;
    }

    @Override
    public int getMaxDanceSongs() {
        return MAX_DANCESONGS;
    }

}
