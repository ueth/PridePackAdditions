package net.sf.l2j.gameserver.fairgames;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Broadcast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class Manager {
    private Map<Integer,L2PcInstance> _registeredPlayers = new HashMap<>();

    private final static int MINIMUM_PLAYERS = 2;

    private static Manager _instance = null;

    private boolean _registrationPhase = false;
    private ScheduledFuture<?> _registrationTask = null;
    private static final int REGISTRATION_TIME = 30; // seconds
    private int _clock = REGISTRATION_TIME;

    public synchronized void run(){
        _registrationTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
                new RegistrationTask(), 35000,  1000
        );
    }

    public void abortRegistrationPhase(){
        Broadcast.announceToOnlinePlayers("Fair Games have been aborted!");
        _registrationTask.cancel(true);
        _registrationTask = null;
    }

    public void register(L2PcInstance player){
        if(!_registrationPhase) {
            player.sendMessage("Fair Games are currently off");
            return;
        }
        if(!_registeredPlayers.containsKey(player.getObjectId()))
            _registeredPlayers.put(player.getObjectId(), player);
    }

    public void unRegister(L2PcInstance player){
        if(_registeredPlayers.containsKey(player.getObjectId()) && !player.isInFairGame())
            _registeredPlayers.remove(player.getObjectId(), player);
    }

    public class RegistrationTask implements Runnable {
        public void run() {
            switch (_clock){
                case REGISTRATION_TIME:{
                    _registrationPhase = true;
                    Broadcast.announceToOnlinePlayers("Fair Games are now active!");
                    break;
                }
                case 0 : {
                    Broadcast.announceToOnlinePlayers("Fair Games are now over");
                    _clock = REGISTRATION_TIME;
                    _registrationPhase = false;
                    _registrationTask.cancel(true);
                    _registrationTask = null;
                }
            }
            _clock--;
        }
    }

    private void checkIfGamesShouldStart(){
        if(_registeredPlayers.size() >= MINIMUM_PLAYERS){
            for(L2PcInstance player : _registeredPlayers.values()){
                _registeredPlayers.remove(player.getObjectId(), player);
                for(L2PcInstance player2 : _registeredPlayers.values()){
                    _registeredPlayers.remove(player2.getObjectId(), player2);
                    new Game(player, player2, 1);
                    break;
                }
            }

            int counter=0;
            L2PcInstance player1;
            L2PcInstance player2;
            for(int i=0; i<_registeredPlayers.size(); i++){
                for(L2PcInstance player : _registeredPlayers.values()){

                }
            }
        }
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
