/**
 * 
 */
package cz.nxs.events.engine.main.globalevent.raidevent.bosses;

import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.interf.delegate.NpcData;

/**
 * @author hNoke
 *
 */
public abstract class BossTemplate
{
	public BossTemplate()
	{
		startClock();
	}
	
	ScheduledFuture<?> _nextClock = null;
	
	public void startClock()
	{
		_nextClock = ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			@Override
			public void run()
			{
				clockTick();
			}
		}, 5000);
	}
	
	public void stopClock()
	{
		if(_nextClock != null)
		{
			_nextClock.cancel(false);
			_nextClock = null;
		}
	}
	
	public void monsterDied(L2Npc npc)
	{
		if(npc != null && npc.getNpcId() == getBossId())
			stopClock();
	}
	
	public void clockTick()
	{
		startClock();
	}
	
	public abstract int getBossId();
	
	public abstract EventSpawn getBossSpawn();
	public abstract EventSpawn getPlayersSpawn();
	
	public abstract void rewardPlayer(L2PcInstance player);
	
	
	public abstract NpcData doSpawn();
}
