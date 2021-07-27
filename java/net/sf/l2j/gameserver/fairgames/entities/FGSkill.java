package net.sf.l2j.gameserver.fairgames.entities;

public class FGSkill {
    private int _id;
    private String _name;
    private String _icon;
    private String _desc;

    public FGSkill(int _id, String _name, String _icon, String _desc) {
        this._id = _id;
        this._name = _name;
        this._icon = _icon;
        this._desc = _desc;
    }

    public int getId() { return _id; }

    public void setId(int _id) { this._id = _id; }

    public String getName() { return _name; }

    public void setName(String _name) { this._name = _name; }

    public String getIcon() { return _icon; }

    public void setIcon(String _icon) { this._icon = _icon; }

    public String getDesc() { return _desc; }

    public void setDesc(String _desc) { this._desc = _desc; }
}
