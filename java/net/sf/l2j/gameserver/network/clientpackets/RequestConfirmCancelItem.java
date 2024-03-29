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

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExPutItemResultForVariationCancel;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * Format(ch) d
 * @author  -Wooden-
 */
public final class RequestConfirmCancelItem extends L2GameClientPacket
{
private static final String _C__D0_2D_REQUESTCONFIRMCANCELITEM = "[C] D0:2D RequestConfirmCancelItem";
private int _itemId;

/**
 * @param buf
 * @param client
 */
@Override
protected void readImpl()
{
	_itemId = readD();
}

/**
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
 */
@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	final L2ItemInstance item = (L2ItemInstance)L2World.getInstance().findObject(_itemId);
	if (activeChar == null || item == null)
		return;
	
	if (item.getOwnerId() != activeChar.getObjectId())
	{
		Util.handleIllegalPlayerAction(getClient().getActiveChar(),"Warning!! Character "+getClient().getActiveChar().getName()+" of account "+getClient().getActiveChar().getAccountName()+" tryied to destroy augment on item that doesn't own.",Config.DEFAULT_PUNISH);
		return;
	}
	
	if (!item.isAugmented())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
		return;
	}
	
	int price=0;
	switch (item.getItem().getCrystalType())
	{
	case L2Item.CRYSTAL_C:
		if (item.getCrystalCount() < 1720)
			price = 95000;
		else if (item.getCrystalCount() < 2452)
			price = 150000;
		else
			price = 210000;
		break;
	case L2Item.CRYSTAL_B:
		if (item.getCrystalCount() < 1746)
			price = 240000;
		else
			price = 270000;
		break;
	case L2Item.CRYSTAL_A:
		if (item.getCrystalCount() < 2160)
			price = 330000;
		else if (item.getCrystalCount() < 2824)
			price = 390000;
		else
			price = 420000;
		break;
	case L2Item.CRYSTAL_S:
		price = 480000;
		break;
	case L2Item.CRYSTAL_S80:
	case L2Item.CRYSTAL_S84:
		price = 920000;
		break;
		// any other item type is not augmentable
	default:
		if (item.getItemId() == 20325)
		{
			price = 480000;
			break;
		}
		return;
	}
	
	activeChar.sendPacket(new ExPutItemResultForVariationCancel(_itemId, price));
}

/**
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _C__D0_2D_REQUESTCONFIRMCANCELITEM;
}
}
