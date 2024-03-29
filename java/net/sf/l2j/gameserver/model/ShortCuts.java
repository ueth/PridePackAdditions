package net.sf.l2j.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.1.2.3 $ $Date: 2005/03/27 15:29:33 $
 */
public class ShortCuts
{
private static Logger _log = Logger.getLogger(ShortCuts.class.getName());

private L2PcInstance _owner;
private Map<Integer, L2ShortCut> _shortCuts = new TreeMap<Integer, L2ShortCut>();

public ShortCuts(L2PcInstance owner)
{
	_owner = owner;
}

public L2ShortCut[] getAllShortCuts()
{
	return _shortCuts.values().toArray(new L2ShortCut[_shortCuts.values().size()]);
}

public L2ShortCut getShortCut(int slot, int page)
{
	L2ShortCut sc = _shortCuts.get(slot + page * 12);
	
	// verify shortcut
	if (sc != null && sc.getType() == L2ShortCut.TYPE_ITEM)
	{
		if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
		{
			deleteShortCut(sc.getSlot(), sc.getPage());
			sc = null;
		}
	}
	
	return sc;
}

public synchronized void registerShortCut(L2ShortCut shortcut)
{
	L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
	registerShortCutInDb(shortcut, oldShortCut);
}

public synchronized void registerShortCut(L2ShortCut shortcut, boolean storeToDb)
{
	L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
	
	if(storeToDb)
		registerShortCutInDb(shortcut, oldShortCut);
}

private void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut)
{
	if(_owner.isInFairGame())
		return;
	if (oldShortCut != null)
		deleteShortCutFromDb(oldShortCut);
	
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (charId,slot,page,type,shortcut_id,level,class_index) values(?,?,?,?,?,?,?)");
		statement.setInt(1, _owner.getObjectId());
		statement.setInt(2, shortcut.getSlot());
		statement.setInt(3, shortcut.getPage());
		statement.setInt(4, shortcut.getType());
		statement.setInt(5, shortcut.getId());
		statement.setInt(6, shortcut.getLevel());
		statement.setInt(7, _owner.getClassIndex());
		statement.execute();
		statement.close();
	}
	catch (Exception e)
	{
		_log.warning("Could not store character shortcut: " + e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

/**
 * @param slot
 */
public synchronized void deleteShortCut(int slot, int page)
{
	deleteShortCut(slot, page, true);
}
public synchronized void deleteShortCut(int slot, int page, boolean fromDb)
{
	L2ShortCut old = _shortCuts.remove(slot+page*12);
	
	if (old == null || _owner == null)
		return;
	if(fromDb)
		deleteShortCutFromDb(old);
	
	_owner.sendPacket(new ShortCutInit(_owner));
}

public synchronized void deleteShortCutByObjectId(int objectId)
{
	L2ShortCut toRemove = null;
	
	for (L2ShortCut shortcut : _shortCuts.values())
	{
		if (shortcut.getType() == L2ShortCut.TYPE_ITEM && shortcut.getId() == objectId)
		{
			toRemove = shortcut;
			break;
		}
	}
	
	if (toRemove != null)
		deleteShortCut(toRemove.getSlot(), toRemove.getPage());
}

/**
 * @param shortcut
 */
private void deleteShortCutFromDb(L2ShortCut shortcut)
{
	if(_owner.isInFairGame())
		return;
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND slot=? AND page=? AND class_index=?");
		statement.setInt(1, _owner.getObjectId());
		statement.setInt(2, shortcut.getSlot());
		statement.setInt(3, shortcut.getPage());
		statement.setInt(4, _owner.getClassIndex());
		statement.execute();
		statement.close();
	}
	catch (Exception e)
	{
		_log.warning("Could not delete character shortcut: " + e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

public void restore()
{
	_shortCuts.clear();
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT charId, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE charId=? AND class_index=?");
		statement.setInt(1, _owner.getObjectId());
		statement.setInt(2, _owner.getClassIndex());
		
		ResultSet rset = statement.executeQuery();
		
		while (rset.next())
		{
			int slot = rset.getInt("slot");
			int page = rset.getInt("page");
			int type = rset.getInt("type");
			int id = rset.getInt("shortcut_id");
			int level = rset.getInt("level");
			
			L2ShortCut sc = new L2ShortCut(slot, page, type, id, level, 1);
			_shortCuts.put(slot+page*12, sc);
		}
		
		rset.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.warning("Could not restore character shortcuts: " + e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
	
	// verify shortcuts
	for (L2ShortCut sc : getAllShortCuts())
	{
		if (sc.getType() == L2ShortCut.TYPE_ITEM)
		{
			if (_owner.getInventory().getItemByObjectId(sc.getId()) == null)
				deleteShortCut(sc.getSlot(), sc.getPage());
		}
	}
}

public synchronized void tempRemoveAll()
{
	_shortCuts.clear();
}
}
