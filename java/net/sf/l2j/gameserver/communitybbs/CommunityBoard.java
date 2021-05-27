/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.communitybbs;

import net.sf.l2j.Config;

import net.sf.l2j.gameserver.communitybbs.Manager.ClanBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PostBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.PvPBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RaidStatusBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopBBSManager;
import net.sf.l2j.gameserver.communitybbs.Manager.TopicBBSManager;
import net.sf.l2j.gameserver.handler.voicedcommandhandlers.Wedding;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.network.L2GameClient;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;
import cz.nxs.interf.NexusEvents;

public class CommunityBoard
{
private CommunityBoard()
{
}

public static CommunityBoard getInstance()
{
	return SingletonHolder._instance;
}

public void handleCommands(L2GameClient client, String command)
{
	final L2PcInstance activeChar = client.getActiveChar();
	
	if (activeChar == null)
		return;
	
	if(NexusEvents.cbBypass(activeChar, command))
		return;
	
	switch (Config.COMMUNITY_TYPE)
	{
	default:
	case 0: //disabled
		/*activeChar.sendPacket(new SystemMessage(SystemMessageId.CB_OFFLINE));*/
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		break;
	case 1: // old
		RegionBBSManager.getInstance().parsecmd(command, activeChar);
		break;
	case 2: // new
		if (command.startsWith("_bbsclan"))
		{
			ClanBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsmemo"))
		{
			TopicBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbstopics"))
		{
			TopicBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsposts"))
		{
			PostBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbstop"))
		{
			TopBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbshome"))
		{
			TopBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsloc"))
		{
			RegionBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsgetfav") || command.startsWith("_bbspvp") || command.startsWith("_bbsfame") || command.startsWith("_bbspk"))
		{
			PvPBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsraid"))
		{
			RaidStatusBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsclannotice"))
		{
			ClanBBSManager.getInstance().parsecmd(command, activeChar);
		}
		else if (command.startsWith("_bbsengage"))
		{
			Wedding.engage(activeChar);
		}
		else if (command.startsWith("_bbsdivorce"))
		{
			Wedding.divorce(activeChar);
		}
		else if (command.startsWith("_bbsgotolove"))
		{
			Wedding.goToLove(activeChar);
		}
		else if (command.startsWith("_bbstradeoff"))
		{
			activeChar.setTradeRefusal(true);
		}
		else if (command.startsWith("_bbstradeon"))
		{
			activeChar.setTradeRefusal(false);
		}
		else if (command.startsWith("_bbsaccd"))
		{
			byte i = activeChar.getAccDisplay();
			i = (byte) (i + 1);
			if (i > 3)
				i = 0;
			
			activeChar.setAccDisplay(i);
			
			switch (i)
			{
			case 0:
				activeChar.sendMessage("Displaying both of your accessories");
				break;
			case 1:
				activeChar.sendMessage("Displaying only your hair accessory");
				break;
			case 2:
				activeChar.sendMessage("Displaying only your face accessory");
				break;
			case 3:
				activeChar.sendMessage("Displaying none of your accessories");
				break;
			}
			activeChar.broadcastUserInfo();
		}
		else if (command.startsWith("_bbsroll"))
		{
			if (!activeChar.getFloodProtectors().getRollDice().tryPerformAction("roll dice"))
				return;
			
			final int number = Rnd.get(1, 100);
			
			SystemMessage sm = new SystemMessage(SystemMessageId.C1_ROLLED_S2);
			sm.addCharName(activeChar);
			sm.addNumber(number);
			
			activeChar.sendPacket(sm);
			if (activeChar.isInsideZone(L2Character.ZONE_PEACE))
				Broadcast.toKnownPlayers(activeChar, sm);
			else if (activeChar.isInParty())
				activeChar.getParty().broadcastToPartyMembers(activeChar, sm);
		}
		else if (command.startsWith("_bbslockdown"))
		{
			double timeInHours = 4.2;
						
			if (timeInHours < 0.1 || timeInHours > 504)
			{
				activeChar.sendMessage("It's a minimum of 0.1 and a maximum of 504 hours");
			}
			
			activeChar.doLockdown(timeInHours);
		}
		else if (command.startsWith("_bbslockdowntime"))
		{
			if (activeChar.isAccountLockedDown())
				activeChar.sendLockdownTime();
			else
				activeChar.sendMessage("Your account is not locked down");
		}
		else if (command.startsWith("_bbsadenaclanwh"))
		{
			final L2Clan clan = activeChar.getClan();
			
			if (clan == null)
			{
				activeChar.sendMessage("You don't have a clan");
			}
			
			long num = Long.MAX_VALUE;
			
			
			if (num <= 100 || num > Long.MAX_VALUE)
			{
				activeChar.sendMessage("Adena value has to be greater than 100");
			}
			
			if (activeChar.getInventory().getInventoryItemCount(57, 0) >= num)
			{
				final ItemContainer wh = clan.getWarehouse();
				
				if (wh != null)
				{
					InventoryUpdate iu = new InventoryUpdate();
					
					final int adenaObjId = activeChar.getInventory().getAdenaInstance().getObjectId();
					
					activeChar.getInventory().transferItem("ClanWH deposit Adena", adenaObjId, num, wh, activeChar, null);
					activeChar.getInventory().updateDatabase();
					
					activeChar.sendPacket(iu);
					activeChar.sendMessage(num+" Adenas has been deposited to your clan warehouse");
				}
			}
			else
			{
				activeChar.sendMessage("You don't have that much Adena");
			}
		}
		else
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
					+ " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
		}
		break;
	}
}



/**
 * @param client
 * @param url
 * @param arg1
 * @param arg2
 * @param arg3
 * @param arg4
 * @param arg5
 */
public void handleWriteCommands(L2GameClient client, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
{
	L2PcInstance activeChar = client.getActiveChar();
	if (activeChar == null)
		return;
	
	switch (Config.COMMUNITY_TYPE)
	{
	case 2:
		if (url.equals("Topic"))
		{
			TopicBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		}
		else if (url.equals("Post"))
		{
			PostBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		}
		else if (url.equals("Region"))
		{
			RegionBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		}
		else if (url.equals("Notice"))
		{
			ClanBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		}
		else
		{
			ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + url
					+ " is not implemented yet</center><br><br></body></html>", "101");
			activeChar.sendPacket(sb);
		}
		break;
	case 1:
		RegionBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, activeChar);
		break;
	default:
	case 0:
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>The Community board is currently disabled</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		break;
	}
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final CommunityBoard _instance = new CommunityBoard();
}
}
