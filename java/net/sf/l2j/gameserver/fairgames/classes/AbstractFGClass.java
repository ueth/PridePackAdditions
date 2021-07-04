package net.sf.l2j.gameserver.fairgames.classes;

public abstract class AbstractFGClass {
    protected int _skillCounter = 0;
    protected int _buffCounter = 0;
    protected int _dancesongCounter = 0;
    protected String _name;

    public void incSkillCounter(){
        _skillCounter++;
    }
    public void incBuffCounter(){
        _buffCounter++;
    }
    public void incDanceSongCounter(){
        _dancesongCounter++;
    }


    public int getSkillCounter() {
        return _skillCounter;
    }
    public int getBuffCounter() {
        return _buffCounter;
    }
    public int getDanceSongCounter() {
        return _dancesongCounter;
    }

    public String getName(){
        return _name;
    }

    public abstract int getMaxBuffs();
    public abstract int getMaxSkills();
    public abstract int getMaxDanceSongs();
}
