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

import java.util.StringTokenizer;

import javolution.text.TextBuilder;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.StringUtil;

public class ClanBBSManager extends BaseBBSManager
{
private ClanBBSManager()
{
}

/**
 * @return
 */
public static ClanBBSManager getInstance()
{
	return SingletonHolder._instance;
}

/**
 * @param command
 * @param activeChar
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	try
	{
		if (command.equals("_bbsclan"))
		{
			if (activeChar.getClan() == null || activeChar.getClan().getLevel() < 2)
				clanlist(activeChar, 1);
			else
				clanhome(activeChar);
		}
		else if (command.startsWith("_bbsclan_clanlist"))
		{
			if (command.equals("_bbsclan_clanlist"))
			{
				clanlist(activeChar, 1);
			}
			else if (command.startsWith("_bbsclan_clanlist;"))
			{
				StringTokenizer st = new StringTokenizer(command, ";");
				st.nextToken();
				int index = Integer.parseInt(st.nextToken());
				clanlist(activeChar, index);
			}
		}
		else if (command.startsWith("_bbsclan_clanhome"))
		{
			if (command.equals("_bbsclan_clanhome"))
			{
				clanhome(activeChar);
			}
			else if (command.startsWith("_bbsclan_clanhome;"))
			{
				StringTokenizer st = new StringTokenizer(command, ";");
				st.nextToken();
				int index = Integer.parseInt(st.nextToken());
				clanhome(activeChar, index);
			}
		}
		else if (command.startsWith("_bbsclan_clannotice_edit;"))
		{
			clanNotice(activeChar, activeChar.getClan().getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_enable"))
		{
			activeChar.getClan().setNoticeEnabled(true);
			clanNotice(activeChar, activeChar.getClan().getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_disable"))
		{
			activeChar.getClan().setNoticeEnabled(false);
			clanNotice(activeChar, activeChar.getClan().getClanId());
		}
		else
		{
			separateAndSend("<html><body><br><br><center>Command : " + command + " needs core development</center><br><br></body></html>", activeChar);
		}
	}
	catch (Exception e)
	{
	}
}

private void clanNotice(L2PcInstance activeChar, int clanId)
{
	L2Clan cl = ClanTable.getInstance().getClan(clanId);
	if (cl != null)
	{
		if (cl.getLevel() < 2)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_CB_IN_MY_CLAN));
			parsecmd("_bbsclan_clanlist", activeChar);
		}
		else
		{
			final StringBuilder html = StringUtil.startAppend(2000, "<html><body><br><br>"
					+ "<table border=0 width=610><tr><td width=10></td><td width=600 align=left>"
					+ "<a action=\"bypass _bbshome\">HOME</a> &gt; "
					+ "<a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a>  &gt; " + "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), "\"> &amp;$802; </a>"
					+ "</td></tr>" + "</table>");
			if (activeChar.isClanLeader())
			{
				StringUtil.append(html, "<br><br><center>"
						+ "<table width=610 border=0 cellspacing=0 cellpadding=0>"
						+ "<tr><td fixwidth=610><font color=\"AAAAAA\">The Clan Notice function allows the clan leader to send messages through a pop-up window to clan members at login.</font> </td></tr>"
						+ "<tr><td height=20></td></tr>");
				
				if (activeChar.getClan().isNoticeEnabled())
					StringUtil.append(html, "<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;on&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_disable\">off</a>");
				else
					StringUtil.append(html, "<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_enable\">on</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;off");
				
				StringUtil.append(html, "</td></tr>"
						+ "</table>"
						+ "<img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">"
						+ "<br> <br>"
						+ "<table width=610 border=0 cellspacing=2 cellpadding=0>"
						+ "<tr><td>Edit Notice: </td></tr>"
						+ "<tr><td height=5></td></tr>"
						+ "<tr><td>"
						+ "<MultiEdit var =\"Content\" width=610 height=100>"
						+ "</td></tr>"
						+ "</table>"
						+ "<br>"
						+ "<table width=610 border=0 cellspacing=0 cellpadding=0>"
						+ "<tr><td height=5></td></tr>"
						+ "<tr>"
						+ "<td align=center FIXWIDTH=65><button value=\"&$140;\" action=\"Write Notice Set _ Content Content Content\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\" ></td>"
						+ "<td align=center FIXWIDTH=45></td>" + "<td align=center FIXWIDTH=500></td>" + "</tr>" + "</table>"
						+ "</center>" + "</body>" + "</html>");
				send1001(html.toString(), activeChar);
				send1002(activeChar, activeChar.getClan().getNotice(), " ", "0");
			}
			else
			{
				StringUtil.append(html, "<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\">" + "<center>"
						+ "<table border=0 cellspacing=0 cellpadding=0><tr>"
						+ "<td align=center><font color=\"LEVEL\" name=\"hs9\">The Current Clan Notice</font></td>" + "</tr></table>");
				if (activeChar.getClan().isNoticeEnabled())
				{
					StringUtil.append(html, "<table border=0 cellspacing=1 cellpadding=1 bgcolor=666666>" + "<tr>"
							+ "<td FIXWIDTH=600 align=center>" + activeChar.getClan().getNotice() + "</td>" + "<td fixqqwidth=5></td>"
							+ "</tr>" + "</table>");
				}
				StringUtil.append(html, "</center>" + "</body>" + "</html>");
				separateAndSend(html.toString(), activeChar);
			}
		}
	}
}

private void clanlist(L2PcInstance activeChar, int index)
{
	if (index < 1)
	{
		index = 1;
	}
	//header
	final TextBuilder html = TextBuilder.newInstance();
	html.append("<html><body><br><br><center>");
	html.append("<br1><br1><table border=0 cellspacing=0 cellpadding=0>");
	html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
	html.append("<td width=610 height=30 align=left>");
	html.append("<a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a>");
	html.append("</td></tr></table>");
	html.append("<table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343>");
	html.append("<tr><td height=10></td></tr>");
	html.append("<tr>");
	html.append("<td fixWIDTH=5></td>");
	html.append("<td fixWIDTH=600>");
	html.append("<a action=\"bypass _bbsclan_clanhome;").append(
			activeChar.getClan() != null ? activeChar.getClan().getClanId() : 0).append(
			"\">[GO TO MY CLAN]</a>&nbsp;&nbsp;");
	html.append("</td>");
	html.append("<td fixWIDTH=5></td>");
	html.append("</tr>");
	html.append("<tr><td height=10></td></tr>");
	html.append("</table>");
	//body
	html.append("<br>");
	html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=5A5A5A width=610>");
	html.append("<tr>");
	html.append("<td FIXWIDTH=5></td>");
	html.append("<td FIXWIDTH=200 align=center>CLAN NAME</td>");
	html.append("<td FIXWIDTH=200 align=center>CLAN LEADER</td>");
	html.append("<td FIXWIDTH=100 align=center>CLAN LEVEL</td>");
	html.append("<td FIXWIDTH=100 align=center>CLAN MEMBERS</td>");
	html.append("<td FIXWIDTH=5></td>");
	html.append("</tr>");
	html.append("</table>");
	html.append("<img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
	int i = 0;
	for (L2Clan cl : ClanTable.getInstance().getClans())
	{
		if (cl.getLevel() < 6)
			continue;
		
		if (i > (index + 1) * 7)
		{
			break;
		}
		if (i++ >= (index - 1) * 7)
		{
			html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\">");
			html.append("<table border=0 cellspacing=0 cellpadding=0 width=610>");
			html.append("<tr> ");
			html.append("<td FIXWIDTH=5></td>");
			html.append("<td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;").append(
					cl.getClanId()).append("\">").append(cl.getName()).append("</a></td>");
			html.append("<td FIXWIDTH=200 align=center>").append(cl.getLeaderName()).append("</td>");
			html.append("<td FIXWIDTH=100 align=center>").append(cl.getLevel()).append("</td>");
			html.append("<td FIXWIDTH=100 align=center>").append(cl.getMembersCount()).append("</td>");
			html.append("<td FIXWIDTH=5></td>");
			html.append("</tr>");
			html.append("<tr><td height=5></td></tr>");
			html.append("</table>");
			html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\">");
			html.append("<img src=\"L2UI.SquareGray\" width=\"610\" height=\"1\">");
		}
	}
	html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"2\">");
	html.append("<table cellpadding=0 cellspacing=2 border=0><tr>");
	if (index == 1)
	{
		html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
	}
	else
	{
		html.append("<td><button action=\"_bbsclan_clanlist;").append(index - 1).append(
		"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
	}
	i = 0;
	int nbp;
	nbp = ClanTable.getInstance().getClans().length / 8;
	if (nbp * 8 != ClanTable.getInstance().getClans().length)
	{
		nbp++;
	}
	for (i = 1; i <= nbp; i++)
	{
		if (i == index)
		{
			html.append("<td> ").append(i).append(" </td>");
		}
		else
		{
			html.append("<td><a action=\"bypass _bbsclan_clanlist;").append(i).append("\"> ").append(i).append(" </a></td>");
		}
		
	}
	if (index == nbp)
	{
		html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
	}
	else
	{
		html.append("<td><button action=\"bypass _bbsclan_clanlist;").append(index + 1).append(
		"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
	}
	html.append("</tr></table>");
	html.append("<table border=0 cellspacing=0 cellpadding=0>");
	html.append("<tr><td width=610><img src=\"sek.cbui141\" width=\"610\" height=\"1\"></td></tr>");
	html.append("</table>");
	html.append("<table border=0><tr><td><combobox width=65 var=keyword list=\"Name;Ruler\"></td><td><edit var = \"Search\" width=130 height=11 length=\"16\"></td>");
	//TODO: search (Write in BBS)
	html.append("<td><button value=\"&$420;\" action=\"Write 5 -1 0 Search keyword keyword\" back=\"l2ui_ch3.smallbutton2_down\" width=65 height=20 fore=\"l2ui_ch3.smallbutton2\"> </td> </tr></table>");
	html.append("<br>");
	html.append("<br>");
	html.append("</center>");
	html.append("</body>");
	html.append("</html>");
	separateAndSend(html.toString(), activeChar);
}

/**
 * @param activeChar
 */
private void clanhome(L2PcInstance activeChar)
{
	clanhome(activeChar, activeChar.getClan().getClanId());
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final ClanBBSManager _instance = new ClanBBSManager();
}

/**
 * @param activeChar
 * @param clanId
 */
private void clanhome(L2PcInstance activeChar, int clanId)
{
	L2Clan cl = ClanTable.getInstance().getClan(clanId);
	if (cl != null)
	{
		if (cl.getLevel() < 2)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NO_CB_IN_MY_CLAN));
			parsecmd("_bbsclan_clanlist", activeChar);
		}
		else
		{
			final String html = StringUtil.concat("<html><body><center><br><br>"
					+ "<br1><br1><table border=0 cellspacing=0 cellpadding=0>"
					+ "<tr><td FIXWIDTH=15>&nbsp;</td>"
					+ "<td width=610 height=30 align=left>"
					+ "<a action=\"bypass _bbshome\">HOME</a> &gt; <a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a>  &gt; <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), "\"> &amp;$802; </a>"
					+ "</td></tr></table>"
					+ "<table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343>"
					+ "<tr><td height=10></td></tr>"
					+ "<tr>"
					+ "<td fixWIDTH=5></td>"
					+ "<td fixwidth=600>"
					+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";announce\">[CLAN ANNOUNCEMENT]</a> <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cbb\">[CLAN BULLETIN BOARD]</a>"
					+ "<a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cmail\">[CLAN MAIL]</a>&nbsp;&nbsp;"
					+ "<a action=\"bypass _bbsclan_clannotice_edit;", String.valueOf(clanId), ";cnotice\">[CLAN NOTICE]</a>&nbsp;&nbsp;"
					+ "</td>"
					+ "<td fixWIDTH=5></td>"
					+ "</tr>"
					+ "<tr><td height=10></td></tr>"
					+ "</table>"
					+ "<table border=0 cellspacing=0 cellpadding=0 width=610>"
					+ "<tr><td height=10></td></tr>"
					+ "<tr><td fixWIDTH=5></td>"
					+ "<td fixwidth=290 valign=top>"
					+ "</td>"
					+ "<td fixWIDTH=5></td>"
					+ "<td fixWIDTH=5 align=center valign=top><img src=\"l2ui.squaregray\" width=2  height=128></td>"
					+ "<td fixWIDTH=5></td>"
					+ "<td fixwidth=295>"
					+ "<table border=0 cellspacing=0 cellpadding=0 width=295>"
					+ "<tr>"
					+ "<td fixWIDTH=100 align=left>CLAN NAME</td>" + "<td fixWIDTH=195 align=left>", cl.getName(), "</td>" + "</tr>"
					+ "<tr><td height=7></td></tr>" + "<tr>" + "<td fixWIDTH=100 align=left>CLAN LEVEL</td>"
					+ "<td fixWIDTH=195 align=left height=16>", String.valueOf(cl.getLevel()), "</td>" + "</tr>"
					+ "<tr><td height=7></td></tr>" + "<tr>" + "<td fixWIDTH=100 align=left>CLAN MEMBERS</td>"
					+ "<td fixWIDTH=195 align=left height=16>", String.valueOf(cl.getMembersCount()), "</td>" + "</tr>"
					+ "<tr><td height=7></td></tr>" + "<tr>" + "<td fixWIDTH=100 align=left>CLAN LEADER</td>"
					+ "<td fixWIDTH=195 align=left height=16>", cl.getLeaderName(), "</td>" + "</tr>" + "<tr><td height=7></td></tr>"
					+
					//ADMINISTRATOR ??
					/*html.append("<tr>");
						 html.append("<td fixWIDTH=100 align=left>ADMINISTRATOR</td>");
						 html.append("<td fixWIDTH=195 align=left height=16>"+cl.getLeaderName()+"</td>");
						 html.append("</tr>");*/
					"<tr><td height=7></td></tr>" + "<tr>" + "<td fixWIDTH=100 align=left>ALLIANCE</td>"
					+ "<td fixWIDTH=195 align=left height=16>", (cl.getAllyName() != null) ? cl.getAllyName() : "", "</td>" + "</tr>"
							+ "</table>" + "</td>" + "<td fixWIDTH=5></td>"
							+ "</tr>"
							+ "<tr><td height=10></td></tr>"
							+ "</table>"
							+
							//TODO: the BB for clan :)
							//html.append("<table border=0 cellspacing=0 cellpadding=0 width=610  bgcolor=333333>");
							"<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">"
							+ "<img src=\"L2UI.squaregray\" width=\"610\" height=\"1\">" + "<br>" + "</center>" + "<br> <br>" + "</body>"
							+ "</html>");
			separateAndSend(html, activeChar);
		}
	}
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsewrite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
{
	// TODO Auto-generated method stub
	if (ar1.equals("Set"))
	{
		activeChar.getClan().setNotice(ar4);
		parsecmd("_bbsclan_clanhome;" + activeChar.getClan().getClanId(), activeChar);
	}
}

}