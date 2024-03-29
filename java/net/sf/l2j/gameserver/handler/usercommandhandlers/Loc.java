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
package net.sf.l2j.gameserver.handler.usercommandhandlers;

import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

/**
 *
 *
 */
public class Loc implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		0
	};
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
	 */
	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		int _nearestTown = MapRegionTable.getInstance().getClosestTownNumber(activeChar);
		SystemMessageId msg;
		switch (_nearestTown)
		{
			case 0:
				msg = SystemMessageId.LOC_TI_S1_S2_S3;
				break;
			case 1:
				msg = SystemMessageId.LOC_ELVEN_S1_S2_S3;
				break;
			case 2:
				msg = SystemMessageId.LOC_DARK_ELVEN_S1_S2_S3;
				break;
			case 3:
				msg = SystemMessageId.LOC_ORC_S1_S2_S3;
				break;
			case 4:
				msg = SystemMessageId.LOC_DWARVEN_S1_S2_S3;
				break;
			case 5:
				msg = SystemMessageId.LOC_GLUDIO_S1_S2_S3;
				break;
			case 6:
				msg = SystemMessageId.LOC_GLUDIN_S1_S2_S3;
				break;
			case 7:
				msg = SystemMessageId.LOC_DION_S1_S2_S3;
				break;
			case 8:
				msg = SystemMessageId.LOC_GIRAN_S1_S2_S3;
				break;
			case 9:
				msg = SystemMessageId.LOC_OREN_S1_S2_S3;
				break;
			case 10:
				msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
				break;
			case 11:
				msg = SystemMessageId.LOC_HUNTER_S1_S2_S3;
				break;
			case 12:
				msg = SystemMessageId.LOC_GIRAN_HARBOR_S1_S2_S3;
				break;
			case 13:
				msg = SystemMessageId.LOC_HEINE_S1_S2_S3;
				break;
			case 14:
				msg = SystemMessageId.LOC_RUNE_S1_S2_S3;
				break;
			case 15:
				msg = SystemMessageId.LOC_GODDARD_S1_S2_S3;
				break;
			case 16:
				msg = SystemMessageId.LOC_SCHUTTGART_S1_S2_S3;
				break;
			case 17:
				msg = SystemMessageId.LOC_FLORAN_S1_S2_S3;
				break;
			case 18:
				msg = SystemMessageId.LOC_PRIMEVAL_ISLE_S1_S2_S3;
				break;
			case 19:
				msg = SystemMessageId.LOC_KAMAEL_VILLAGE_S1_S2_S3;
				break;
			case 20:
				msg = SystemMessageId.LOC_WASTELANDS_CAMP_S1_S2_S3;
				break;
			case 21:
				msg = SystemMessageId.LOC_FANTASY_ISLAND_S1_S2_S3;
				break;
			case 22:
				msg = SystemMessageId.LOC_NETRAL_ZONE_S1_S2_S3;
				break;
			case 23:
				msg = SystemMessageId.LOC_COLISEUM_S1_S2_S3;
				break;
			case 24:
				msg = SystemMessageId.LOC_GM_CONSULATION_SERVICE_S1_S2_S3;
				break;
			case 25:
				msg = SystemMessageId.LOC_DIMENSIONAL_GAP_S1_S2_S3;
				break;
			case 26:
				msg = SystemMessageId.LOC_CEMETARY_OF_THE_EMPIRE_S1_S2_S3;
				break;
			case 27:
				msg = SystemMessageId.LOC_STEEL_CITADEL_S1_S2_S3;
				break;
			case 28:
				msg = SystemMessageId.LOC_STEEL_CITADEL_RESISTANCE;
				break;
			case 29:
				msg = SystemMessageId.LOC_KAMALOKA;
				break;
			case 30:
				msg = SystemMessageId.LOC_NIA_KAMALOKA;
				break;
			case 31:
				msg = SystemMessageId.LOC_RIM_KAMALOKA;
				break;
			case 32:
				msg = SystemMessageId.LOC_KEUCEREUS_S1_S2_S3;
				break;
			case 33:
				msg = SystemMessageId.LOC_IN_SEED_INFINITY_S1_S2_S3;
				break;
			case 34:
				msg = SystemMessageId.LOC_OUT_SEED_INFINITY_S1_S2_S3;
				break;
			case 35:
				msg = SystemMessageId.LOC_CLEFT_S1_S2_S3;
				break;
			default:
				msg = SystemMessageId.LOC_ADEN_S1_S2_S3;
		}
		SystemMessage sm = new SystemMessage(msg);
		sm.addNumber(activeChar.getX());
		sm.addNumber(activeChar.getY());
		sm.addNumber(activeChar.getZ());
		activeChar.sendPacket(sm);
		return true;
	}
	
	/**
	 * 
	 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
	 */
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}
