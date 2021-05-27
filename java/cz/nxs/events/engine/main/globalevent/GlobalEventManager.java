/**
 * 
 */
package cz.nxs.events.engine.main.globalevent;

import java.util.List;

import javolution.util.FastList;
import cz.nxs.events.engine.EventManager;
import cz.nxs.events.engine.EventWarnings;
import cz.nxs.events.engine.lang.LanguageEngine;
import cz.nxs.events.engine.main.globalevent.raidevent.RaidbossEvent;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.l2j.CallBack;

/**
 * @author hNoke
 *
 */
public class GlobalEventManager
{
	private GlobalEvent _activeGlobalEvent;
	private String selectedEvent;
	
	private long timeStarted;
	private int lastEventIndex;
	
	private GlobalEvent _raidboss;
	
	public static List<GlobalEvent> _events = new FastList<GlobalEvent>();
	
	public GlobalEventManager()
	{
		load();
	}
	
	private void load()
	{
		_activeGlobalEvent = null;
		lastEventIndex = -1;
		
		_raidboss = new RaidbossEvent();
		
		_events.add(_raidboss);
	}
	
	public void selectEvent()
	{
		if(isActive())
			return;
		
		lastEventIndex ++;
		if(lastEventIndex >= _events.size() || _events.get(lastEventIndex) == null)
		{
			lastEventIndex = 0;
			selectedEvent = _events.get(0).getName();
		}
		else
		{
			selectedEvent = _events.get(lastEventIndex).getName();
		}
	}
	
	public void start(String event)
	{
		if(event == null)
			event = selectedEvent;
		
		if(event == null)
		{
			selectEvent();
			event = selectedEvent;
		}
		
		if(event.equals("RaidBoss"))
		{
			_activeGlobalEvent = _raidboss;
			_activeGlobalEvent.start();
		}
		
		timeStarted = System.currentTimeMillis();
	}
	
	public void stopByGm()
	{
		if(_activeGlobalEvent != null)
		{
			_activeGlobalEvent.end();
			_activeGlobalEvent = null;
			
			timeStarted = 0;
		}
	}
	
	public void stop()
	{
		if(_activeGlobalEvent != null)
		{
			_activeGlobalEvent = null;
			
			timeStarted = 0;
		}
	}
	
	public void registerPlayer(PlayerEventInfo player)
	{
		if(_activeGlobalEvent != null)
		{
			if (player.isRegistered())
			{
				player.sendMessage(LanguageEngine.getMsg("registering_alreadyRegistered"));
				return;
			}
			
			int i = EventWarnings.getInstance().getPoints(player);
			if(i >= EventWarnings.MAX_WARNINGS && !player.isGM())
			{
				player.sendMessage(LanguageEngine.getMsg("registering_warningPoints", EventWarnings.MAX_WARNINGS, i));
				return;
			}
			
			if (EventManager.getInstance().canRegister(player) && _activeGlobalEvent.canRegister(player))
			{
				PlayerEventInfo pi = CallBack.getInstance().getPlayerBase().addInfo(player);
				pi.setIsRegisteredToPvpZone(true);
				
				player.sendMessage("You have been registered to the pvp zone.");
				
				_activeGlobalEvent.addPlayer(player);
			}
			else
			{
				player.sendMessage("You cannot register in this state.");
			}
		}
	}
	
	public boolean isActive()
	{
		return _activeGlobalEvent != null;
	}
	
	public GlobalEvent getActiveEvent()
	{
		return _activeGlobalEvent;
	}
	
	public String timeActive()
	{
		if(timeStarted == 0)
			return "N/A";
		
		long currTime = System.currentTimeMillis();
		return Math.max(0, (currTime - timeStarted) / 60000) + " min";
	}
	
	public String getEventName()
	{
		if(isActive())
		{
			return _activeGlobalEvent.getName();
		}
		else
			return selectedEvent == null ? "N/A" : selectedEvent;
	}
	
	public String getState()
	{
		if(_activeGlobalEvent != null) return _activeGlobalEvent.getStateNameForHtml();
		return "N/A";
	}
}
