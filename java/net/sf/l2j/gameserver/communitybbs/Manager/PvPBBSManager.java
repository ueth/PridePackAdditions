package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class PvPBBSManager extends BaseBBSManager
{
protected static StringBuilder _pvpHTML = null;
protected static StringBuilder _fameHTML = null;
protected static StringBuilder _pkHTML = null;

private static final String LOAD_PVP_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY pvpkills DESC LIMIT 0, 25";
private static final String LOAD_FAME_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY fame DESC LIMIT 0, 25";
private static final String LOAD_PK_HTML = "SELECT * FROM characters WHERE accesslevel=0 ORDER BY pkkills DESC LIMIT 0, 25";

public static PvPBBSManager getInstance()
{
	return SingletonHolder._instance;
}

public void loadHTML(int whichone)
{
	final StringBuilder HTML = StringUtil.startAppend(1000, "");
	
	String info = null;
	switch (whichone)
	{
	default:
		info = "Top PvPers of the server (updated every 5 minutes)";
		break;
	case 1:
		info = "Most famous people of the server (updated every 5 minutes)";
		break;
	case 2:
		info = "The most \"hardcore\" players (updated every 5 minutes)";
		break;
	}
	
	HTML.append("<html><title>"+info+"</title><body><br><center><table width=\"100%\">");
	
	HTML.append("<tr>");
	
	HTML.append("<td><font color=\"LEVEL\">Player Name</font></td>");
	HTML.append("<td><font color=\"LEVEL\">Player Title</font></td>");
	HTML.append("<td><font color=\"LEVEL\">Base Class</font></td>");
	HTML.append("<td><font color=\"LEVEL\">Clan</font></td>");
	HTML.append("<td><font color=\"LEVEL\">%Fame%</font></td>");
	HTML.append("<td><font color=\"LEVEL\">%PvP%</font></td>");
	HTML.append("<td><font color=\"LEVEL\">%PK%</font></td>");
	
	HTML.append("</tr>");
	
	PreparedStatement statement;
	ResultSet rs;
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		switch (whichone)
		{
		default:
			statement = con.prepareStatement(LOAD_PVP_HTML);
			break;
		case 1:
			statement = con.prepareStatement(LOAD_FAME_HTML);
			break;
		case 2:
			statement = con.prepareStatement(LOAD_PK_HTML);
			break;
		}
		
		boolean lol = true;
		String color = "FFF8C6";
		
		rs = statement.executeQuery();
		
		while (rs.next())
		{
			String name = rs.getString("char_name");
			String title = rs.getString("title");
			if (title == null)
				title = "";
			title = title.replaceAll("<", "&lt;");
			title = title.replaceAll(">", "&gt;");
			String fame = String.valueOf(rs.getInt("fame"));
			String pvps = String.valueOf(rs.getInt("pvpkills"));
			String pks = String.valueOf(rs.getInt("pkkills"));
			L2Clan clan = ClanTable.getInstance().getClan(rs.getInt("clanid"));
			String clanname = "-";
			if (clan != null)
				clanname = clan.getName()+ " (Lvl "+clan.getLevel()+")";
			
			String baseclass = CharTemplateTable.getInstance().getClassNameById(rs.getInt("base_class"));
			
			if(rs.getBoolean("online"))
				name = "<a action=\"bypass _bbsloc;playerinfo;" +name+ "\">"+name+"</a>";
			
			HTML.append("<tr>");
			HTML.append("<td><font color="+color+">"+name+"</font></td>");
			HTML.append("<td><font color="+color+">"+title+"</font></td>");
			HTML.append("<td><font color="+color+">"+baseclass+"</font></td>");
			HTML.append("<td><font color="+color+">"+clanname+"</font></td>");
			HTML.append("<td><font color="+color+">"+fame+"</font></td>");
			HTML.append("<td><font color="+color+">"+pvps+"</font></td>");
			HTML.append("<td><font color="+color+">"+pks+"</font></td>");
			HTML.append("</tr>");
			
			if (lol)
			{
				lol = false;
				color = "817679";
			}
			else
			{
				lol = true;
				color = "FFF8C6";
			}
		}
		
		rs.close();
		statement.close();
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
		
		HTML.append("</table></center><br></body></html>");
	}
	
	switch (whichone)
	{
	default:
		_pvpHTML = HTML;
		break;
	case 1:
		_fameHTML = HTML;
		break;
	case 2:
		_pkHTML = HTML;
		break;
	}
}

private PvPBBSManager()
{
	ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
		public void run()
		{
			PvPBBSManager.getInstance().loadHTML(0);
			PvPBBSManager.getInstance().loadHTML(1);
			PvPBBSManager.getInstance().loadHTML(2);
		}
	}, 0, 6*60*1000);
}

/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	if (command.startsWith("_bbspvp"))
	{
		String html = _pvpHTML.toString();
		html = html.replace("%PvP%", "PvP");
		html = html.replace("%Fame%", "<a action=\"bypass _bbsfame\">Fame</a>");
		html = html.replace("%PK%", "<a action=\"bypass _bbspk\">PK</a>");
		
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbsfame") || command.startsWith("_bbsgetfav"))
	{
		String html = _fameHTML.toString();
		html = html.replace("%PvP%", "<a action=\"bypass _bbspvp\">PvP</a>");
		html = html.replace("%Fame%", "Fame");
		html = html.replace("%PK%", "<a action=\"bypass _bbspk\">PK</a>");
		
		separateAndSend(html, activeChar);
	}
	else if (command.startsWith("_bbspk"))
	{
		String html = _pkHTML.toString();
		html = html.replace("%PvP%", "<a action=\"bypass _bbspvp\">PvP</a>");
		html = html.replace("%Fame%", "<a action=\"bypass _bbsfame\">Fame</a>");
		html = html.replace("%PK%", "PK");
		
		separateAndSend(html, activeChar);
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
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final PvPBBSManager _instance = new PvPBBSManager();
}
}