package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.Map;

public class BattlePass implements Cloneable{
    private L2PcInstance _player;
    private double _points;
    private int _id;
    private String _name;
    private Map<Integer, Reward> _rewards;
    private int _price;
    private int _itemId;
    private boolean _availability;

    public BattlePass(L2PcInstance player, double points, String name, Map<Integer, Reward> rewards, boolean availability, int id, int price, int itemId){
        _points = points;
        _name = name;
        _id = id;
        _rewards = rewards;
        _availability = availability;
        _player = player;
        _price = price;
        _itemId = itemId;
    }

    public void increasePoints(boolean isPvP, int hp, int pdef, int mdef, int patk, int matk){
        if(!_availability)
            return;

        if(isPvP)
            _points += 0.004;
        else
            _points += hp/50000000 + pdef/500000 + mdef/500000 + patk/1000000 + matk/1000000;
        update();
    }

    public void setPoints(int points){
        _points = points;
    }

    public boolean isAvailable() {
        return _availability;
    }

    public void setAvailability(boolean availability) {
        _availability = availability;
    }

    /**
     * This method is called every time _points are increasing
     */
    public void update(){
        int maxRewardNumber = _rewards.size();
        for(int i = maxRewardNumber-1; i >= 0; i--){
            if(_points >= i+1 && _availability){
                goodies(_rewards.get(i).getItemId(), _rewards.get(i).getAmount());
                break;
            }
        }
    }

    private void goodies(int itemId, int amount){
        if(!_availability)
            return;

        _player.addItem("BattlePassReward", itemId, amount, null, false);
        //add skill animation and html window
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new BattlePass(_player, _points, _name, _rewards, _availability, _id, _price, _itemId);
    }
}
