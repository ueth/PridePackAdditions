package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Broadcast;

import java.util.HashMap;
import java.util.Map;

public class BattlePass implements Cloneable{
    private L2PcInstance _player;
    private double _points;
    private int _id;
    private String _name;
    private Map<Integer, Reward> _rewards;
    private int _minLvl;
    private int _price;
    private int _itemId;
    private boolean _availability;
    private int _rewarded;

    public BattlePass(L2PcInstance player, double points, String name, boolean availability, int id, int price, int itemId, int minLvl){
        _points = points;
        _name = name;
        _id = id;
        _rewards = new HashMap<>();
        _availability = availability;
        _player = player;
        _price = price;
        _itemId = itemId;
        _minLvl = minLvl;
    }

    public BattlePass(L2PcInstance player, double points, String name, boolean availability, int id, int price, int itemId, int minLvl, Map<Integer, Reward> rewards, int rewarded){
        _points = points;
        _name = name;
        _id = id;
        _rewards = rewards;
        _availability = availability;
        _player = player;
        _price = price;
        _itemId = itemId;
        _minLvl = minLvl;
        _rewarded = rewarded;
    }

    public void increasePoints(){
        if(!_availability)
            return;

        _points +=0.5;

        update();
    }

    public void increasePoints(boolean isPvP, double hp, double pdef, double mdef, double patk, double matk){
        if(!_availability)
            return;

        if(isPvP)
            _points += 0.004;
        else{
            _points += hp/12000000 + pdef/500000 + mdef/500000 + patk/1000000 + matk/1000000;
        }

        update();
    }

    public void setPoints(double points){ _points = points; }

    public double getPoints(){return _points;}

    public boolean isAvailable() {
        return _availability;
    }

    public void setAvailability(boolean availability) {
        _availability = availability;
    }

    public void setPlayer(L2PcInstance player){ _player = player; }

    public void addReward(int id, Reward reward){ _rewards.put(id, reward); }

    public Map<Integer, Reward> getRewards(){
        return _rewards;
    }

    public String getName(){ return _name; }

    public int getPrice(){return _price;}

    public int getId(){
        return _id;
    }

    public int getItemId(){return _itemId;}

    public void setRewarded(int i){_rewarded = i;}

    /**
     * This method is called every time _points are increasing
     */
    public void update(){
        if(!_availability)
            return;

        BattlePassPlayer.updateBattlePass(_id, _player, _points, _rewarded);
        int maxRewardNumber = _rewards.size();

        if(_points >= _rewarded+1 && _rewarded <= maxRewardNumber-1) {
            BattlePassPlayer.updateBattlePass(_id, _player, _points, ++_rewarded);
            goodies(_rewards.get(_rewarded).getItemId(), _rewards.get(_rewarded).getAmount());
        }

        if(_points > maxRewardNumber && _availability) {
            _points = maxRewardNumber;
            BattlePassPlayer.updateBattlePassAvailability(_availability=false, _player, _id);
            return;
        }
    }

    private void goodies(int itemId, int amount){
        if(!_availability)
            return;

        _player.addItem("BattlePassReward", itemId, amount, null, true);

        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setHtml("<html><body>Test text<br></body></html>");
        _player.sendPacket(html);

        MagicSkillUse msk = new MagicSkillUse(_player, _player, 5103, 1, 0, 0);
        Broadcast.toSelfAndKnownPlayersInRadius(_player, msk, 5000);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new BattlePass(_player, _points, _name, _availability, _id, _price, _itemId, _minLvl, _rewards, _rewarded);
    }
}
