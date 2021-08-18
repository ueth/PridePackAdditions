package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.fairgames.configurations.ConfigManager;
import net.sf.l2j.gameserver.fairgames.enums.GameStage;
import net.sf.l2j.gameserver.fairgames.stadium.Stadium;
import net.sf.l2j.gameserver.fairgames.stadium.StadiumManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;

public class Game {
    private PlayerHandler _player1;
    private PlayerHandler _player2;
    private int _instanceId;

    private int _winningPlayer = 0;
    private String _winHow = "Draw";

    private Stadium _stadium = StadiumManager.getInstance().getRandomStadium();

    protected ScheduledFuture<?> _gameTask = null;
    public static final int GAME_TIME = ConfigManager.getInstance().getGameTime(); // seconds
    private GameStage _gameStage;
    private int _clock = GAME_TIME;
    private static final int BUILD_TIME = ConfigManager.getInstance().getBuildTime();

    protected ScheduledFuture<?> _teleportTask = null;
    public static final int TELEPORT_TIME = ConfigManager.getInstance().getTeleportTime(); // seconds
    private int _teleportClock = TELEPORT_TIME;

    private boolean _abort = false;
    private boolean _win = false;

    public L2Spawn _spawnOne;
    public L2Spawn _spawnTwo;

    private int _playerPoints1;
    private int _playerPoints2;

    public Game(int instanceId){_instanceId = instanceId;}

    public Game(L2PcInstance player1, L2PcInstance player2, int instanceId){
        _player1 = new PlayerHandler(player1, instanceId);
        _player2 = new PlayerHandler(player2, instanceId);
        _playerPoints1 = player1.getPlayerStats().getPoints();
        _playerPoints2 = player2.getPlayerStats().getPoints();
        _instanceId = instanceId;
    }

    public int getInstanceId(){return _instanceId;}

    /**
     * Setting the instanceid and the location to tp players
     * @param player
     */
    public void setPlayer1(L2PcInstance player){
        _playerPoints1 = player.getPlayerStats().getPoints();
        _player1 = new PlayerHandler(player, _instanceId);
        _player1.setLoc(_stadium.getLocPlayer1().getX(), _stadium.getLocPlayer1().getY(), _stadium.getLocPlayer1().getZ());
    }
    public void setPlayer2(L2PcInstance player){
        _playerPoints2 = player.getPlayerStats().getPoints();
        _player2 = new PlayerHandler(player, _instanceId);
        _player2.setLoc(_stadium.getLocPlayer2().getX(), _stadium.getLocPlayer2().getY(), _stadium.getLocPlayer2().getZ());
    }


    public void notifyWin(int objectId){
        if(_player2.getPlayer().isOnline() == 1 && _player2.getPlayer().getObjectId() == objectId) {
            _winningPlayer = 1;
            sendMessageToPlayers(_player1.getPlayer().getName() + " won the match!");
        }
        else if(_player1.getPlayer().isOnline() == 1 && _player1.getPlayer().getObjectId() == objectId) {
            _winningPlayer = 2;
            sendMessageToPlayers(_player2.getPlayer().getName() + " won the match!");
        }
        _winHow = "Killed opponent";
        _win = true;
    }

    private void notifyWinDamage(){
        if(_player1.getDamage() > _player2.getDamage()) {
            _winningPlayer = 1;
            sendMessageToPlayers(_player1.getPlayer().getName() + " won the match!");
            _winHow = "Most damage";
        }
        else if(_player1.getDamage() < _player2.getDamage()) {
            _winningPlayer = 2;
            sendMessageToPlayers(_player2.getPlayer().getName() + " won the match!");
            _winHow = "Most damage";
        }
        else
            sendMessageToPlayers("Match ended in a draw");
    }

    private boolean notifyWinDisconnect(){
        if(_player1.getPlayer().isOnline() != 1) {
            _winningPlayer = 2;
            sendMessageToPlayers(_player2.getPlayer().getName() + " won the match!");
            _winHow = "Opponent disconnected";
            return true;
        }
        else if(_player2.getPlayer().isOnline() != 1) {
            _winningPlayer = 1;
            sendMessageToPlayers(_player1.getPlayer().getName() + " won the match!");
            _winHow = "Opponent disconnected";
            return true;
        }
        return false;
    }

    public void increaseDamage(int objectId, int damage){
        if(_player1.getPlayer().getObjectId() == objectId)
            _player1.increaseDamage(damage);
        else
            _player2.increaseDamage(damage);
    }

