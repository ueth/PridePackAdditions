package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import java.util.concurrent.ScheduledFuture;

public class Game {
    private PlayerHandler _player1;
    private PlayerHandler _player2;
    private int _instanceId;

    protected ScheduledFuture<?> _gameTask = null;
    private int _gameTime = 180; // seconds
    private GameStage _gameStage;

    private boolean _abort;

    public L2Spawn _spawnOne;
    public L2Spawn _spawnTwo;

    public Game(int instanceId){_instanceId = instanceId;}
    public Game(L2PcInstance player1, L2PcInstance player2, int instanceId){
        _player1 = new PlayerHandler(player1, instanceId);
        _player2 = new PlayerHandler(player2, instanceId);
        _instanceId = instanceId;
    }

    public void setPlayer1(L2PcInstance player){
        _player1 = new PlayerHandler(player, _instanceId);
    }
    public void setPlayer2(L2PcInstance player){
        _player2 = new PlayerHandler(player, _instanceId);
    }

    public synchronized void start(){
//        _player1.getPlayer().sendMessage("You will be teleported in 20 seconds");
//        _player2.getPlayer().sendMessage("You will be teleported in 20 seconds");
        _gameStage = GameStage.TELEPORTING;
        _gameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                new GameTask(), 1000,  1000
        );
    }

    public boolean teleportPlayersIntoArena(){
        _abort = !_player1.teleportPlayerToArena() ? false :  _player2.teleportPlayerToArena();

        if(_abort)
            return false;

        _spawnOne = SpawnCoach(0,0, 0, 123);
        _spawnTwo = SpawnCoach(0,0, 0, 123);

        return _abort;
    }

    public void teleportPlayersBack(){
        _player1.teleportPlayerBack();
        _player2.teleportPlayerBack();
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
            SpawnTable.getInstance().addNewSpawn(spawn, false);
            spawn.init();
            return spawn;
        }
        catch (Exception e) { return null; }
    }

    public class GameTask implements Runnable {
        public void run() {

        }
    }
}
