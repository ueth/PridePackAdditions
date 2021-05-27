package net.sf.l2j.gameserver.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.lib.Log;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

public class GMAudit
{
private static final Logger _log = Logger.getLogger(Log.class.getName());
public static final SimpleDateFormat _formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");

public static void auditGMAction(L2PcInstance gm, String action, String param)
{
	String gm_name = gm.getAccountName() + " - " + gm.getName();
	String target = "null";
	
	L2Object targetChar = gm.getTarget();
	if (targetChar != null)
		target = targetChar.getName() + " - " + targetChar.getObjectId();
	
	auditGMAction(gm_name, action, target, param);
}

public static void auditGMAction(String gm_name, String action, String target, String param)
{
	String today = _formatter.format(new Date());
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("INSERT INTO audit_gm(gm_name, action, target, param, date) VALUES(?,?,?,?,?)");
		
		statement.setString(1, gm_name);
		statement.setString(2, action);
		statement.setString(3, target);
		statement.setString(4, param);
		statement.setString(5, today);
		
		statement.executeUpdate();
	}
	catch (Exception e)
	{
		_log.severe(e.getMessage());
	}
	finally { try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); } }
}
}