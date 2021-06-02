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

    public void increasePoints(){
        if(!_availability)
            return;

        _player.sendMessage("Got into increase points");

        _points +=0.5;

        update();
    }

    public void increasePoints(boolean isPvP, int hp, int pdef, int mdef, int patk, int matk){
        if(!_availability)
            return;

        _points +=0.5;



        /*if(isPvP)
            _points += 0.004;
        else
            _points += hp/50000000 + pdef/500000 + mdef/500000 + patk/1000000 + matk/1000000;*/
        update();
    }

    public void setPoints(int points){ _points = points; }

    public boolean isAvailable() {
        return _availability;
    }

    public void setAvailability(boolean availability) {
        _availability = availability;
    }

    public void setPlayer(L2PcInstance player){ _player = player; }

    public void addReward(int id, Reward reward){ _rewards.put(id, reward); }

    public int getId(){
        return _id;
    }

    /**
     * This method is called every time _points are increasing
     */
    public void update(){
        _player.sendMessage("Got into update");
        BattlePassPlayer.updateBattlePass(_id, _player, _points);
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

        NpcHtmlMessage html = new NpcHtmlMessage(1);
        html.setHtml("<html><body>Test text<br></body></html>");
        _player.sendPacket(html);

        MagicSkillUse msk = new MagicSkillUse(_player, _player, 837, 1, 1000, 0);
        Broadcast.toSelfAndKnownPlayersInRadius(_player, msk, 1000000);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new BattlePass(_player, _points, _name, _availability, _id, _price, _itemId, _minLvl);
    }
}
