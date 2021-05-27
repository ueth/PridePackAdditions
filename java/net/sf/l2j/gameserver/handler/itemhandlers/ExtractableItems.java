package net.sf.l2j.gameserver.handler.itemhandlers;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.ExtractableItemsData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ExtractableItem;
import net.sf.l2j.gameserver.model.L2ExtractableProductItem;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.util.Rnd;

public class ExtractableItems implements IItemHandler
{
private static Logger _log = Logger.getLogger(ItemTable.class.getName());

public void useItem(L2Playable playable, L2ItemInstance item)
{
	if (!(playable instanceof L2PcInstance))
		return;
	
	final L2PcInstance activeChar = (L2PcInstance) playable;
	
	int itemID = item.getItemId();
	L2ExtractableItem exitem = ExtractableItemsData.getInstance().getExtractableItem(itemID);
	
	if (exitem == null)
		return;
	
	int rndNum = Rnd.get(1000000), chanceFrom = 0;
	int[] createItemID = new int[20];
	int[] createAmount = new int[20];
	
	// calculate extraction
	for (L2ExtractableProductItem expi : exitem.getProductItemsArray())
	{
		int chance = expi.getChance();
		
		if (rndNum >= chanceFrom && rndNum <= chance + chanceFrom)
		{
			createItemID = expi.getId();
			
			for (int i = 0; i < expi.getId().length; i++)
			{
				createItemID[i] = expi.getId()[i];
				
				if ((itemID >= 6411 && itemID <= 6518) || (itemID >= 7726 && itemID <= 7860) || (itemID >= 8403 && itemID <= 8483))
					createAmount[i] = (int)(expi.getAmmount()[i]* Config.RATE_EXTR_FISH);
				else
					createAmount[i] = expi.getAmmount()[i];
			}
			break;
		}
		
		chanceFrom += chance;
	}
	
	if (activeChar.destroyItemByItemId("Extract", itemID, 1, activeChar.getTarget(), true))
	{
		if (createItemID[0] <= 0 || createItemID.length == 0 )
		{
			System.out.println("Player: "+activeChar.getName()+" did not receive his/her item via extractable pack.");
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_INSIDE_THAT));
		}
		else
		{
			for (int i = 0; i < createItemID.length; i++)
			{
				if (createItemID[i] <= 0)
					continue;
				
				final L2ItemInstance dummyItem = ItemTable.getInstance().createDummyItem(createItemID[i]);
				
				if (dummyItem == null)
				{
					_log.warning("createItemID " + createItemID[i] + " doesn't have template!");
					activeChar.sendPacket(new SystemMessage(SystemMessageId.NOTHING_INSIDE_THAT));
					continue;
				}
				
				if (dummyItem.isStackable())
					activeChar.addItem("Extract", createItemID[i], createAmount[i], activeChar, true);
				else
				{
					final boolean enchantable = dummyItem.isEnchantable();
					
					for (int j = 0; j < createAmount[i]; j++)
					{
						int enchant = 0;
						
						if (enchantable)
						{
							if (item.getItem().getCrystalCount() > 0)
							{
								enchant = Rnd.get(Math.max(item.getItem().getReferencePrice(), 0), item.getItem().getCrystalCount());
							}
							
							assert (enchant >= 0 && enchant < 400);
						}
						
						activeChar.addItem("Extract", createItemID[i], 1, activeChar, true, enchant);
					}
				}
				activeChar.sendPacket(new PlaySound("Itemsound.quest_itemget"));
				//SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S); 
				SystemMessage sm2 = new SystemMessage(SystemMessageId.EARNED_ADENA);
				if (createItemID[i] == 57)
				{
					sm2.addNumber(createAmount[i]);
					activeChar.sendPacket(sm2);
				}
				/*else
				{
					sm.addItemName(createItemID[i]);
					if (createAmount[i] > 1)
						sm.addNumber(createAmount[i]);
					activeChar.sendPacket(sm);
				}*/
			}
		}
	}
	else
	{
		_log.severe(activeChar.getName()+ " TRIED TO USE A EXTRACTABLE ITEM: "+item.getItemName()+" but doesn't have it!!!!!!!");
		activeChar.setPunishLevel(L2PcInstance.PunishLevel.CHAR, 0);
	}
}
}