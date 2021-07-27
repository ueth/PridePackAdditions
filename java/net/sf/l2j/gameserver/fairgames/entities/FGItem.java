package net.sf.l2j.gameserver.fairgames.entities;

public class FGItem {
    private int _itemId;
    private String _desc;

    public FGItem(int _itemId, String _desc) {
        this._itemId = _itemId;
        this._desc = _desc;
    }

    public int getItemId() { return _itemId; }

    public void setItemId(int _itemId) { this._itemId = _itemId; }

    public String getDesc() { return _desc; }

    public void setDesc(String _desc) { this._desc = _desc; }
}
