package net.sf.l2j.gameserver.instances.DVC;

import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class DVC extends Quest
{
//NPCs
private static int MALKION = 96012;
private static int EXIT_TELEPORTER = 90013;

//BOSSES
private static final int[] BOSSES = {95602,95603,95622,95623,95626,95628,95632,95644,95654,95646};

//final bosses
private static final int[] GRAND_BOSSES = {95631,95634};

//MOBS
private static final int[] MOBS   = {95123,95124,95125,95126,95127,95128,95129,95130,95132,95133,95134,95135,95136,95629,95630,95638};

private static String qn = "DVC";
private static final int INSTANCEID = 2001;

//REQUIREMENTS
private static boolean debug = false;
private static int levelReq = 86;
private static int pvpReq = 100;
private static int fameReq = 0;
private static int pkReq = 0;


private class teleCoord {int instanceId; int x; int y; int z;}

public class dvcWorld extends InstanceWorld
{
public dvcWorld()
{
	InstanceManager.getInstance().super();
}
}

public DVC(int questId, String name, String descr)
{
	super(questId, name, descr);
	
	addStartNpc(MALKION);
	addTalkId(MALKION);
	addTalkId(EXIT_TELEPORTER);
	
	for (int boss : BOSSES)
		addKillId(boss);
	
	for (int mob : MOBS)
		addKillId(mob);
	
	for (int mob : GRAND_BOSSES)
		addKillId(mob);
}

public static void main(String[] args)
{
	new DVC(-1, qn, "instances");
}

private boolean checkConditions(L2PcInstance player, boolean single)
{
	if (debug || player.isGM())
		return true;
	else
	{
		final L2Party party = player.getParty();
		
		if (!single && (party == null || party.getMemberCount() < 4 || party.getMemberCount() > 7))
		{
			player.sendMessage("This is a 4-7 player party instance, so you must have a party of 4-7 people");
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
					ptm.sendMessage("You can only enter this instance once every day, wait until the next 12AM");
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
		
		instanceId = InstanceManager.getInstance().createDynamicInstance(template);
		world = new dvcWorld();
		world.instanceId = instanceId;
		world.templateId = INSTANCEID;
		InstanceManager.getInstance().addWorld(world);
		_log.info("DVC: new " + template + " Instance: " + instanceId + " created by player: " + player.getName());
		
		// teleport players
		teleto.instanceId = instanceId;
		
		spawnMobs((dvcWorld) world, player);
		spawnBosses((dvcWorld) world, player);
		spawnGrandBosses((dvcWorld) world, player);
		L2Party party = player.getParty();
		
		if (party == null)
		{
			if (!player.isGM())
				return 0;
			
			// this can happen only if debug is true
			InstanceManager.getInstance().setInstanceTime(player.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));
			world.allowed.add(player.getObjectId());
			auditInstances(player, template, instanceId);
			teleportplayer(player,teleto);
			spawnExitGK((dvcWorld) world, player);
		}
		else
		{
			for (L2PcInstance partyMember : party.getPartyMembers())
			{
				partyMember.sendMessage("You have entered the Dragon Valley Caves");
				InstanceManager.getInstance().setInstanceTime(partyMember.getAccountName(), INSTANCEID, getNextInstanceTime(TWODAYS));
				world.allowed.add(partyMember.getObjectId());
				auditInstances(partyMember, template, instanceId);
				teleportplayer(partyMember,teleto);
			}
			
			spawnExitGK((dvcWorld) world, player);
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
	
	if (npcId == MALKION)
	{
		teleCoord teleto = new teleCoord();
		teleto.x = 131175;
		teleto.y = 114415;
		teleto.z = -3715;
		enterInstance(player, "DVC.xml", teleto);
	}
	else if (npcId == EXIT_TELEPORTER)
	{
		final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
		
		if (world == null || !(world instanceof dvcWorld))
			return null;
		
		teleCoord teleto = new teleCoord();
		teleto.x = -82993;
		teleto.y = 150860;
		teleto.z = -3129;
		
		if (player.getParty() == null)
		{
			exitInstance(player, teleto);
			player.sendPacket(new ExShowScreenMessage("You have completed the DVC instance", 6000));
		}
		else
		{
			for (L2PcInstance ptm : player.getParty().getPartyMembers())
			{
				exitInstance(ptm, teleto);
				player.sendPacket(new ExShowScreenMessage("You have completed the DVC instance", 6000));
			}
		}
		
		st.exitQuest(true);
	}
	
	return null;
}
@Override
public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	return null;
}
public void spawnExitGK(dvcWorld world, L2PcInstance player)
{
	addSpawn(EXIT_TELEPORTER, 153073, 122107, -3805, 0, false, 0, false, world.instanceId);
}

public void spawnMobs(dvcWorld world, L2PcInstance player)
{
	addSpawn(MOBS[0], 141413, 121634, -3911, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 141126, 121005, -3911, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140325, 121046, -3911, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140955, 115168, -3715, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 142110, 119296, -3907, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140554, 118040, -3907, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140412, 118502, -3907, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 142136, 117306, -3907, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 143187, 117451, -3907, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 154746, 115390, -5253, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 151117, 115771, -5471, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 151005, 115572, -5471, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 151049, 114791, -5471, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 151148, 114481, -5454, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 149490, 114707, -5471, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 149453, 115038, -5471, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 149495, 115319, -5471, 0, false, 0, false, world.instanceId); //Tortured Man	
	addSpawn(MOBS[0], 152338, 110566, -5519, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 152463, 110268, -5519, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 142589, 107114, -3943, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 142445, 108984, -3943, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140802, 110393, -3943, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 140642, 109175, -3943, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 144405, 114849, -3719, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 144906, 114527, -3719, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 144958, 114934, -3719, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 146211, 116114, -3719, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148715, 118347, -3709, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148597, 118268, -3710, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148597, 118070, -3710, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148618, 117872, -3710, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148626, 117659, -3710, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148828, 117519, -3710, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 149244, 121262, -4861, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[0], 148413, 120449, -4861, 0, false, 0, false, world.instanceId); //Tortured Man
	addSpawn(MOBS[1], 139841, 114482, -3719, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 142582, 117632, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 142638, 117118, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 142143, 116783, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140866, 117889, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140055, 117581, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140451, 117524, -3907, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140508, 115108, -3718, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140200, 114277, -3715, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 139833, 114116, -3715, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 142977, 121227, -3911, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 143369, 121293, -3911, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 143760, 120655, -3911, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 146022, 122016, -3911, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 147118, 118744, -4106, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153000, 115193, -5253, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153393, 115643, -5253, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153112, 115899, -5253, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 151707, 115166, -5471, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 151287, 115468, -5471, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 151346, 115187, -5471, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 151327, 114879, -5471, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 151474, 114662, -5469, 0, false, 0, false, world.instanceId); //Disfigured One	
	addSpawn(MOBS[1], 153679, 112431, -5519, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153330, 111722, -5519, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153385, 111374, -5519, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 149475, 109472, -5214, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153758, 108621, -5151, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153429, 108275, -5151, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 153293, 107728, -5151, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 152673, 121748, -3803, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 145276, 108901, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 144713, 107683, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 144535, 108115, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 142499, 110710, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 143391, 112527, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 144112, 112787, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140232, 110367, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 141029, 109899, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 140309, 109715, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[1], 141434, 109286, -3943, 0, false, 0, false, world.instanceId); //Disfigured One
	addSpawn(MOBS[2], 147209, 120057, -4515, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 148138, 121114, -4778, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 146879, 119299, -4250, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 141987, 121497, -3911, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140693, 120710, -3894, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142687, 119457, -3910, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142373, 119660, -3907, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 143608, 116809, -3907, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142534, 116556, -3907, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140178, 119256, -3894, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 141172, 117545, -3886, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140137, 118015, -3907, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140833, 114382, -3699, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140016, 114801, -3695, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142746, 120876, -3911, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 144310, 120929, -3903, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 146314, 121267, -3911, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 154320, 116963, -5240, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 153727, 114592, -5253, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 151561, 115396, -5471, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 151611, 114969, -5471, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 152075, 113611, -5509, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 153087, 112908, -5497, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 154392, 111784, -5519, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 153252, 108601, -5117, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 149608, 108204, -4497, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 150133, 108147, -4634, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 146629, 111154, -3547, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 152265, 119685, -3782, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 145091, 108040, -3925, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142381, 107339, -3943, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142250, 106993, -3943, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142730, 111241, -3943, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 140998, 108670, -3929, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 141458, 112546, -3710, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 142414, 113472, -3699, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[2], 145248, 115670, -3719, 0, false, 0, false, world.instanceId); //Soul Flayer
	addSpawn(MOBS[3], 135610, 113835, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 135759, 114266, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 135719, 114641, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 135905, 114889, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 136275, 114840, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 136367, 114563, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 136336, 114174, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 136150, 113837, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 136985, 114139, -3715, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 137141, 114597, -3703, 0, false, 0, false, world.instanceId); //Mistake of the maker
    addSpawn(MOBS[3], 153456, 116410, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 153654, 116645, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 154053, 116569, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 154466, 116213, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 154527, 115538, -5243, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 154295, 115240, -5229, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 154068, 115027, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 153546, 115250, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 153678, 115553, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 153661, 115940, -5253, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 140526, 110022, -3925, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[3], 145788, 115790, -3696, 0, false, 0, false, world.instanceId); //Mistake of the maker
	addSpawn(MOBS[4], 142998, 116128, -3896, 0, false, 0, false, world.instanceId); //Magmacoil
    addSpawn(MOBS[4], 143118, 118158, -3891, 0, false, 0, false, world.instanceId); //Magmacoil
    addSpawn(MOBS[4], 142138, 118780, -3892, 0, false, 0, false, world.instanceId); //Magmacoil
    addSpawn(MOBS[4], 140209, 121655, -3893, 0, false, 0, false, world.instanceId); //Magmacoil
    addSpawn(MOBS[4], 142396, 121141, -3892, 0, false, 0, false, world.instanceId); //Magmacoil
    addSpawn(MOBS[4], 148599, 119690, -4838, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 149762, 120118, -4849, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 154105, 114717, -5237, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 150700, 115165, -5458, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 153916, 111999, -5498, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 150242, 112653, -5509, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 153623, 109590, -5135, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 148718, 111876, -3703, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 152153, 121322, -3789, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 147927, 109571, -3929, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 145372, 109243, -3927, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 142822, 107465, -3924, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[4], 141924, 107140, -3922, 0, false, 0, false, world.instanceId); //Magmacoil
	addSpawn(MOBS[5], 145260, 120441, -3911, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 144835, 116939, -3911, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 150058, 121738, -4861, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 145260, 120441, -3911, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 150168, 121041, -4861, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 148734, 114936, -5471, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 154011, 111244, -5504, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 150068, 111408, -5519, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 152598, 119371, -3778, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 152227, 120106, -3803, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[5], 143976, 110317, -3922, 0, false, 0, false, world.instanceId); //Horror Knight
	addSpawn(MOBS[6], 146415, 109708, -3414, 0, false, 0, false, world.instanceId); //Bone Fiend
	addSpawn(MOBS[6], 147886, 112671, -3719, 0, false, 0, false, world.instanceId); //Bone Fiend
	addSpawn(MOBS[6], 148204, 112027, -3719, 0, false, 0, false, world.instanceId); //Bone Fiend
	addSpawn(MOBS[6], 148676, 112638, -3719, 0, false, 0, false, world.instanceId); //Bone Fiend
	addSpawn(MOBS[6], 150901, 118057, -3693, 0, false, 0, false, world.instanceId); //Bone Fiend
	addSpawn(MOBS[7], 147272, 112549, -3719, 0, false, 0, false, world.instanceId); //Hemagorgon
	addSpawn(MOBS[7], 148906, 112377, -3719, 0, false, 0, false, world.instanceId); //Hemagorgon
	addSpawn(MOBS[7], 148885, 113056, -3719, 0, false, 0, false, world.instanceId); //Hemagorgon
	addSpawn(MOBS[7], 151760, 118632, -3898, 0, false, 0, false, world.instanceId); //Hemagorgon
	addSpawn(MOBS[8], 143250, 120581, -3911, 0, false, 0, false, world.instanceId); //Puker
    addSpawn(MOBS[8], 143668, 120182, -3911, 0, false, 0, false, world.instanceId); //Puker
    addSpawn(MOBS[8], 150684, 111655, -5519, 0, false, 0, false, world.instanceId); //Puker
	addSpawn(MOBS[8], 150239, 111866, -5519, 0, false, 0, false, world.instanceId); //Puker
	addSpawn(MOBS[8], 150432, 111074, -5519, 0, false, 0, false, world.instanceId); //Puker
	addSpawn(MOBS[8], 152431, 109323, -5151, 0, false, 0, false, world.instanceId); //Puker
	addSpawn(MOBS[8], 153706, 109249, -5151, 0, false, 0, false, world.instanceId); //Puker
	addSpawn(MOBS[9], 145988, 120239, -3911, 0, false, 0, false, world.instanceId); //Stag Beast
	addSpawn(MOBS[9], 145718, 120331, -3911, 0, false, 0, false, world.instanceId); //Stag Beast
	addSpawn(MOBS[10], 144760, 117630, -3911, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 145264, 117182, -3896, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 148907, 120753, -4861, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 149669, 120570, -4849, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 149169, 119546, -4861, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 153072, 119534, -3803, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 152741, 120000, -3803, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 144746, 109021, -3943, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[10], 144534, 112319, -3932, 0, false, 0, false, world.instanceId); //Oblivion Knight
	addSpawn(MOBS[11], 151771, 112734, -5519, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 151157, 109322, -5134, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 151081, 108763, -5151, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 154853, 108703, -5151, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 154863, 108361, -5151, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 154703, 108091, -5151, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 153833, 107192, -5151, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 150739, 107364, -4773, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 148197, 107460, -4134, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 149154, 113932, -3719, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[11], 149061, 114267, -3719, 0, false, 0, false, world.instanceId); //Lost Warden
	addSpawn(MOBS[12], 152932, 109419, -5151, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 152957, 109159, -5151, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 147250, 107674, -3969, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 147009, 107490, -3943, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 146461, 108960, -3501, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 146823, 112686, -3719, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 147483, 112130, -3719, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 150252, 117366, -3694, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 150468, 117210, -3694, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 152074, 119181, -3799, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[12], 152228, 119010, -3799, 0, false, 0, false, world.instanceId); //Mutation Drake
	addSpawn(MOBS[14], 149179, 115084, -3719, 0, false, 0, false, world.instanceId); //Dragonsor
	addSpawn(MOBS[15], 143244, 120912, -3911, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 143425, 120335, -3911, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 143684, 119934, -3911, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 148907, 114766, -5471, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 148811, 115194, -5473, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 143214, 114365, -3719, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 143448, 114081, -3719, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 146956, 116773, -3697, 0, false, 0, false, world.instanceId); //Forsaken Warlord
    addSpawn(MOBS[15], 147292, 116624, -3697, 0, false, 0, false, world.instanceId); //Forsaken Warlord
}

public void spawnBosses(dvcWorld world, L2PcInstance player)
{
	addSpawn(BOSSES[0], 145162, 113376, -3715, 0, false, 0, false, world.instanceId); //Fulkard
	addSpawn(BOSSES[2], 145712, 117626, -3907, 0, false, 0, false, world.instanceId); //Argekunte
    addSpawn(BOSSES[3], 148118, 110330, -3939, 0, false, 0, false, world.instanceId); //Gargantuan
	addSpawn(BOSSES[4], 152043, 110372, -5515, 0, false, 0, false, world.instanceId); //Astaroth
	addSpawn(BOSSES[5], 152415, 107670, -5063, 0, false, 0, false, world.instanceId); //Hitchkarshiek
	addSpawn(BOSSES[6], 145944, 119297, -3907, 0, false, 0, false, world.instanceId); //D-Elm-Lars
	addSpawn(BOSSES[7], 146419, 111716, -3559, 0, false, 0, false, world.instanceId); //Quad Kill
    addSpawn(BOSSES[8], 148932, 117878, -3687, 0, false, 0, false, world.instanceId); //Anencephalic Man
    addSpawn(BOSSES[9], 143322, 108810, -3939, 0, false, 0, false, world.instanceId); //Nosferatu
}

public void spawnGrandBosses(dvcWorld world, L2PcInstance player)
{
	addSpawn(GRAND_BOSSES[0], 154306, 118617, -3799, 0, false, 0, false, world.instanceId); // Rinma Estilgan
	addSpawn(GRAND_BOSSES[1], 154461, 121174, -3799, 0, false, 0, false, world.instanceId); // Darion
}
}