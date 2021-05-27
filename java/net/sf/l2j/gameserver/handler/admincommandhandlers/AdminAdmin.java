package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.AccessLevels;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.BufferSkillsTable;
import net.sf.l2j.gameserver.datatables.EnchantHPBonusData;
import net.sf.l2j.gameserver.datatables.FakePcsTable;
import net.sf.l2j.gameserver.datatables.ItemLists;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.NpcWalkerRoutesTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.Manager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.RaidBossSpawnManager;
import net.sf.l2j.gameserver.model.L2Multisell;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2RaidBossInstance;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles following admin commands:
 * - admin|admin1/admin2/admin3/admin4/admin5 = slots for the 5 starting admin menus
 * - gmliston/gmlistoff = includes/excludes active character from /gmlist results
 * - silence = toggles private messages acceptance mode
 * - diet = toggles weight penalty mode
 * - tradeoff = toggles trade acceptance mode
 * - reload = reloads specified component from multisell|skill|npc|htm|item|instancemanager
 * - set/set_menu/set_mod = alters specified server setting
 * - saveolymp = saves olympiad state manually
 * - manualhero = cycles olympiad and calculate new heroes.
 * @version $Revision: 1.3.2.1.2.4 $ $Date: 2007/07/28 10:06:06 $
 */
