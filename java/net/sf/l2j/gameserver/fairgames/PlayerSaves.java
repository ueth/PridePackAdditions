package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.model.L2Skill;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PlayerSaves {
    private Map<Integer, List<Integer>> _itemsToDelete= new HashMap<>();
    private Map<Integer, List<Integer>> _previousWear= new HashMap<>();
    private Map<Integer, List<L2Skill>> _previousSkills = new HashMap<>();

    private static PlayerSaves _instance = null;

    public void addItemsToDelete(int objectID, List<Integer> itemList){
        _itemsToDelete.put(objectID, itemList);
    }
    public void removeItemsToDelete(int objectID){
        if(_itemsToDelete.containsKey(objectID))
            _itemsToDelete.remove(objectID);
    }
    public List<Integer> getItemsToDelete(int objectID){
        if (_itemsToDelete.containsKey(objectID))
            return _itemsToDelete.get(objectID);
        else
            return null;
    }

    public void addPreviousWear(int objectID, List<Integer> itemList){
        _previousWear.put(objectID, itemList);
    }
    public void removePreviousWear(int objectID){
        if(_previousWear.containsKey(objectID))
            _previousWear.remove(objectID);
    }
    public List<Integer> getPreviousWear(int objectID){
        if (_previousWear.containsKey(objectID))
            return _previousWear.get(objectID);
        else
            return null;
    }

    public void addPreviousSkills(int objectID, List<L2Skill> skillList){
        _previousSkills.put(objectID, skillList);
    }
    public void removePreviousSkills(int objectID){
        if(_previousSkills.containsKey(objectID))
            _previousSkills.remove(objectID);
    }
    public List<L2Skill> getPreviousSkills(int objectID){
        if (_previousSkills.containsKey(objectID))
            return _previousSkills.get(objectID);
        else
            return null;
    }

    public static PlayerSaves getInstance(){
        if (_instance == null)
            synchronized (PlayerSaves.class) {
                if (_instance == null)
                    _instance = new PlayerSaves();
            }

        return _instance;
    }
}