    public synchronized void start(){
        _gameStage = GameStage.TELEPORTING;
        _teleportTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                new TeleportTask(), 100,  1000
        );
    }

    public boolean teleportPlayersIntoArena(){
        _abort = !(_player1.getPlayer().isOnline() == 1 && _player2.getPlayer().isOnline() == 1);

        if(_abort)
            return false;

        _player1.teleportPlayerToArena();
        _player2.teleportPlayerToArena();

        _spawnOne = SpawnCoach(_stadium.getLocManager1().getX(), _stadium.getLocManager1().getY(), _stadium.getLocManager1().getZ(), 1);
        _spawnTwo = SpawnCoach(_stadium.getLocManager2().getX(), _stadium.getLocManager2().getY(), _stadium.getLocManager2().getZ(), 1);

        return true;
    }

    protected void unSpawnCoaches(){
        if(_spawnOne.getLastSpawn() != null)
            _spawnOne.getLastSpawn().deleteMe();
        if(_spawnTwo.getLastSpawn() != null)
            _spawnTwo.getLastSpawn().deleteMe();
    }

    public void teleportPlayersBack(){
        _player1.teleportPlayerBack();
        _player2.teleportPlayerBack();
    }

    public void fight(){
        _player1.fight();
        _player2.fight();
        unSpawnCoaches();
    }

    private void sendPlayersClock(){
        _player1.sendPlayerClock(_clock);
        _player2.sendPlayerClock(_clock);
    }

    private void sendMessageToPlayers(String message){
        if(_player1.getPlayer().isOnline()==1)
            _player1.getPlayer().sendMessage(message);
        if(_player2.getPlayer().isOnline()==1)
            _player2.getPlayer().sendMessage(message);
    }

    private void removePlayersFromInstance(){
        if(_player1.getPlayer().isOnline()==1)
            InstanceManager.getInstance().getInstance(_instanceId).removePlayer(_player1.getPlayer().getObjectId());
        if(_player2.getPlayer().isOnline()==1)
            InstanceManager.getInstance().getInstance(_instanceId).removePlayer(_player2.getPlayer().getObjectId());
    }

    private void saveStats(){
        String fontPlayer1 = "ffffff";
        String fontPlayer2 = "ffffff";
        switch (_winningPlayer){
            case 0 :
                _player1.getPlayer().getPlayerStats().saveStatsAfterGame(false, _player1.getClassName());
                _player2.getPlayer().getPlayerStats().saveStatsAfterGame(false, _player2.getClassName());
                break;
            case 1 :
                _player1.getPlayer().getPlayerStats().saveStatsAfterGame(true, _player1.getClassName());
                _player2.getPlayer().getPlayerStats().saveStatsAfterGame(false, _player2.getClassName());
                fontPlayer1 = "339966";
                fontPlayer2 = "ff5050";
                break;
            case 2 :
                _player1.getPlayer().getPlayerStats().saveStatsAfterGame(false, _player1.getClassName());
                _player2.getPlayer().getPlayerStats().saveStatsAfterGame(true, _player2.getClassName());
                fontPlayer2 = "339966";
                fontPlayer1 = "ff5050";
                break;
        }
        NpcHtmlMessage htmlMessage = new NpcHtmlMessage(1);
        htmlMessage.setFile("data/html/fairGames/gameResults.html");
        htmlMessage.replace("%fontPlayer1%", fontPlayer1);
        htmlMessage.replace("%fontPlayer2%", fontPlayer2);
        htmlMessage.replace("%player1%", _player1.getPlayer().getName());
        htmlMessage.replace("%player2%", _player2.getPlayer().getName());
        htmlMessage.replace("%player1points%", _player1.getPlayer().getPlayerStats().getPoints() + "" + (_playerPoints1>=_player1.getPlayer().getPlayerStats().getPoints() ?
                "(-"+(_playerPoints1-_player1.getPlayer().getPlayerStats().getPoints())+")" : "(+"+(_player1.getPlayer().getPlayerStats().getPoints()-_playerPoints1)+")"));
        htmlMessage.replace("%player2points%", _player2.getPlayer().getPlayerStats().getPoints() + "" + (_playerPoints2>=_player2.getPlayer().getPlayerStats().getPoints() ?
                "(-"+(_playerPoints2-_player2.getPlayer().getPlayerStats().getPoints())+")" : "(+"+(_player2.getPlayer().getPlayerStats().getPoints()-_playerPoints2)+")"));
        htmlMessage.replace("%player1dmg%", _player1.getDamage()+"");
        htmlMessage.replace("%player2dmg%", _player2.getDamage()+"");

        _player1.getPlayer().sendPacket(htmlMessage);
        _player2.getPlayer().sendPacket(htmlMessage);

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
            if(_win || notifyWinDisconnect()){
                _teleportTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                        new TeleportBackTask(), 1,  1000
                );
            }
            //Could use switch but wouldn't be able to use configs
            if(_clock == GAME_TIME){
                _gameStage = GameStage.CHOOSING_BUILD;
                handleGame();
            }else if(_clock == GAME_TIME-BUILD_TIME){
                _gameStage = GameStage.STARTED;
                handleGame();
            }else if(_clock == 0){
                _gameStage = GameStage.ENDED;
                handleGame();
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
                notifyWinDamage();
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
                    sendMessageToPlayers("You will be teleported in Fair Games in 20 seconds");
                    break;

                case 10:
                case 5:
                case 4:
                case 3:
                case 2:
                case 1:
                    sendMessageToPlayers(_teleportClock + " seconds left");
                    break;

                case 0:
                    _teleportTask.cancel(false);
                    _teleportTask = null;
                    Manager.getInstance().removePlayerFromWaitingPlayers(_player1.getPlayer());
                    Manager.getInstance().removePlayerFromWaitingPlayers(_player2.getPlayer());
                    if(teleportPlayersIntoArena()) {
                        _player1.getPlayer().sendMessage("Opponent's class: "+_player2.getClassName());
                        _player2.getPlayer().sendMessage("Opponent's class: "+_player1.getClassName());
                        _gameTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new GameTask(), 100, 1000);
                    }
                    else{
                        sendMessageToPlayers("Match is aborted, your opponent disconnected");
                        Manager.getInstance().removeGame(_instanceId);
                        InstanceManager.getInstance().destroyInstance(_instanceId);
                        return;
                    }
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
            unSpawnCoaches();
            saveStats();
            saveMatch();
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
                    sendMessageToPlayers(_teleportClock + " seconds left");
                    break;

                case 0:
                    teleportPlayersBack();
                    _teleportTask.cancel(false);
                    _teleportTask = null;
                    Manager.getInstance().removeGame(_instanceId);
                    InstanceManager.getInstance().destroyInstance(_instanceId);
                    removePlayersFromInstance();
                    break;
            }
            _teleportClock--;
        }
    }

    private void saveMatch(){
        try (Connection con = L2DatabaseFactory.getInstance().getConnection()) {
            PreparedStatement statement = con.prepareStatement(
                    "INSERT INTO fg_match_history (playerName1,playerName2,playerId1,playerId2,class1,class2,dmgDone1,dmgDone2,gameTime,winner,winHow,realTime) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

            String playerName1 = _player1.getPlayer().getName();
            String playerName2 = _player2.getPlayer().getName();
            int playerId1 = _player1.getPlayer().getObjectId();
            int playerId2 = _player2.getPlayer().getObjectId();
            String class1 = _player1.getClassName();
            String class2 = _player2.getClassName();
            int dmgDone1 = _player1.getDamage();
            int dmgDone2 = _player2.getDamage();
            String gameTime = getGameTimeInTimeFormat(GAME_TIME-_clock);
            String winner = _winningPlayer == 0 ? "Draw" : _winningPlayer == 1 ? playerName1 : playerName2;

            String match = playerName1 + "(" + class1 + ")" + " Dmg - " + dmgDone1 + " vs " + playerName2 + "(" + class2 + ")" + " Dmg - " + dmgDone2 + " / Winner - " + winner + " / Won by: " + _winHow + " / Time - " + gameTime;

            if(_player1.getPlayer().isOnline() == 1)
                _player1.getPlayer().getPlayerStats().addMatch(match);
            if(_player2.getPlayer().isOnline() == 1)
                _player2.getPlayer().getPlayerStats().addMatch(match);

            statement.setString(1, playerName1);
            statement.setString(2, playerName2);
            statement.setInt(3, playerId1);
            statement.setInt(4, playerId2);
            statement.setString(5, class1);
            statement.setString(6, class2);
            statement.setInt(7, dmgDone1);
            statement.setInt(8, dmgDone2);
            statement.setString(9, gameTime);
            statement.setString(10, winner);
            statement.setString(11, _winHow);
            statement.setLong(12, System.currentTimeMillis());

            statement.execute();
            statement.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getGameTimeInTimeFormat(int gameTime){
        int minutes = gameTime/60;
        int seconds = gameTime%60;

        String minutesString;
        String secondsString;

        if(minutes<10) minutesString = "0"+minutes;
        else minutesString = minutes+"";

        if(seconds<10) secondsString = "0"+seconds;
        else secondsString = seconds+"";

        return  minutesString+":"+secondsString;
    }
}
