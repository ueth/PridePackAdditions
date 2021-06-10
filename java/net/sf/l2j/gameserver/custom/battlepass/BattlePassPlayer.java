package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BattlePassPlayer{
    private List<BattlePass> _battlePasses = new ArrayList<>(BattlePassTable.getBattlePasses().size());
    private BattlePassPages _battlePassPages;

    public BattlePassPlayer(L2PcInstance player) {
        _battlePassPages = new BattlePassPages(player);
    }

    public List<BattlePass> getBattlePasses(){
        return _battlePasses;
    }

    public BattlePassPages getBattlePassPages(){
        return _battlePassPages;
    }

    public void loadPreviousBattlePasses(L2PcInstance player){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM battle_pass_player where playerId=?");
            statement.setInt(1, player.getObjectId());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                BattlePass battlePassClone = (BattlePass) BattlePassTable.getBattlePasses().get(rs.getInt("battlePassId")).clone();
                battlePassClone.setPlayer(player);
                battlePassClone.setPoints(rs.getDouble("points"));
                battlePassClone.setRewarded(rs.getInt("rewarded"));
                battlePassClone.setAvailability(rs.getInt("availability") == 1 ? true : false);
                _battlePasses.add(battlePassClone);
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveBattlePass(int id, L2PcInstance player){

        BattlePass battlePassClone = null;
        try {
            battlePassClone = (BattlePass) BattlePassTable.getBattlePasses().get(id).clone();
        } catch (CloneNotSupportedException e) { e.printStackTrace(); }
        battlePassClone.setPlayer(player);
        battlePassClone.setPoints(0);
        battlePassClone.setAvailability(true);
        _battlePasses.add(battlePassClone);

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO battle_pass_player (playerId,battlePassId,availability,points) VALUES (?,?,?,?)");
            statement.setInt(1, player.getObjectId());
            statement.setInt(2, id);
            statement.setInt(3, 1);
            statement.setInt(4, 0);
            statement.execute();
            statement.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateBattlePass(int id, L2PcInstance player, double points, int rewarded){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE battle_pass_player SET points=?,rewarded=? WHERE playerId=? and battlePassId=?");

            stm.setDouble(1, points);
            stm.setInt(2, rewarded);
            stm.setInt(3, player.getObjectId());
            stm.setLong(4, id);
            stm.execute();
            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateBattlePassAvailability(boolean bool, L2PcInstance player, int battlePassId){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE battle_pass_player SET availability=? WHERE playerId=? and battlePassId=?");
            stm.setInt(2, player.getObjectId());
            stm.setInt(3, battlePassId);
            if(bool)
                stm.setInt(1, 1);
            else
                stm.setInt(1, 0);
            stm.execute();
            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }

}
