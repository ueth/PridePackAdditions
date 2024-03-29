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
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.PledgeInfo;

/**
 * This class ...
 *
 * @version $Revision: 1.5.4.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestPledgeInfo extends L2GameClientPacket
{
private static final String _C__66_REQUESTPLEDGEINFO = "[C] 66 RequestPledgeInfo";

private static Logger _log = Logger.getLogger(RequestPledgeInfo.class.getName());

private int _clanId;

@Override
protected void readImpl()
{
	_clanId = readD();
}

@Override
protected void runImpl()
{
	if (_log.isLoggable(Level.FINE))
		_log.log(Level.FINE, "Info for clan " + _clanId + " requested");
	
	if (_clanId == 101)
		return;
	
	L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	L2Clan clan = ClanTable.getInstance().getClan(_clanId);
	if (clan == null)
	{
		//_log.warning("Clan data for clanId " + _clanId + " is missing for player "+activeChar.getName());
		return; // we have no clan data ?!? should not happen
	}
	
	PledgeInfo pc = new PledgeInfo(clan);
	activeChar.sendPacket(pc);
	
}

/*
 * (non-Javadoc)
 *
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__66_REQUESTPLEDGEINFO;
}
}
