package net.sf.l2j.gameserver.model.actor.instance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.StringTokenizer;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.GMAudit;

public class L2DonationMerchantInstance extends L2MerchantInstance
{
public L2DonationMerchantInstance(int objectId, L2NpcTemplate template)
{
	super(objectId, template);
}

@Override
public void onBypassFeedback(L2PcInstance player, String command)
{
	final StringTokenizer st = new StringTokenizer(command, " ");
	final String actualCommand = st.nextToken(); // Get actual command
	
	if (actualCommand.equalsIgnoreCase("donation_take"))
	{
		if (st.hasMoreTokens())
		{
			String transaction_id = st.nextToken();
			
			if (transaction_id.startsWith("#"))
			{
				transaction_id = transaction_id.substring(1);
			}
			
			if (transaction_id.length() != 17)
			{
				sendErrorPage(player, 1);
				return;
			}
			
			transaction_id = transaction_id.toUpperCase();
			
			if (!retrieveDonation(transaction_id, player))
			{
				//send fail page
				return;
			}
			else
			{
				//send ty page - handled by retrieveDonation method
				return;
			}
		}
		
		sendErrorPage(player, 2);
		return;
	}
	else
	{
		super.onBypassFeedback(player, command);
	}
}

private static void sendErrorPage(L2PcInstance player, int code)
{
	String filename = "99999-8.htm";
	
	switch (code)
	{
	case 1:
		filename = "99999-8.htm"; //general error
		break;
	case 2:
		filename = "99999-2.htm"; //enter transaction #
		break;
	case 3:
		filename = "99999-9.htm"; //already retrieved
		break;
	case 5:
		filename = "99999-6.htm"; //thank you
		break;
	}
	
	String content = HtmCache.getInstance().getHtmForce("data/html/merchant/" + filename);
	NpcHtmlMessage tele = new NpcHtmlMessage(1);
	tele.setHtml(content);
	player.sendPacket(tele);
}

private synchronized static final boolean retrieveDonation(String txn_id, L2PcInstance player)
{
	if (txn_id == null || player == null)
	{
		return false;
	}
	
	System.out.println(txn_id);
	
	int payment_amount = 0;
	String payment_status = null;
	boolean retrieved = true;
	
	boolean exit = false;
	
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT * FROM donations WHERE txn_id=?");
		statement.setString(1, txn_id);
		ResultSet rset = statement.executeQuery();
		
		if (rset.next())
		{
			payment_amount = rset.getInt("payment_amount");
			System.out.println("Payment Amount: "+payment_amount);
			
			if (payment_amount < 1)
				exit = true;
			else if (payment_amount > 15000)
				exit = true;
			
			payment_status = rset.getString("payment_status");
			System.out.println("Payment Status: "+payment_status);
			
			if (!payment_status.equals("Completed"))
				exit = true;
			
			retrieved = rset.getBoolean("retrieved");
			System.out.println("Has this donation been retrieved before? "+retrieved);
			
			if (retrieved)
				exit = true;
		}
		else
		{
			exit = true;
			retrieved = false;
		}
		
		rset.close();
		statement.close();
		
		if (exit)
		{
			if (retrieved)
				sendErrorPage(player, 3);
			else
				sendErrorPage(player, 1);
			
			con.close();
			return false;
		}
		
		try
		{
			String today = GMAudit._formatter.format(new Date());
			
			statement = con.prepareStatement("UPDATE donations set retrieved=1, retriever_ip=?, retriever_acct=?, retriever_char=?, retrieval_date=? WHERE txn_id=?");
			statement.setString(1, player.getIP());
			statement.setString(2, player.getAccountName());
			statement.setString(3, player.getName());
			statement.setString(4, today);
			statement.setString(5, txn_id);
			
			statement.execute();
		}
		catch (Exception e)
		{
			_log.warning("Could not update retreival from false to true on id: "+txn_id+ " with player name: " +player.getName()+ " " + e.getMessage());
			exit = true;
		}
		finally
		{
			statement.close();
		}
		
		if (exit)
		{
			con.close();
			return false;
		}
		
		if (payment_amount >= 7000)
		{
			_log.severe(player.getName() + " has initiated a donation retrieval of more than 700 euros with id "+txn_id);
		}
		
		_log.config(player.getName() + " has retrieved a donation of "+payment_amount+" tokens with id "+txn_id);
		
		try
		{
			player.addItem("donation_token", L2Item.DONATION_TOKEN, payment_amount, player, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			exit = true;
		}
		
		if (exit)
		{
			con.close();
			return false;
		}
		
		String content = HtmCache.getInstance().getHtmForce("data/html/merchant/99999-6.htm");
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		tele.replace("!fdas!", String.valueOf(payment_amount));
		player.sendPacket(tele);
		
		return true;
	}
	catch (SQLException e)
	{
		_log.warning("could not check existing char number:" + e.getMessage());
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
	}
	
	return false;
}
}