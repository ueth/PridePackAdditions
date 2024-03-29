package net.sf.l2j.gameserver.instances.Ultraverse;

import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.Instance;
import net.sf.l2j.gameserver.model.entity.Instance.SendPacketToPlayerProcedure;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Ultraverse extends Quest
{
//NPCs
private static int CAHIRA = 90010;
private static int EXIT_TELEPORTER = 90014;

private static String qn = "Ultraverse";
public static final int INSTANCEID = 2002;

//REQUIREMENTS
private static boolean debug = false;
private static int levelReq = 87;
private static int pvpReq = 50;
private static int fameReq = 50;
private static int pkReq = 0;


private class teleCoord {int instanceId; int x; int y; int z;}

private class freeRBWorld extends InstanceWorld
{
public freeRBWorld()
{
	InstanceManager.getInstance().super();
}
}

public Ultraverse(int questId, String name, String descr)
{
	super(questId, name, descr);
	
	addStartNpc(CAHIRA);
	addTalkId(CAHIRA);
	addTalkId(EXIT_TELEPORTER);
	
	for (L2Spawn boss : RaidBossSpawnManager.getInstance().getSpawns().values())
		addKillId(boss.getNpcid());
}

public static void main(String[] args)
{
	new Ultraverse(-1, qn, "instances");
}

private boolean checkConditions(L2PcInstance player, boolean single)
{
	if (debug || player.isGM())
		return true;
	else
	{
		final L2Party party = player.getParty();
		
		if (!single && (party == null || party.getMemberCount() < 4 || party.getMemberCount() > 8))
		{
			player.sendMessage("This is a 4-8 player party instance, so you must have a party of 4-8 people");
			return false;
		}
		if (!single && party.getPartyLeaderOID() != player.getObjectId())
		{
			player.sendPacket(new SystemMessage(2185));
			return false;
		}
		
		if (!single)
		{
			/*if (!checkIPs(party))
				return false;*/
			
			boolean canEnter = true;
			
			for (L2PcInstance ptm : party.getPartyMembers())
			{
				if (ptm == null) return false;
				
				if (ptm.getLevel() < levelReq)
				{
					ptm.sendMessage("You must be level "+levelReq+" to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < pvpReq)
				{
					ptm.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < pkReq)
				{
					ptm.sendMessage("You must have "+pkReq+" PKs to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpKills() < fameReq)
				{
					ptm.sendMessage("You must have "+fameReq+" fame to enter this instance");
					canEnter = false;
				}
				else if (ptm.getPvpFlag() != 0 || ptm.getKarma() > 0)
				{
					ptm.sendMessage("You can't enter the instance while in PVP mode or have karma");
					canEnter = false;
				}
				else if (ptm.isInFunEvent())
				{
					ptm.sendMessage("You can't enter the instance while in an event");
					canEnter = false;
				}
				else if (ptm.isInDuel() || ptm.isInOlympiadMode() || Olympiad.getInstance().isRegistered(ptm))
				{
					ptm.sendMessage("You can't enter the instance while in duel/oly");
					canEnter = false;
				}
				else if (System.currentTimeMillis() < InstanceManager.getInstance().getInstanceTime(ptm.getAccountName(), INSTANCEID))
				{
					ptm.sendMessage("You can only enter this instance once a week. Wait until next Sunday.");
					canEnter = false;
				}
				else if (!ptm.isInsideRadius(player, 500, true, false))
				{
					ptm.sendMessage("You're too far away from your party leader");
					player.sendMessage("One of your party members is too far away");
					canEnter = false;
				}
				else
				{
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					
					if (world != null)
					{
						ptm.sendMessage("You can't enter because you have entered into another instance that hasn't expired yet, try waiting 5 min");
						canEnter = false;
					}
				}
				
				if (!canEnter)
				{
					ptm.sendMessage("You're preventing your party from entering an instance");
					if (ptm != player)
						player.sendMessage(ptm.getName()+" is preventing you from entering the instance");
					return false;
				}
			}
		}
		else
		{
			if (player.getLevel() < levelReq)
			{
				player.sendMessage("You must be level "+levelReq+" to enter this instance");
				return false;
			}
			else if (player.getPvpKills() < pvpReq)
			{
				player.sendMessage("You must have "+pvpReq+" PvPs to enter this instance");
				return false;
			}
			else if (player.getPvpFlag() != 0 || player.getKarma() > 0)
			{
				player.sendMessage("You can't enter the instance while in PVP mode or have karma");
				return false;
			}
			else if (player.isInFunEvent())
			{
				player.sendMessage("You can't enter the instance while in an event");
				return false;
			}
			else if (player.isInDuel() || player.isInOlympiadMode() || Olympiad.getInstance().isRegistered(player))
			{
				player.sendMessage("You can't enter the instance while in duel/oly");
				return false;
			}
		}
		
		return true;
	}
}

private void teleportplayer(L2PcInstance player, teleCoord teleto)
{
	player.setInstanceId(teleto.instanceId);
	player.teleToLocation(teleto.x, teleto.y, teleto.z);
	L2Summon pet = player.getPet();
	if (pet != null)
	{
		pet.setInstanceId(teleto.instanceId);
		pet.teleToLocation(teleto.x, teleto.y, teleto.z);
	}
}

protected int enterInstance(L2PcInstance player, String template, teleCoord teleto)
{
	int instanceId = 0;
	
	//check for existing instances for this player
	InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
	//existing instance
	if (world != null)
	{
		if (world.templateId != INSTANCEID)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.ALREADY_ENTERED_ANOTHER_INSTANCE_CANT_ENTER));
			return 0;
		}
		
		if (!checkConditions(player, true))
			return 0;
		
		teleto.instanceId = world.instanceId;
		teleportplayer(player,teleto);
		return instanceId;
	}
	else  //New instance
	{
		if (!checkConditions(player, false))
			return 0;
		
		L2Party party = player.getParty();
		instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		world = new freeRBWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("Ultraverse: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		// teleport players
		teleto.instanceId = instanceId;
		
		if (party == null)
		{
			if (!player.isGM())
				return 0;
			
			// this can happen only if debug is true
			InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(WEEK));
			world.allowed.add(player.getObjectId());
			auditInstances(player, template, instanceId);
			teleportplayer(player,teleto);
		}
		else
		{
			for (L2PcInstance partyMember : party.getPartyMembers())
			{
				partyMember.sendMessage("You have entered the alternate dimension of raidbosses");
				InstanceManager.getInstance().setInstanceTime(partyMember.getAccountName(), INSTANCEID, getNextInstanceTime(WEEK));
				world.allowed.add(partyMember.getObjectId());
				auditInstances(partyMember, template, instanceId);
				teleportplayer(partyMember,teleto);
			}
		}
		
		for (L2Spawn bossSpawn : RaidBossSpawnManager.getInstance().getSpawns().values())
		{
			if (bossSpawn != null)
				addSpawn(bossSpawn.getNpcid(), bossSpawn.getLocx(), bossSpawn.getLocy(), bossSpawn.getLocz(), bossSpawn.getHeading(), false, 120*60*1000, false, instanceId);
		}
		
		return instanceId;
	}
	
}

protected void exitInstance(L2PcInstance player, teleCoord tele)
{
	player.setInstanceId(0);
	player.teleToLocation(tele.x, tele.y, tele.z);
	
	L2Summon pet = player.getPet();
	if (pet != null)
	{
		pet.setInstanceId(0);
		pet.teleToLocation(tele.x, tele.y, tele.z);
	}
}

@Override
public String onTalk(L2Npc npc, L2PcInstance player)
{
	final int npcId = npc.getNpcId();
	
	QuestState st = player.getQuestState(qn);
	
	if (st == null)
		st = newQuestState(player);
	
	if (npcId == CAHIRA)
	{
		teleCoord teleto = new teleCoord();
		teleto.x = player.getX();
		teleto.y = player.getY();
		teleto.z = player.getZ();
		enterInstance(player, "Ultraverse.xml", teleto);
	}
	else if (npcId == EXIT_TELEPORTER)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof freeRBWorld))
			return null;
		
		teleCoord teleto = new teleCoord();
		teleto.x = -82993;
		teleto.y = 150860;
		teleto.z = -3129;
		
		if (player.getParty() == null)
		{
			exitInstance(player, teleto);
			player.sendPacket(new ExShowScreenMessage("You have completed the Ultraverse instance", 6000));
		}
		else
		{
			for (L2PcInstance ptm : player.getParty().getPartyMembers())
			{
				exitInstance(ptm, teleto);
				player.sendPacket(new ExShowScreenMessage("You have completed the Ultraverse instance", 6000));
			}
		}
		
		st.exitQuest(true);
	}
	
	return null;
}

@Override
public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	if (npc.getInstanceId() < 1000)
		return null;
	
	final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(killer);
	
	if (world == null || !(world instanceof freeRBWorld))
	{
		QuestState st = killer.getQuestState(qn);
		
		if (st != null)
			st.exitQuest(true);
		
		return null;
	}
	
	final freeRBWorld rbWorld = (freeRBWorld)world;
	
	Instance instance = InstanceManager.getInstance().getPlayerInstance(killer.getObjectId());
	
	instance.removeNpcs();
	
	SendPacketToPlayerProcedure prc = instance.new SendPacketToPlayerProcedure(new ExShowScreenMessage("You have killed "+npc.getName()+". Now you're free to leave", 6000));
	
	instance.getPlayers().forEach(prc);
	
	spawnExitGK(rbWorld, killer);
	
	return null;
}

public void spawnExitGK(freeRBWorld world, L2PcInstance player)
{
	addSpawn(EXIT_TELEPORTER, player.getX(), player.getY(), player.getZ(), 0, true, 0, false, world.instanceId);
}
}