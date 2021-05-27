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

import net.sf.l2j.gameserver.model.actor.instance.L2FenceInstance;

/**
 * Format: (ch)ddddddd
 * d: object id
 * d: type (00 - no fence, 01 - only 4 columns, 02 - columns with fences)
 * d: x coord
 * d: y coord
 * d: z coord
 * d: width
 * d: height
 */
public class ExColosseumFenceInfoPacket extends L2GameServerPacket
{
private static final String _S__FE_03_EXCOLOSSEUMFENCEINFOPACKET = "[S] FE:03 ExColosseumFenceInfoPacket";
private L2FenceInstance _fence;

public ExColosseumFenceInfoPacket(L2FenceInstance fence)
{
	_fence = fence;
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.serverpackets.ServerBasePacket#writeImpl()
 */
@Override
protected void writeImpl()
{
	writeC(0xfe);
	writeH(0x03);
	
	writeD(_fence.getObjectId());
	writeD(_fence.getType());
	writeD(_fence.getX());
	writeD(_fence.getY());
	writeD(_fence.getZ());
	writeD(_fence.getWidth());
	writeD(_fence.getLength());
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _S__FE_03_EXCOLOSSEUMFENCEINFOPACKET;
}
}