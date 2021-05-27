/**
 * 
 */
package cz.nxs.events.engine.main.globalevent.raidevent.bosses;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import cz.nxs.events.engine.base.EventSpawn;
import cz.nxs.events.engine.base.Loc;
import cz.nxs.interf.delegate.NpcData;
import cz.nxs.interf.delegate.NpcTemplateData;

/**
 * @author hNoke
 *
 */
public class DummyBoss extends BossTemplate
{
	public DummyBoss()
	{
		super();
	}
	
	@Override
	public NpcData doSpawn()
	{
		NpcData bossNpc;
		
		NpcTemplateData template = new NpcTemplateData(getBossId());
		bossNpc = template.doSpawn(getBossSpawn().getLoc().getX(), getBossSpawn().getLoc().getY(), getBossSpawn().getLoc().getZ(), 1, 0);
		return bossNpc;
	}

	@Override
	public int getBossId()
	{
		return 9996;
	}

	@Override
	public EventSpawn getBossSpawn()
	{
		return new EventSpawn(1, 1, new Loc(1, 1, 1), 1, "Boss");
	}

	@Override
	public EventSpawn getPlayersSpawn()
	{
		//can have more than one spawn
		return new EventSpawn(1, 1, new Loc(1, 1, 1), 1, "Regular");
	}
	
	@Override
	public void monsterDied(L2Npc npc)
	{
		super.monsterDied(npc);
		
		//TODO special actions after boss dies?
	}

	@Override
	public void rewardPlayer(L2PcInstance player)
	{
		player.addItem("RB Event", 57, 1, null, true);
	}

	@Override
	public void clockTick()
	{
		//TODO special actions for the boss
		
		super.clockTick();
	}
}
