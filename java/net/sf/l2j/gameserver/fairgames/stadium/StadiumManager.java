package net.sf.l2j.gameserver.fairgames.stadium;

import java.util.HashMap;
import java.util.Map;

public class StadiumManager {
    private Map<Integer,Stadium> _stadiums = new HashMap<>();
    private static StadiumManager _instance = null;

    public Stadium getRandomStadium(){
        return null;
    }

    public static StadiumManager getInstance(){
        if(_instance == null)
            _instance = new StadiumManager();
        return _instance;
    }
}
