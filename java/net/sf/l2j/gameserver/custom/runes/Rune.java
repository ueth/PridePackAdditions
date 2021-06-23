package net.sf.l2j.gameserver.custom.runes;

public class Rune {
    private int _level;
    private int _maxLevel;
    private int _id;
    private String _name;
    private double _exp;
    private int _skillId;

    public Rune(int _level, int _maxLevel, int _id, String _name, double _exp, int _skillId) {
        this._level = _level;
        this._maxLevel = _maxLevel;
        this._id = _id;
        this._name = _name;
        this._exp = _exp;
        this._skillId = _skillId;
    }

    @Override
    public String toString() {
        return "Rune{" +
                "_level=" + _level +
                ", _maxLevel=" + _maxLevel +
                ", _id=" + _id +
                ", _name='" + _name + '\'' +
                ", _exp=" + _exp +
                ", _skillId=" + _skillId +
                '}';
    }

    public void increaseExp(boolean isPvP, double hp, double pdef, double mdef, double patk, double matk){
        if(isPvP)
            _exp += 0.75;
        else
            _exp += hp/1200000 + pdef/50000 + mdef/50000 + patk/100000 + matk/50000;

        checkForLevelUp();
    }

    private void checkForLevelUp(){
        if(_exp >= 100*_level && _level < _maxLevel) {
            _level++;
            _exp = 0.0;
        }
    }

    public int getLevel() { return _level; }

    public void setLevel(int _level) { this._level = _level; }

    public int getId() { return _id; }

    public void setId(int _id) { this._id = _id; }

    public String getName() { return _name; }

    public void setName(String _name) { this._name = _name; }

    public double getExp() { return _exp; }

    public void setExp(double _exp) { this._exp = _exp; }

    public int getSkillId() { return _skillId; }

    public void setSkillId(int _skillId) { this._skillId = _skillId; }

    public int getMaxLevel() { return _maxLevel; }

    public void setMaxLevel(int _maxLevel) { this._maxLevel = _maxLevel; }
}
