package net.sf.l2j.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.StringUtil;

/**
 * 
 * @author Meydex
 *
 */

public class RaidStatusBBSManager extends BaseBBSManager
{

protected static StringBuilder _raidInfoHTML = null;

public static RaidStatusBBSManager getInstance()
{
	return SingletonHolder._instance;
}

public void loadHTML()
{
	final StringBuilder HTML = StringUtil.startAppend(1000, "");
	
	HTML.append("<html><title>Raid Boss Status (Updated every 1 minute)</title><body><br><center><table width=700 border=0 bgcolor=333333>");
	
	HTML.append("<tr>");
	
	HTML.append("<td align=center><font color=\"LEVEL\">Boss Name</font></td>");
	HTML.append("<td align=center><font color=\"LEVEL\">Boss Difficulty</font></td>");
	HTML.append("<td align=center><font color=\"LEVEL\">Respawn Date</font></td>");
	
	HTML.append("</tr>");
	
	PreparedStatement statement;
	ResultSet rs;
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();		
		
		statement = con.prepareStatement("SELECT * FROM raidboss_spawnlist ORDER BY respawn_time DESC LIMIT 0, 25");		
		
		rs = statement.executeQuery();
		while (rs.next())
		{
			L2NpcTemplate boss = NpcTable.getInstance().getTemplate(rs.getInt("boss_id"));
			String bossname = boss.getName();
			String difficulty = boss.getDifficulty();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(rs.getLong("respawn_time"));
			int mYear = calendar.get(Calendar.YEAR);
			int mMonth = calendar.get(Calendar.MONTH) + 1;
			int mDay = calendar.get(Calendar.DAY_OF_MONTH);
			int mHour = calendar.get(Calendar.HOUR_OF_DAY);
			int mMinute = calendar.get(Calendar.MINUTE);
			
			String status = rs.getLong("respawn_time") > 0 ? String.valueOf(mDay) + "/" + String.valueOf(mMonth) + "/" + String.valueOf(mYear) + " " + String.valueOf(mHour) + ":" + String.valueOf(mMinute) : "<font color=\"00FF00\">Alive</font>";
			
			HTML.append("<tr>");
			HTML.append("<td align=center>"+bossname+"</td>");
			HTML.append("<td align=center>"+difficulty+"</td>");
			HTML.append("<td align=center>"+status+"</td>");
			HTML.append("</tr>");
			
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
	_raidInfoHTML = HTML;
}

//Method to update every x minutes regardless of the status!
private RaidStatusBBSManager()
{
	ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new Runnable() {
		public void run()
		{
			RaidStatusBBSManager.getInstance().loadHTML();
		}
	}, 0, 60000);
}


/**
 * 
 * @see net.sf.l2j.gameserver.communitybbs.Manager.BaseBBSManager#parsecmd(java.lang.String, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
@Override
public void parsecmd(String command, L2PcInstance activeChar)
{
	if (command.startsWith("_bbsraid"))
	{
		String html = _raidInfoHTML.toString();	
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
protected static final RaidStatusBBSManager _instance = new RaidStatusBBSManager();
}
}