package net.sf.l2j.gameserver.custom.battlepass;

public class Reward {
    private int _itemId;
    private int _amount;

    public Reward(int _itemId, int _amount) {
        this._itemId = _itemId;
        this._amount = _amount;
    }

    public int getItemId(){
        return _itemId;
    }
    public int getAmount(){
        return _amount;
    }
}
