package net.sf.l2j.gameserver.fairgames.classes;

public abstract class AbstractFGClass {
    protected int _skillCounter = 0;
    protected int _buffCounter = 0;
    protected String _name;

    public void incSkillCounter(){
        _skillCounter++;
    }
    public void incBuffCounter(){
        _buffCounter++;
    }


    public int getSkillCounter() {
        return _skillCounter;
    }
    public int getBuffCounter() {
        return _buffCounter;
    }

    public String getName(){
        return _name;
    }

    public void resetCounters(){
        _skillCounter = 0;
        _buffCounter = 0;
    }

    public abstract int getMaxBuffs();
    public abstract int getMaxSkills();
}
