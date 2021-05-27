package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.templates.item.L2Item;

public final class RequestRefine extends AbstractRefinePacket
{
private static final String _C__D0_2C_REQUESTREFINE = "[C] D0:2C RequestRefine";
private int _targetItemObjId;
private int _refinerItemObjId;

@Override
protected void readImpl()
{
	_targetItemObjId = readD();
	_refinerItemObjId = readD();
	readD();
	readQ();
}

@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	if (activeChar == null)
		return;
	
	if (activeChar.isAccountLockedDown())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	L2ItemInstance targetItem = (L2ItemInstance)L2World.getInstance().findObject(_targetItemObjId);
	if (targetItem == null)
		return;
	L2ItemInstance refinerItem = (L2ItemInstance)L2World.getInstance().findObject(_refinerItemObjId);
	if (refinerItem == null)
		return;
	
	if (!isValid(activeChar, targetItem, refinerItem))
	{
		activeChar.sendPacket(new ExVariationResult(0,0,0));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS));
		return;
	}
	
	final LifeStone ls = getLifeStone(refinerItem.getItemId());
	
	if (ls == null)
		return;
	
	int lifeStoneLevel = ls.getLevel();
	int lifeStoneGrade = ls.getGrade();
	
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
	}
	
	// consume the life stone
	if (!activeChar.destroyItem("RequestRefine", refinerItem, 1, null, false))
		return;
	
	if (!refinerItem.isTradeable() && refinerItem.getUntradeableTime() > System.currentTimeMillis())
	{
		if (targetItem.getUntradeableTime() < refinerItem.getUntradeableTime())
		{
			targetItem.setUntradeableTimer(refinerItem.getUntradeableTime());
			activeChar.sendMessage("Your "+targetItem.getName()+" is now untradeable for "+refinerItem.getUntradeableTime()/3600000+"" +
			" hours due to your life stone being temporarily untradeable");
		}
	}
	
	boolean doYouHavePride = false;
	
	if (targetItem.isJewelry()) //jewel
	{
		switch (targetItem.getItemId())
		{
		case 9455:
		case 9456:
		case 9457:
		case 9458: //pride jewels
		case 9460:
		case 14163:
		case 14164:
		case 14165:
			doYouHavePride = true;
		}
	}
	else if (targetItem.getItem().getBodyPart() == L2Item.SLOT_BELT) //belt
		doYouHavePride = true;
	
	final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(lifeStoneLevel, lifeStoneGrade, targetItem.getItem().getBodyPart(), doYouHavePride);
	targetItem.setAugmentation(aug);
	
	final int stat12 = 0x0000FFFF & aug.getAugmentationId();
	final int stat34 = aug.getAugmentationId() >> 16;
		activeChar.sendPacket(new ExVariationResult(stat12,stat34,1));
		activeChar.sendPacket(new SystemMessage(SystemMessageId.THE_ITEM_WAS_SUCCESSFULLY_AUGMENTED));
		
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		activeChar.sendPacket(iu);
}

@Override
public String getType()
{
	return _C__D0_2C_REQUESTREFINE;
}
}