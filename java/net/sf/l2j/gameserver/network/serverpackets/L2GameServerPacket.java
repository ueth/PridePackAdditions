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
package net.sf.l2j.gameserver.network.serverpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.network.L2GameClient;

import org.mmocore.network.SendablePacket;

/**
 *
 * @author  KenM
 */
public abstract class L2GameServerPacket extends SendablePacket<L2GameClient>
{
private static final Logger _log = Logger.getLogger(L2GameServerPacket.class.getName());

protected boolean _invisible = false;

/**
 * 
 * @return True if packet originated from invisible character.
 */
public boolean isInvisible()
{
	return _invisible;
}

/**
 * Set "invisible" boolean flag in the packet.
 * Packets from invisible characters will not be broadcasted to players.
 * @param b
 */
public void setInvisible(boolean b)
{
	_invisible = b;
}

/**
 * @see com.l2jserver.mmocore.network.SendablePacket#write()
 */
@Override
protected void write()
{
	try
	{
		writeImpl();
	}
	catch (Exception e)
	{
		final String type = getType();
		
		if (type == "[S] 4e PartySmallWindowAll" || type == "[S] 4f PartySmallWindowAdd")
		{}
		else
		{
			_log.severe("Client: "+getClient().toString()+" - Failed writing: "+type+" - L2J Server Version: "+Config.SERVER_VERSION+" - DP Revision: "+Config.DATAPACK_VERSION);
			e.printStackTrace();
		}
	}
}

public void runImpl()
{
	
}

protected abstract void writeImpl();

/**
 * @return A String with this packet name for debuging purposes
 */
public abstract String getType();
}
