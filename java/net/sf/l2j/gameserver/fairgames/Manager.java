package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.fairgames.configurations.ConfigManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.Broadcast;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

public class Manager {
    private Map<Integer,L2PcInstance> _registeredPlayers = new HashMap<>();
    private Map<Integer,L2PcInstance> _waitingToStartPlayers = new HashMap<>();

    private final static int MINIMUM_PLAYERS = ConfigManager.getInstance().getMinRegisteredPlayers();

    private static Manager _instance = null;

    private static int _instanceID = 2000;

    public L2Spawn _spawn;

    private Map<Integer, Game> _games = new HashMap<>();

    private boolean _registrationPhase = false;
    private ScheduledFuture<?> _registrationTask = null;
    private static final int REGISTRATION_TIME = ConfigManager.getInstance().getRegistrationTime(); // seconds
    private int _clock = REGISTRATION_TIME;

    public synchronized void run(){
        _registrationTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                new RegistrationTask(), 35000,  5000
        );
    }

    public void abortRegistrationPhase(){
        Broadcast.announceToOnlinePlayers("Fair Games have been aborted!");
        _registrationTask.cancel(true);
        _registrationTask = null;
        _spawn.getLastSpawn().deleteMe();
    }

    public void register(L2PcInstance player){
        if(player.getFGClass() == null)
            return;
        if(!ConditionChecker.validate(player))
            return;
        if(!_registrationPhase) {
            player.sendMessage("Fair Games are currently off");
            return;
        }
        if(!_registeredPlayers.containsKey(player.getObjectId()) && !_waitingToStartPlayers.containsKey(player.getObjectId()))
            _registeredPlayers.put(player.getObjectId(), player);

        checkIfGamesShouldStart(); //When a player registers we check if players are enough for the games to start
    }

    public void unRegister(L2PcInstance player){
        //if the match is about to begin and player is in waitingToStartPlayers map then he should not be able to unregister
        if(_registeredPlayers.containsKey(player.getObjectId()) && !player.isInFairGame() && !_waitingToStartPlayers.containsKey(player.getObjectId()))
            _registeredPlayers.remove(player.getObjectId(), player);
    }

    public class RegistrationTask implements Runnable {
        public void run() {

            if(_clock == REGISTRATION_TIME){
                _registrationPhase = true;
                _spawn = spawnManager(ConfigManager.getInstance().getRegNPCSpawnLocation().getX(),
                        ConfigManager.getInstance().getRegNPCSpawnLocation().getY(),
                        ConfigManager.getInstance().getRegNPCSpawnLocation().getZ(), 31783);

                Broadcast.announceToOnlinePlayers("Fair Games are now active!");
            }else if(_clock == 0){
                Broadcast.announceToOnlinePlayers("Fair Games are now over");
                _clock = REGISTRATION_TIME;
                _registrationPhase = false;
                _registrationTask.cancel(true);
                _registrationTask = null;
                _spawn.getLastSpawn().deleteMe();
            }
            _clock-=5;
        }
    }

    private synchronized void checkIfGamesShouldStart(){
        if(_registeredPlayers.size() < MINIMUM_PLAYERS)
            return;

        List<L2PcInstance> players = new ArrayList<>(_registeredPlayers.values());

        int counter = 0;
        int counterForSize = 0;
        int size = players.size();
        Game game = null;

        for(L2PcInstance player : players){
            if(size%2 != 0 && counterForSize == 0) {
                counterForSize++;
                continue;
            }
            if(counter%2 == 0) {
                game = new Game(createInstanceID());

                InstanceManager.getInstance().createInstance(game.getInstanceId());

                game.setPlayer1(player);
                _games.put(game.getInstanceId(), game);
            }else{
                game.setPlayer2(player);
            }
            _registeredPlayers.remove(player.getObjectId(), player);
            _waitingToStartPlayers.put(player.getObjectId(), player);
            counter++;
        }
        for(Game game1 : _games.values()){
            game1.start();
        }
    }

    public void removeGame(int id){
        for(Game game : _games.values()){
            if(game.getInstanceId() == id){
                _games.remove(game.getInstanceId(), game);
                break;
            }
        }
    }

    public void notifyGameDamage(int id, int objectID, int damage){
        if(!_games.isEmpty() && _games.containsKey(id))
            _games.get(id).increaseDamage(objectID, damage);
    }

    public void notifyWin(int id, int objectId){
        if(!_games.isEmpty() && _games.containsKey(id))
            _games.get(id).notifyWin(objectId);
    }

    public void removePlayerFromWaitingPlayers(L2PcInstance player){
        if(_waitingToStartPlayers.containsKey(player.getObjectId()))
            _waitingToStartPlayers.remove(player.getObjectId());
    }

    public int createInstanceID(){
        _instanceID = 2000;
        if(_games.isEmpty())
            return _instanceID;
        else{
            while (_games.keySet().contains(_instanceID))
                _instanceID++;
        }

        return _instanceID;
    }

    public boolean isPlayerRegistered(L2PcInstance player){
        return _registeredPlayers.containsKey(player.getObjectId()) || _waitingToStartPlayers.containsKey(player.getObjectId());
    }
    public Game getGame(int id){
        return _games.get(id);
    }

    public L2Spawn spawnManager(int xPos, int yPos, int zPos, int npcId) {
        final L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
        try {
            final L2Spawn spawn = new L2Spawn(template);
            spawn.setLocx(xPos);
            spawn.setLocy(yPos);
            spawn.setLocz(zPos);
            spawn.setAmount(1);
            spawn.setHeading(0);
            spawn.setRespawnDelay(1);
            SpawnTable.getInstance().addNewSpawn(spawn, false);
            spawn.init();
            return spawn;
        }
        catch (Exception e) { return null; }
    }

    public static Manager getInstance(){
        if (_instance == null)
            synchronized (Manager.class) {
                if (_instance == null)
                    _instance = new Manager();
            }

        return _instance;
    }
}
