/**
 * 
 */
package cz.nxs.events.engine.main.globalevent.raidevent;

import java.util.List;

import cz.nxs.events.engine.main.globalevent.raidevent.bosses.BossTemplate;
import cz.nxs.events.engine.main.globalevent.raidevent.bosses.DummyBoss;
import javolution.util.FastList;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;
import cz.nxs.events.engine.main.globalevent.GlobalEvent;
import cz.nxs.interf.PlayerEventInfo;
import cz.nxs.interf.delegate.NpcData;

/**
 * @author hNoke
 *
 */
public class RaidbossEvent extends GlobalEvent
{
	private final List<BossTemplate> _data = new FastList<BossTemplate>();
	
	public RaidbossEvent()
	{
		_data.add(new DummyBoss());
	}
	
	private BossTemplate _currentBossData;
	private NpcData _raidboss;
	
	@Override
	public String getName()
	{
		return "Raidboss";
	}
	
	private BossTemplate getRandomBoss()
	{
		return _data.get(Rnd.get(_data.size())); //TODO first index 0 ?
	}

	@Override
	public void start()
	{
		BossTemplate boss = getRandomBoss();
		_currentBossData = boss;
		
		_raidboss = boss.doSpawn();
		_raidboss.setGlobalEvent(this);
		
		announce("Global raidboss spawned.");
	}

	@Override
	public void end()
	{
		if(_raidboss == null)
			return;

		if(!_raidboss.isDead())
		{
			_raidboss.deleteMe();
		}
		else if(_raidboss.isDead())
		{
			for(L2PcInstance player : _raidboss.getOwner().getKnownList().getKnownPlayersInRadius(30000))
			{
				_currentBossData.rewardPlayer(player);
			}
		}
		
		announce("Global raidboss event ended.");
	}
	
	public void bossDied()
	{
		
	}

	@Override
	public boolean canRegister(PlayerEventInfo player)
	{
		return true;
	}
	
	@Override
	public void monsterDies(L2Npc npc)
	{
		if(_currentBossData != null)
		{
			_currentBossData.monsterDied(npc);
		}
	}

	@Override
	public void addPlayer(PlayerEventInfo player)
	{
		if(_raidboss != null && _currentBossData != null)
		{
			if(player.isDead())
			{
				player.doRevive();
			}
			
			player.screenMessage("You are being teleported close to the boss.", getName(), false);
			
			player.teleport(_currentBossData.getPlayersSpawn().getLoc(), 0, true, 0);
		}
	}

	@Override
	public String getStateNameForHtml()
	{
		return "Dangerous";
	}
}
