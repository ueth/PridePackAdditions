/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
+ * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
+ * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;


public class IconTable
{
	private static final Logger _log = Logger.getLogger(IconTable.class.getName());
	
	private static Map<Integer, String> _icons;
	
	public static final IconTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	protected IconTable()
	{
		_icons = new FastMap<>();
		load();
	}
	
	public void reload()
	{
		_icons.clear();
		load();
	}
	
	private void load()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			final PreparedStatement stmt = con.prepareStatement("SELECT * FROM icons");
			final ResultSet rset = stmt.executeQuery();
			int id;
			String value;
			
			while (rset.next())
			{
				id = rset.getInt("itemId");
				value = rset.getString("iconName");
				_icons.put(id, value);
			}
			rset.close();
			stmt.close();
			
		}
		catch (SQLException e)
		{
			_log.log(Level.WARNING, "IconTable: Error loading from database:" + e.getMessage(), e);
		}
		finally
		{
			try { con.close(); } catch (Exception e) {}
		}
		
		_log.info("IconTable: Loaded " + _icons.size() + " icons.");
	}
	
	public String getIcon(int id)
	{
		if (_icons.get(id) == null)
			return "icon.NOIMAGE";
		
		return _icons.get(id);
	}
	
	private static class SingletonHolder
	{
		protected static final IconTable _instance = new IconTable();
	}
}