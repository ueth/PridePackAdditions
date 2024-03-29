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
package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;

/**
 * This class ...
 *
 * @version $Revision: 1.3.2.2.2.1 $ $Date: 2005/03/27 15:29:18 $
 */
public class CharNameTable
{
	private static Logger _log = Logger.getLogger(CharNameTable.class.getName());
	
	private CharNameTable()
	{
	}
	
	public static CharNameTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public synchronized boolean doesCharNameExist(String name)
	{
		boolean result = true;
		Connection con = null;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, name);
			ResultSet rset = statement.executeQuery();
			result = rset.next();
			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warning("could not check existing charname:" + e.getMessage());
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
		return result;
	}
	
	public int accountCharNumber(String account)
	{
		Connection con = null;
		int number = 0;
		
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT COUNT(char_name) FROM characters WHERE account_name=?");
			statement.setString(1, account);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				number = rset.getInt(1);
			}
			rset.close();
			statement.close();
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
		
		return number;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final CharNameTable _instance = new CharNameTable();
	}
}