public class AdminAdmin implements IAdminCommandHandler
{

private static final String[] ADMIN_COMMANDS =
{
	"admin_admin",
	"admin_admin1",
	"admin_admin2",
	"admin_admin3",
	"admin_admin4",
	"admin_admin5",
	"admin_gmliston",
	"admin_gmlistoff",
	"admin_silence",
	"admin_diet",
	"admin_tradeoff",
	"admin_reload",
	"admin_set",
	"admin_set_menu",
	"admin_set_mod",
	"admin_saveolymp",
	"admin_manualhero",
	"admin_sethero",
	"admin_killAllRaids",
	"admin_upraids",
	"admin_gotoraid",
	"admin_endolympiad",
	"admin_setppl"
};

public boolean useAdminCommand(String command, L2PcInstance activeChar)
{
	
	if (command.startsWith("admin_admin"))
	{
		showMainPage(activeChar, command);
	}
	else if (command.startsWith("admin_gmliston"))
	{
		GmListTable.getInstance().showGm(activeChar);
		activeChar.sendMessage("Registered into gm list");
	}
	else if (command.startsWith("admin_gmlistoff"))
	{
		if (activeChar.getAccessLevel().getLevel() > 3)
		{
			GmListTable.getInstance().hideGm(activeChar);
			activeChar.sendMessage("Removed from gm list");
		}
	}
	else if (command.startsWith("admin_silence"))
	{
		if (activeChar.getMessageRefusal()) // already in message refusal mode
		{
			activeChar.setMessageRefusal(false);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_ACCEPTANCE_MODE));
		}
		else
		{
			activeChar.setMessageRefusal(true);
			activeChar.sendPacket(new SystemMessage(SystemMessageId.MESSAGE_REFUSAL_MODE));
		}
	}
	else if (command.startsWith("admin_saveolymp"))
	{
		Olympiad.getInstance().saveOlympiadStatus();
		activeChar.sendMessage("olympiad system saved.");
	}
	else if (command.startsWith("admin_endolympiad"))
	{
		try
		{
			Olympiad.getInstance().manualSelectHeroes();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		activeChar.sendMessage("Heroes formed");
	}
	else if (command.startsWith("admin_manualhero") || command.startsWith("admin_sethero"))
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		if (target instanceof L2PcInstance)
			player = (L2PcInstance) target;
		else
			return false;
		player.setHero(!player.isHero());
		if (player.isHero())
			player.broadcastPacket(new SocialAction(player.getObjectId(), 16));
		player.sendMessage("Admin changed your hero status");
		player.broadcastUserInfo();
	}
	else if (command.startsWith("admin_diet"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			st.nextToken();
			if (st.nextToken().equalsIgnoreCase("on"))
			{
				activeChar.setDietMode(true);
				activeChar.sendMessage("Diet mode on");
			}
			else if (st.nextToken().equalsIgnoreCase("off"))
			{
				activeChar.setDietMode(false);
				activeChar.sendMessage("Diet mode off");
			}
		}
		catch (Exception ex)
		{
			if (activeChar.getDietMode())
			{
				activeChar.setDietMode(false);
				activeChar.sendMessage("Diet mode off");
			}
			else
			{
				activeChar.setDietMode(true);
				activeChar.sendMessage("Diet mode on");
			}
		}
	}
	else if (command.startsWith("admin_tradeoff"))
	{
		try
		{
			String mode = command.substring(15);
			if (mode.equalsIgnoreCase("on"))
			{
				activeChar.setTradeRefusal(true);
				activeChar.sendMessage("Trade refusal enabled");
			}
			else if (mode.equalsIgnoreCase("off"))
			{
				activeChar.setTradeRefusal(false);
				activeChar.sendMessage("Trade refusal disabled");
			}
		}
		catch (Exception ex)
		{
			if (activeChar.getTradeRefusal())
			{
				activeChar.setTradeRefusal(false);
				activeChar.sendMessage("Trade refusal disabled");
			}
			else
			{
				activeChar.setTradeRefusal(true);
				activeChar.sendMessage("Trade refusal enabled");
			}
		}
	}
	else if (command.startsWith("admin_reload"))
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		try
		{
			String type = st.nextToken();
			if (type.equals("multisell"))
			{
				L2Multisell.getInstance().reload();
				activeChar.sendMessage("multisell reloaded");
				System.out.println("multisell reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("teleport"))
			{
				TeleportLocationTable.getInstance().reloadAll();
				activeChar.sendMessage("teleport location table reloaded");
				System.out.println("teleporation locations reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("skill"))
			{
				SkillTable.getInstance().reload();
				activeChar.sendMessage("skills reloaded");
				System.out.println("skills reloaded by "+activeChar.getName());
			}
			else if (type.equalsIgnoreCase("npc") || type.equalsIgnoreCase("npcs"))
			{
				FakePcsTable.getInstance().load();
				NpcTable.getInstance().reloadAllNpc();
				QuestManager.getInstance().reloadAllQuests();
				activeChar.sendMessage("npcs reloaded");
				System.out.println("NPCs reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("htm"))
			{
				HtmCache.getInstance().reload();
				activeChar.sendMessage("Cache[HTML]: " + HtmCache.getInstance().getMemoryUsage() + " megabytes on " + HtmCache.getInstance().getLoadedFiles() + " files loaded");
				System.out.println("HTMLs reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("item"))
			{
				ItemTable.getInstance().reload();
				activeChar.sendMessage("Item templates reloaded");
				System.out.println("items reloaded by "+activeChar.getName());
				EnchantHPBonusData.getInstance().reload();
				ItemLists.getInstance().loadLists();
				activeChar.sendMessage("Item lists have been reloaded");
				System.out.println("ItemList table reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("config"))
			{
				Config.load();
				activeChar.sendMessage("All config settings have been reload");
				System.out.println("Configs reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("instancemanager"))
			{
				Manager.reloadAll();
				activeChar.sendMessage("All instance manager has been reloaded");
				System.out.println("InstanceManager reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("npcwalkers"))
			{
				NpcWalkerRoutesTable.getInstance().load();
				activeChar.sendMessage("All NPC walker routes have been reloaded");
				System.out.println("NPCwalkers reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("access"))
			{
				AccessLevels.getInstance().reloadAccessLevels();
				AdminCommandAccessRights.getInstance().reloadAdminCommandAccessRights();
				activeChar.sendMessage("Access Rights have been reloaded");
				System.out.println("Access level rights reloaded by "+activeChar.getName());
			}
			else if (type.startsWith("quests"))
			{
				QuestManager.getInstance().reloadAllQuests();
				activeChar.sendMessage("All Quests have been reloaded");
				System.out.println("quests reloaded by "+activeChar.getName());
			}
			else if(type.startsWith("npcbuffer"))
			{
				BufferSkillsTable.reload();
				activeChar.sendMessage("Buffer skills table has been reloaded");
				System.out.println("NPCBuffer table reloaded by "+activeChar.getName());
			}
			else if(type.startsWith("list"))
			{
				ItemLists.getInstance().loadLists();
				activeChar.sendMessage("Item lists have been reloaded");
				System.out.println("ItemList table reloaded by "+activeChar.getName());
			}
		}
		catch (Exception e)
		{
			activeChar.sendMessage("Usage:  //reload <multisell|skill|npc|htm|item|teleport|config|instancemanager|npcwalkers|access|quests|npcbuffer>");
		}
	}
	else if (command.startsWith("admin_set"))
	{
		StringTokenizer st = new StringTokenizer(command);
		String[] cmd = st.nextToken().split("_");
		try
		{
			String[] parameter = st.nextToken().split("=");
			String pName = parameter[0].trim();
			String pValue = parameter[1].trim();
			if (Config.setParameterValue(pName, pValue))
				activeChar.sendMessage("parameter " + pName + " succesfully set to " + pValue);
			else
				activeChar.sendMessage("Invalid parameter!");
		}
		catch (Exception e)
		{
			if (cmd.length == 2)
				activeChar.sendMessage("Usage: //set parameter=value");
		}
		finally
		{
			if (cmd.length == 3)
			{
				if (cmd[2].equalsIgnoreCase("menu"))
					AdminHelpPage.showHelpPage(activeChar, "settings.htm");
				else if (cmd[2].equalsIgnoreCase("mod"))
					AdminHelpPage.showHelpPage(activeChar, "mods_menu.htm");
			}
		}
	}
	else if (command.startsWith("admin_killAllRaids"))
	{
		try
		{
			RaidBossSpawnManager.getInstance().updateDbKillAll();
		}
		catch (Exception e)
		{
		}
		finally
		{
			activeChar.sendMessage("All Raidbosses have been set to respawn in the database.");
		}
	}
	else if (command.startsWith("admin_upraids"))
	{
		try
		{
			L2RaidBossInstance.showRaidsThatAreUp(activeChar);
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_gotoraid"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			
			st.nextToken();
			int number = 0;
			
			try
			{
				number = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You didn't specify a number!");
				return true;
			}
			
			if (number < 0)
				return true;
			
			L2RaidBossInstance.goToRaid(activeChar, number);
		}
		catch (Exception e)
		{
		}
	}
	else if (command.startsWith("admin_fakeppl"))
	{
		try
		{
			StringTokenizer st = new StringTokenizer(command);
			
			st.nextToken();
			int number = 0;
			
			try
			{
				number = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("You didn't specify a number!");
				return true;
			}
			
			if (number < 0)
				return false;
			
			L2World.FAKE_NUMBER_OF_ONLINE_PEOPLE = number;
			activeChar.sendMessage("Number of online players set to "+number);
		}
		catch (Exception e)
		{
		}
	}
	return true;
}

public String[] getAdminCommandList()
{
	return ADMIN_COMMANDS;
}

private void showMainPage(L2PcInstance activeChar, String command)
{
	int mode = 0;
	String filename = null;
	try
	{
		mode = Integer.parseInt(command.substring(11));
	}
	catch (Exception e)
	{
	}
	switch (mode)
	{
	case 1:
		filename = "main";
		break;
	case 2:
		filename = "game";
		break;
	case 3:
		filename = "effects";
		break;
	case 4:
		filename = "server";
		break;
	case 5:
		filename = "mods";
		break;
	default:
		if (Config.GM_ADMIN_MENU_STYLE.equals("modern"))
			filename = "main";
		else
			filename = "classic";
		break;
	}
	AdminHelpPage.showHelpPage(activeChar, filename + "_menu.htm");
}
}
