package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BattlePassPlayer implements Cloneable{
    private List<BattlePass> _battlePasses = new ArrayList<>(BattlePassTable.getBattlePasses().size());

    public BattlePassPlayer(L2PcInstance player) {
        loadPreviousBattlePasses(player);
    }

    public List<BattlePass> getBattlePasses(){
        return _battlePasses;
    }

    public void loadPreviousBattlePasses(L2PcInstance player){
        int counter = 0;

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM battle_pass_player where playerId=?");
            statement.setInt(1, player.getObjectId());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                counter++;
                for(BattlePass battlePass : BattlePassTable.getBattlePasses().values()){
                    if(battlePass.getId() == rs.getInt("battlePassId")){
                        _battlePasses.add((BattlePass) battlePass.clone());
                        battlePass.setPlayer(player);
                        battlePass.setPoints(rs.getInt("points"));
                        battlePass.setAvailability(rs.getInt("availability") == 1 ? true : false);
                    }
                }
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveBattlePass(int id, L2PcInstance player){
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

    public static void updateBattlePass(int id, L2PcInstance player, double points){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE battle_pass_player SET battlePassId=?,points=? WHERE playerId=?");
            stm.setLong(1, id);
            stm.setDouble(2, points);
            stm.setInt(3, player.getObjectId());
            stm.execute();
            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }
}
