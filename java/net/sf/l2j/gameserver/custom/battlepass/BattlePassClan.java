package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BattlePassClan {

    private List<BattlePass> _battlePasses;
    private BattlePassClanPages _battlePassClanPages;

    public BattlePassClan(L2Clan clan) {
        _battlePasses = new ArrayList<>(BattlePassTable.getClanBattlePasses().size());
        _battlePassClanPages = new BattlePassClanPages(clan);
    }

    public List<BattlePass> getBattlePasses(){
        return _battlePasses;
    }

    public BattlePassClanPages getBattlePassClanPages(){
        return _battlePassClanPages;
    }

    public void loadPreviousBattlePasses(L2Clan clan){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM battle_pass_clan where clanId=?");
            statement.setInt(1, clan.getClanId());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                BattlePass battlePassClone = BattlePassTable.getClanBattlePasses().get(rs.getInt("battlePassId")).cloneClanBattlePass();
                battlePassClone.setClan(clan);
                battlePassClone.setPoints(rs.getDouble("points"));
                battlePassClone.setRewarded(rs.getInt("rewarded"));
                battlePassClone.setAvailability(rs.getInt("availability") == 1 ? true : false);
                _battlePasses.add(battlePassClone);
                System.out.println("**********Inserting clan battle pass for clan " + clan.getName());
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public void saveBattlePass(int id, L2Clan clan){

        BattlePass battlePassClone;
        battlePassClone = BattlePassTable.getBattlePasses().get(id).cloneClanBattlePass();
        battlePassClone.setClan(clan);
        battlePassClone.setPoints(0);
        battlePassClone.setAvailability(true);
        battlePassClone.setRewarded(0);
        _battlePasses.add(battlePassClone);

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO battle_pass_clan (clanId,battlePassId,availability,points, rewarded) VALUES (?,?,?,?)");
            statement.setInt(1, clan.getClanId());
            statement.setInt(2, id);
            statement.setInt(3, 1);
            statement.setInt(4, 0);
            statement.setInt(5, 0);
            statement.execute();
            statement.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateBattlePass(int id, L2Clan clan, double points, int rewarded){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE battle_pass_clan SET points=?,rewarded=? WHERE clanId=? and battlePassId=?");

            stm.setDouble(1, points);
            stm.setInt(2, rewarded);
            stm.setInt(3, clan.getClanId());
            stm.setLong(4, id);
            stm.execute();
            stm.close();
        }catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateBattlePassAvailability(boolean bool, L2Clan clan, int battlePassId){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement stm = con.prepareStatement("UPDATE battle_pass_clan SET availability=? WHERE clanId=? and battlePassId=?");
            stm.setInt(2, clan.getClanId());
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
