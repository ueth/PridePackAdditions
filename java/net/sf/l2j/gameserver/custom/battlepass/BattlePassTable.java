package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.L2DatabaseFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * This class loads all the battle passes from the sql and stores
 * them in a list
 * If more than 1 id is found more battle passes will be created
 */
public class BattlePassTable {

    private static Map<Integer, BattlePass> _battlePasses = new HashMap<>();
    private static Map<Integer, BattlePass> _clanBattlePasses = new HashMap<>();

    public static Map<Integer, BattlePass> getBattlePasses() {
        return _battlePasses;
    }

    public static Map<Integer, BattlePass> getClanBattlePasses(){
        return _clanBattlePasses;
    }

    /**
     * Can also be used for reload
     */
    public static void initBattlePasses(){
        _clanBattlePasses.clear();
        _battlePasses.clear();
        restoreAvailableBattlePasses();
        fillBattlePassesWithRewards();
        restoreAvailableClanBattlePasses();
        fillClanBattlePassesWithRewards();
    }

    public static void restoreAvailableBattlePasses() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement =
                    con.prepareStatement("SELECT * FROM battle_pass");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int price = rs.getInt("price");
                int itemId = rs.getInt("itemId");
                int minLvl = rs.getInt("minLvl");
                _battlePasses.put(id, new BattlePass(null, 0, name, false, id, price, itemId, minLvl));
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void fillBattlePassesWithRewards(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            for(int i : _battlePasses.keySet()){
                PreparedStatement statement = con.prepareStatement("SELECT id,itemId,amount FROM battle_pass_rewards WHERE battlePassId=?");
                statement.setInt(1, i);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    _battlePasses.get(i).addReward(rs.getInt("id"), new Reward(rs.getInt("itemId"), rs.getInt("amount")));
                }
                rs.close();
                statement.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }


    public static void restoreAvailableClanBattlePasses() {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement =
                    con.prepareStatement("SELECT * FROM battle_pass_cl");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int price = rs.getInt("price");
                int itemId = rs.getInt("itemId");
                int minLvl = rs.getInt("minLvl");
                _clanBattlePasses.put(id, new BattlePass(null, 0, name, false, id, price, itemId, minLvl));
            }
            rs.close();
            statement.close();

        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void fillClanBattlePassesWithRewards(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            for(int i : _clanBattlePasses.keySet()){
                PreparedStatement statement = con.prepareStatement("SELECT id,itemId,amount FROM battle_pass_rewards_cl WHERE battlePassId=?");
                statement.setInt(1, i);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    _clanBattlePasses.get(i).addReward(rs.getInt("id"), new Reward(rs.getInt("itemId"), rs.getInt("amount")));
                }
                rs.close();
                statement.close();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
