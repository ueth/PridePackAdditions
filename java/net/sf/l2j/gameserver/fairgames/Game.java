package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import java.util.concurrent.ScheduledFuture;

public class Game {
    private PlayerHandler _player1;
    private PlayerHandler _player2;
    private int _instanceId;

    private boolean _win = false;

    protected ScheduledFuture<?> _gameTask = null;
    public static final int GAME_TIME = 120; // seconds
    private GameStage _gameStage;
    private int _clock = GAME_TIME;

    protected ScheduledFuture<?> _teleportTask = null;
    public static final int TELEPORT_TIME = 20; // seconds
    private int _teleportClock = TELEPORT_TIME;

    private boolean _abort;

    public L2Spawn _spawnOne;
    public L2Spawn _spawnTwo;

    public Game(int instanceId){_instanceId = instanceId;}

    public Game(L2PcInstance player1, L2PcInstance player2, int instanceId){
        _player1 = new PlayerHandler(player1, instanceId);
        _player2 = new PlayerHandler(player2, instanceId);
        _instanceId = instanceId;
    }

    public int getInstanceId(){return _instanceId;}

    /**
     * Setting the instanceid and the location to tp players
     * @param player
     */
    public void setPlayer1(L2PcInstance player){
        _player1 = new PlayerHandler(player, _instanceId);
        _player1.setLoc(84455, -17077, -1847);
    }
    public void setPlayer2(L2PcInstance player){
        _player2 = new PlayerHandler(player, _instanceId);
        _player2.setLoc(81931, -15233, -1841);
    }

    public void increaseDamage(int objectId, int damage){
        if(_player1.getPlayer().getObjectId() == objectId)
            _player1.increaseDamage(damage);
        else
            _player2.increaseDamage(objectId);
    }

    public synchronized void start(){
        sendMessageToPlayers("You will be teleported in 20 seconds");
        _gameStage = GameStage.TELEPORTING;
        _teleportTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                new TeleportTask(), 100,  1000
        );

    }

    public boolean teleportPlayersIntoArena(){
        System.out.println("teleportPlayersToArena");
        _player1.teleportPlayerToArena();
        _player2.teleportPlayerToArena();

//        if(_abort) {
//            return false;
//        }

        _spawnOne = SpawnCoach(0,0, 0, 123);
        _spawnTwo = SpawnCoach(0,0, 0, 123);

        return true;
    }

    public void teleportPlayersBack(){
        _player1.teleportPlayerBack();
        _player2.teleportPlayerBack();
    }

    public void fight(){
        _player1.fight();
        _player2.fight();
    }

    private void sendPlayersClock(){
        _player1.sendPlayerClock(_clock);
        _player2.sendPlayerClock(_clock);
    }

    private void sendMessageToPlayers(String message){
        _player1.getPlayer().sendMessage(message);
        _player2.getPlayer().sendMessage(message);
    }

    public void notifyForWin(){
        _win = true;
    }

    public L2Spawn SpawnCoach(int xPos, int yPos, int zPos, int npcId) {
        final L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
        try {
            final L2Spawn spawn = new L2Spawn(template);
            spawn.setLocx(xPos);
            spawn.setLocy(yPos);
            spawn.setLocz(zPos);
            spawn.setAmount(1);
            spawn.setHeading(0);
            spawn.setRespawnDelay(1);
            spawn.setInstanceId(_instanceId);
            SpawnTable.getInstance().addNewSpawn(spawn, false);
            spawn.init();
            return spawn;
        }
        catch (Exception e) { return null; }
    }

    public class GameTask implements Runnable {
        public void run() {
            if(_win || _player1.getPlayer().isOnline() != 1 || _player2.getPlayer().isOnline() != 1){
                _teleportTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                        new TeleportBackTask(), 1,  1000
                );
            }
            switch (_clock){
                case GAME_TIME :
                    _gameStage = GameStage.CHOOSING_BUILD;
                    handleGame();
                    break;

                case GAME_TIME-60 :
                    _gameStage = GameStage.STARTED;
                    handleGame();
                    break;

                case 0:
                    _gameStage = GameStage.ENDED;
                    handleGame();
                    break;
            }
            sendPlayersClock();
            _clock--;
        }
    }

    private synchronized void handleGame(){
        switch (_gameStage){
            case STARTED:
                fight();
                break;

            case ENDED:
                _teleportTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                        new TeleportBackTask(), 1,  1000
                );
                Manager.getInstance().removeGame(_instanceId);
                break;
        }
    }

    public class TeleportTask implements Runnable {
        public void run() {
            switch (_teleportClock){
                case 20 :
                    sendMessageToPlayers("You will be teleported in 20 seconds");
                    break;

                case 10:
                case 5:
                case 4:
                case 3:
                case 2:
                case 1:
                    sendMessageToPlayers(_teleportClock + " seconds left until teleport");
                    break;

                case 0:
                    teleportPlayersIntoArena();
                    _teleportTask.cancel(false);
                    _teleportTask = null;
                    _gameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new GameTask(), 100,  1000);
                    break;
            }
            _teleportClock--;
        }
    }

    public class TeleportBackTask implements Runnable {
        TeleportBackTask(){
            _teleportClock = TELEPORT_TIME;
            _gameTask.cancel(false);
            _gameTask = null;
        }

        public void run() {
            switch (_teleportClock){
                case 20 :
                    sendMessageToPlayers("You will be teleported back in 20 seconds");
                    break;

                case 10:
                case 5:
                case 4:
                case 3:
                case 2:
                case 1:
                    sendMessageToPlayers(_teleportClock + " seconds left until teleport back");
                    break;

                case 0:
                    teleportPlayersBack();
                    _teleportTask.cancel(false);
                    _teleportTask = null;
                    Manager.getInstance().removeGame(_instanceId);
                    InstanceManager.getInstance().destroyInstance(_instanceId);
                    break;
            }
            _teleportClock--;
        }
    }
}
