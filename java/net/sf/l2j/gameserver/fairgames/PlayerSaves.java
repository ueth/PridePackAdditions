package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.custom.runes.Rune;
import net.sf.l2j.gameserver.custom.runes.RuneTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerSaves {
    private Map<Integer, List<L2Skill>> _previousEffects = new HashMap<>();
    private Map<Integer, List<Integer>> _itemsToDelete = new HashMap<>();
    private Map<Integer, List<Integer>> _previousWear = new HashMap<>();
    private Map<Integer, List<L2Skill>> _previousSkills = new HashMap<>();

    private static PlayerSaves _instance = null;

    //Effects savings
    public void addPreviousEffects(int objectID, List<L2Skill> skillList) {
        _previousEffects.put(objectID, skillList);
    }

    public void clearPreviousEffects(int objectID) {
        if (_previousEffects.containsKey(objectID))
            _previousEffects.remove(objectID);
    }

    public List<L2Skill> getPreviousEffects(int objectID) {
        if (_previousEffects.containsKey(objectID))
            return _previousEffects.get(objectID);
        else
            return null;
    }

    //Given items by the FairGames to remove
    public void addItemsToDelete(int objectID, List<Integer> itemList) {
        _itemsToDelete.put(objectID, itemList);
    }
    public void addItemToDelete(int objectID, int itemId){
        _itemsToDelete.get(objectID).add(itemId);
    }

    public void clearItemsToDelete(int objectID) {
        if (_itemsToDelete.containsKey(objectID))
            _itemsToDelete.remove(objectID);
    }

    public List<Integer> getItemsToDelete(int objectID) {
        if (_itemsToDelete.containsKey(objectID))
            return _itemsToDelete.get(objectID);
        else
            return null;
    }

    //Equipment savings
    public void addPreviousWear(int objectID, List<Integer> itemList) {
        _previousWear.put(objectID, itemList);
    }

    public void clearPreviousWear(int objectID) {
        if (_previousWear.containsKey(objectID))
            _previousWear.remove(objectID);
    }

    public List<Integer> getPreviousWear(int objectID) {
        if (_previousWear.containsKey(objectID))
            return _previousWear.get(objectID);
        else
            return null;
    }

    //Skills savings
    public void addPreviousSkills(int objectID, List<L2Skill> skillList) {
        _previousSkills.put(objectID, skillList);
    }

    public void clearPreviousSkills(int objectID) {
        if (_previousSkills.containsKey(objectID))
            _previousSkills.remove(objectID);
    }

    public List<L2Skill> getPreviousSkills(int objectID) {
        if (_previousSkills.containsKey(objectID))
            return _previousSkills.get(objectID);
        else
            return null;
    }

    /**
     * Executes every save for Fair Games
     *
     * @param activeChar
     */
    public void doItAll(L2PcInstance activeChar) {
        for (L2Effect effect : activeChar.getAllEffects())
            if (effect != null && effect.getSkill() != null) //Remove all added effects
                effect.exit();

        if (PlayerSaves.getInstance().getItemsToDelete(activeChar.getObjectId()) != null) {
            for (int objectId : PlayerSaves.getInstance().getItemsToDelete(activeChar.getObjectId())) {
                L2ItemInstance item = activeChar.getInventory().getItemByObjectId(objectId);
                if (item != null) {
                    activeChar.getInventory().destroyItem("destroy", item, activeChar, null);
                }
            }
        }
        PlayerSaves.getInstance().clearItemsToDelete(activeChar.getObjectId());

        if (PlayerSaves.getInstance().getPreviousWear(activeChar.getObjectId()) != null) {
            for (int objectId : PlayerSaves.getInstance().getPreviousWear(activeChar.getObjectId())) {
                L2ItemInstance item = activeChar.getInventory().getItemByObjectId(objectId);
                if (item != null)
                    activeChar.getInventory().equipItem(item);
            }

            PlayerSaves.getInstance().clearPreviousWear(activeChar.getObjectId());
        }

        if (PlayerSaves.getInstance().getPreviousSkills(activeChar.getObjectId()) != null) {
            for (L2Skill skill : PlayerSaves.getInstance().getPreviousSkills(activeChar.getObjectId())) {
                if (skill != null)
                    activeChar.addSkill(skill, false);
            }

            PlayerSaves.getInstance().clearPreviousSkills(activeChar.getObjectId());
        }

        if (PlayerSaves.getInstance().getPreviousEffects(activeChar.getObjectId()) != null)
            for (L2Skill skill : PlayerSaves.getInstance().getPreviousEffects(activeChar.getObjectId())) {
                if (skill != null)
                    skill.getEffects(activeChar, activeChar);
            }
        PlayerSaves.getInstance().clearPreviousEffects(activeChar.getObjectId());
    }

    public void saveEverythingInDB(int objectId) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {

            PreparedStatement statement = con.prepareStatement("INSERT INTO fg_previous_skills (playerId,skillId,skillLvl) VALUES (?,?,?)");
            statement.setInt(1, objectId);

            for (L2Skill skill : getPreviousSkills(objectId)) {
                statement.setInt(2, skill.getId());
                statement.setInt(3, skill.getLevel());
                statement.execute();
            }

            statement = con.prepareStatement("INSERT INTO fg_previous_effects (playerId,skillId,skillLvl) VALUES (?,?,?)");
            statement.setInt(1, objectId);

            for (L2Skill skill : getPreviousEffects(objectId)) {
                statement.setInt(2, skill.getId());
                statement.setInt(3, skill.getLevel());
                statement.execute();
            }

            statement = con.prepareStatement("INSERT INTO fg_previous_wear (playerId,objectId) VALUES (?,?)");
            statement.setInt(1, objectId);

            for (Integer obj : getPreviousWear(objectId)) {
                statement.setInt(2, obj);
                statement.execute();
            }

            statement = con.prepareStatement("INSERT INTO fg_items_to_delete (playerId,objectId) VALUES (?,?)");
            statement.setInt(1, objectId);

            for (Integer obj : getItemsToDelete(objectId)) {
                statement.setInt(2, obj);
                statement.execute();
            }

            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadEverythingFromDB(int objectId) {
        List<L2Skill> previousSkills = new ArrayList<>();
        List<L2Skill> previousEffects = new ArrayList<>();
        List<Integer> previousWear = new ArrayList<>();
        List<Integer> itemsToDelete = new ArrayList<>();

        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {

            PreparedStatement statement = con.prepareStatement("SELECT * FROM fg_previous_skills where playerId=?");
            statement.setInt(1, objectId);
            ResultSet rs = statement.executeQuery();
            while (rs.next())
                previousSkills.add(SkillTable.getInstance().getInfo(rs.getInt("skillId"), rs.getInt("skillLvl")));
            _previousSkills.put(objectId, previousSkills);


            statement = con.prepareStatement("SELECT * FROM fg_previous_effects where playerId=?");
            statement.setInt(1, objectId);
            rs = statement.executeQuery();
            while (rs.next())
                previousEffects.add(SkillTable.getInstance().getInfo(rs.getInt("skillId"), rs.getInt("skillLvl")));
            _previousEffects.put(objectId, previousEffects);


            statement = con.prepareStatement("SELECT * FROM fg_previous_wear where playerId=?");
            statement.setInt(1, objectId);
            rs = statement.executeQuery();
            while (rs.next())
                previousWear.add(rs.getInt("objectId"));
            _previousWear.put(objectId, previousWear);


            statement = con.prepareStatement("SELECT * FROM fg_items_to_delete where playerId=?");
            statement.setInt(1, objectId);
            rs = statement.executeQuery();
            while (rs.next())
                itemsToDelete.add(rs.getInt("objectId"));
            _itemsToDelete.put(objectId, itemsToDelete);


            rs.close();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        deleteEverythingFromDB(objectId);
    }

    public void deleteEverythingFromDB(int objectId) {
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM fg_previous_skills WHERE playerId=?");
            statement.setInt(1, objectId);
            statement.execute();

            statement = con.prepareStatement("DELETE FROM fg_previous_effects WHERE playerId=?");
            statement.setInt(1, objectId);
            statement.execute();

            statement = con.prepareStatement("DELETE FROM fg_previous_wear WHERE playerId=?");
            statement.setInt(1, objectId);
            statement.execute();

            statement = con.prepareStatement("DELETE FROM fg_items_to_delete WHERE playerId=?");
            statement.setInt(1, objectId);
            statement.execute();

            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PlayerSaves getInstance() {
        if (_instance == null)
            synchronized (PlayerSaves.class) {
                if (_instance == null)
                    _instance = new PlayerSaves();
            }

        return _instance;
    }
}
