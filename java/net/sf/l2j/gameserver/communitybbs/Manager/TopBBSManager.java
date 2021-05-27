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
package net.sf.l2j.gameserver.communitybbs.Manager;

//import java.awt.Desktop;
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
import java.util.StringTokenizer;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class TopBBSManager extends BaseBBSManager
{
private TopBBSManager()
{
}

//Method to redirect players to youporn
/*public void redirectToWebsite() throws IOException, URISyntaxException
{
	if (Desktop.isDesktopSupported() ) {
		Desktop.getDesktop().browse(new URI("http://www.l2prideful.com"));
	}
}*/
/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	if (command.equals("_bbstop"))
	{
		String content = HtmCache.getInstance().getHtm("data/html/CommunityBoard/index.htm");
		if (content == null)
		{
			content = "<html><body><br><br><center>404 :File Not foud: 'data/html/CommunityBoard/index.htm' </center></body></html>";
		}
				
		separateAndSend(content, activeChar);
	}
	else if (command.equals("_bbshome"))
	{
		String content = HtmCache.getInstance().getHtm("data/html/CommunityBoard/index.htm");
		if (content == null)
		{
			content = "<html><body><br><br><center>404 :File Not foud: 'data/html/CommunityBoard/index.htm' </center></body></html>";
		}
		/*try {
			redirectToWebsite();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		separateAndSend(content, activeChar);
	}
	else if (command.startsWith("_bbstop;"))
	{
		StringTokenizer st = new StringTokenizer(command, ";");
		st.nextToken();
		int idp = Integer.parseInt(st.nextToken());
		String content = HtmCache.getInstance().getHtm("data/html/CommunityBoard/" + idp + ".htm");
		content = content.replace("%clanname%", activeChar.getClan() != null ? activeChar.getClan().getName() : "N/A");
		content = content.replace("%allycrest%", activeChar.getAllyId() > 0  && activeChar.getAllyCrestId() > 0 ? "Crest.crest_" + Config.REQUEST_ID + "_" + String.valueOf(activeChar.getClan().getAllyCrestId()) + "" : "N/A");
		content = content.replace("%clancrest%", activeChar.getClan() != null && activeChar.getClanCrestId() > 0 ? "Crest.crest_" + Config.REQUEST_ID + "_" + String.valueOf(activeChar.getClan().getCrestId()) + "" : "N/A");
		content = content.replace("%leader%", activeChar.getClan() != null ? activeChar.getClan().getLeaderName() : "N/A");
		content = content.replace("%alliance%", activeChar.getClan() != null && activeChar.getClan().getAllyName() != null ? activeChar.getClan().getAllyName() : "N/A");
		content = content.replace("%clanlevel%", activeChar.getClan() != null ? String.valueOf(activeChar.getClan().getLevel()) : "N/A");
		content = content.replace("%base%", activeChar.getClan() != null && activeChar.getClan().getHasCastle() > 0 ? activeChar.getClan().getCastle().getName() : activeChar.getClan() != null && activeChar.getClan().getHasFort() > 0 ? activeChar.getClan().getFort().getName() : "N/A");
		content = content.replace("%clanhall%", activeChar.getClan() != null && ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()) != null ? ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan()).getName() : "N/A");
		content = content.replace("%membercount%", activeChar.getClan() != null ? String.valueOf(activeChar.getClan().getMembersCount()) : "N/A");
		content = content.replace("%clanrep%", activeChar.getClan() != null ? String.valueOf(activeChar.getClan().getReputationScore()) : "N/A");
		int aveLevel = 0;
		int total = 0;
		if (activeChar.getClan() != null)
		{
			for (L2ClanMember member : activeChar.getClan().getMembers())
			{
				int level = member.getLevel();
				total += level;
			}
			aveLevel = total / activeChar.getClan().getMembersCount();
		}
		
		content = content.replace("%avelevel%", activeChar.getClan() != null ? String.valueOf(aveLevel) : "N/A");		
		
		content = content.replace("%onlinemembs%", activeChar.getClan() != null ? String.valueOf(activeChar.getClan().getOnlineMembers(0).length) : "N/A");		
		String clanSkill = "null";
		int cell = 0;
		if (activeChar.getClan() != null)
		{
			for (L2Skill skill: activeChar.getClan().getAllSkills())
			{						
				cell++;
				if (cell == 1)
				{
					clanSkill += "<tr><td align=left valign=top><img src=\""+activeChar.getClan().getClanSkillsIcon(skill.getId())+ "\" width=\"32\" height=\"32\"/></td>";
				}			
				else if (cell < 12)
				{
					clanSkill += "<td FIXWIDTH=15></td><td align=left valign=top><img src=\""+activeChar.getClan().getClanSkillsIcon(skill.getId())+ "\" width=\"32\" height=\"32\"/></td>";		
				}
				else if (cell == 12)
				{
					cell = 0;
					clanSkill += "<td FIXWIDTH=15</td><td align=left valign=top><img src=\""+activeChar.getClan().getClanSkillsIcon(skill.getId())+ "\" width=\"32\" height=\"32\"/></td></tr>";
				}
			}
		}
		
		content = content.replace("%clanskilllist%", activeChar.getClan() != null && activeChar.getClan().getAllSkills() != null ? clanSkill : "No Skills to Show");
		
		String warlist = "null";
		
		if (activeChar.getClan() != null)
		{
			for (Integer clanId : activeChar.getClan().getWarList())
			{
				L2Clan clan = ClanTable.getInstance().getClan(clanId);
			
				if (clan == null) continue;
			
				warlist = clan.getName();
			}
		}
		content = content.replace("%clanwarlist%", activeChar.getClan() != null && activeChar.getClan().isAtWar() ? warlist : "No Active War");
		content = content.replace("%clanid%", activeChar.getClan() != null ? String.valueOf(activeChar.getClan().getClanId()) : "");		
		if (content == null)
		{
			content = "<html><body><br><br><center>404 :File Not foud: 'data/html/CommunityBoard/" + idp
			+ ".htm' </center></body></html>";
		}
		separateAndSend(content, activeChar);
	}
	else
	{
		ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
				+ " is not implemented yet</center><br><br></body></html>", "101");
		activeChar.sendPacket(sb);
		
	}
}


/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsewrite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
{
	// TODO Auto-generated method stub
	
}

/**
 * @return
 */
public static TopBBSManager getInstance()
{
	return SingletonHolder._instance;
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final TopBBSManager _instance = new TopBBSManager();
}
}