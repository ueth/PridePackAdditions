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
import net.sf.l2j.gameserver.network.serverpackets.ExVariationCancelResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.Util;

/**
 * Format(ch) d
 * @author  -Wooden-
 */
public final class RequestRefineCancel extends L2GameClientPacket
{
private static final String _C__D0_2E_REQUESTREFINECANCEL = "[C] D0:2E RequestRefineCancel";
private int _targetItemObjId;

@Override
protected void readImpl()
{
	_targetItemObjId = readD();
}

/**
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#runImpl()
 */
@SuppressWarnings("unused")
@Override
protected void runImpl()
{
	L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	if (activeChar.isAccountLockedDown())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
	
	if (activeChar == null) return;
	if (targetItem == null)
	{
		activeChar.sendPacket(new ExVariationCancelResult(0));
		return;
	}
	if (targetItem.getOwnerId() != activeChar.getObjectId())
	{
		Util.handleIllegalPlayerAction(getClient().getActiveChar(),"Warning!! Character "+getClient().getActiveChar().getName()+" of account "+getClient().getActiveChar().getAccountName()+" tryied to augment item that doesn't own.",Config.DEFAULT_PUNISH);
		return;
	}
	// cannot remove augmentation from a not augmented item
	if (!targetItem.isAugmented())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM));
		activeChar.sendPacket(new ExVariationCancelResult(0));
		return;
	}
	
	switch (targetItem.getItem().getItemGradeSPlus())
	{
	case L2Item.CRYSTAL_C:
		break;
	case L2Item.CRYSTAL_B:
		break;
	case L2Item.CRYSTAL_A:
		break;
	case L2Item.CRYSTAL_S:
		break;
	default:
		if (targetItem.getItemId() == 20325)
			break;
		activeChar.sendPacket(new ExVariationCancelResult(0));
		return;
	}
	
	// unequip item
	if (targetItem.isEquipped())
	{
		activeChar.abortAttack();
		activeChar.abortCast();
		
		L2ItemInstance[] unequiped = activeChar.getInventory().unEquipItemInBodySlotAndRecord(activeChar.getInventory().getSlotFromItem(targetItem));
		InventoryUpdate iu = new InventoryUpdate();
		
		for (L2ItemInstance element : unequiped)
			iu.addModifiedItem(element);
		
		activeChar.sendPacket(iu);
		activeChar.broadcastUserInfo();
		
		ItemList il = new ItemList(activeChar, true);
		activeChar.sendPacket(il);
	}
	
	// remove the augmentation
	targetItem.removeAugmentation();
	
	// send ExVariationCancelResult
	activeChar.sendPacket(new ExVariationCancelResult(1));
	
	// send inventory update
	InventoryUpdate iu = new InventoryUpdate();
	iu.addModifiedItem(targetItem);
	activeChar.sendPacket(iu);
	
	// send system message
	SystemMessage sm = new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
	sm.addString(targetItem.getItemName());
	activeChar.sendPacket(sm);
}

/**
 * @see net.sf.l2j.gameserver.BasePacket#getType()
 */
@Override
public String getType()
{
	return _C__D0_2E_REQUESTREFINECANCEL;
}

}
