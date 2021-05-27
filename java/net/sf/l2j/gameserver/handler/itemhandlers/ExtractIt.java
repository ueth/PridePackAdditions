package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2Augmentation;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExVariationResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.util.Rnd;

public class ExtractIt implements IItemHandler
{
	private static final int _sRareWeapIds[] = { 80000,80001,80002,80003,80004,80005,80006,80007,80008,80009,80010,80011,80012,80013,80014,80016,80017 };
	private static final int _s80RareWeapIds[] = { 81001,81002,81003,81004,81005,81006,81007,81008,81009,81010,81011,81012,81013,81014,81015,81017 };
	private static final int _sEpicWeapIds[] = { 80100,80101,80102,80103,80104,80105,80106,80107,80108,80109,80110,80111,80112,80113,80114,80116,80117 };
	private static final int	_gemIds[]		=
												{
		50200,
		50201,
		50202,
		50203,
		50204,
		50205,
		50207,
		50208,
		50209
												};
	
	private static final int _elementalCrystalIds[] = { 9552,9553,9554,9555,9556,9557 };
	
	//private static final int _enchantChanceRare	= 75;  
	//private static final int _enchantChanceEpic	= 70; //Default was 66
	
	@Override
	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		L2PcInstance activeChar = (L2PcInstance) playable;
		final int itemId = item.getItemId();
		final long itemCount = item.getCount();
		if (itemCount > 0)
		{
			if (itemId == 99900)
			{
				final L2ItemInstance weap1 = ItemTable.getInstance().createItem("weap", _sRareWeapIds[Rnd.get(0, 16)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(84, 3, weap1.getItem().getBodyPart(), false);
				weap1.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = Rnd.get(1,18);
				weap1.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap1);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap1, activeChar, null);
				sm.addItemName(weap1);				
				activeChar.sendPacket(sm);
				
				final L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
				if (weap1.getEnchantLevel() >= 17)
				{
					Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName()+" has extracted a +"+weap1.getEnchantLevel()+" "+weap1.getName() + " from an S-Rare Weapon Pack!"));
				    SocialAction atk = new SocialAction(activeChar.getObjectId(), 3);
				    activeChar.broadcastPacket(atk);
				    if (skill != null)
					{
						MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0);
						activeChar.broadcastPacket(MSU);
					}
				}
			}
			else if (itemId == 99901)
			{
				final L2ItemInstance weap2 = ItemTable.getInstance().createItem("weap", _s80RareWeapIds[Rnd.get(0, 15)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(84, 3, weap2.getItem().getBodyPart(), false);
				weap2.setAugmentation(aug);
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = Rnd.get(1,17);
				weap2.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap2);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap2, activeChar, null);
				sm.addItemName(weap2);				
				activeChar.sendPacket(sm);
				
				final L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
				if (weap2.getEnchantLevel() >= 16)
				{
					Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName()+" has extracted a +"+weap2.getEnchantLevel()+" "+weap2.getName() + " from an S80-Rare Weapon Pack!"));
				    SocialAction atk = new SocialAction(activeChar.getObjectId(), 3);
				    activeChar.broadcastPacket(atk);
				    if (skill != null)
					{
						MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0);
						activeChar.broadcastPacket(MSU);
					}
				}
			}
			else if (itemId == 99903)
			{
				final L2ItemInstance weap3 = ItemTable.getInstance().createItem("weap", _sEpicWeapIds[Rnd.get(0, 16)], 1, activeChar);
				final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(84, 3, weap3.getItem().getBodyPart(), false);				
				weap3.setAugmentation(aug);				
				final int stat12 = 0x0000FFFF & aug.getAugmentationId();
				final int stat34 = aug.getAugmentationId() >> 16;
				activeChar.sendPacket(new ExVariationResult(stat12, stat34, 1));
				int enchantLevel = Rnd.get(1,11);
				weap3.setEnchantLevel(enchantLevel);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(weap3);
				activeChar.sendPacket(iu);
				StatusUpdate su = new StatusUpdate(activeChar.getObjectId());
				su.addAttribute(StatusUpdate.CUR_LOAD, activeChar.getCurrentLoad());
				activeChar.sendPacket(su);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				// lets see
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", weap3, activeChar, null);
				sm.addItemName(weap3);				
				activeChar.sendPacket(sm);
                
				final L2Skill skill = SkillTable.getInstance().getInfo(2025,1);								
				if (weap3.getEnchantLevel() >= 10)
				{
					Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName()+" has extracted a +"+weap3.getEnchantLevel()+" "+weap3.getName() + " from an Epic Weapon Pack!"));
				    SocialAction atk = new SocialAction(activeChar.getObjectId(), 3);
				    activeChar.broadcastPacket(atk);
				    if (skill != null)
					{
						MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0);
						activeChar.broadcastPacket(MSU);
					}
				}
				//System.out.println(activeChar.getName()+"has just opened an epic weapon pack!");				
			}
			else if (itemId == 99912)
			{
				final L2ItemInstance eleCrystals = ItemTable.getInstance().createItem("gems", _elementalCrystalIds[Rnd.get(0, 5)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", eleCrystals, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(eleCrystals);
				activeChar.sendPacket(iu);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(eleCrystals);				
				activeChar.sendPacket(sm);
			}
			else if (itemId == 99914)
			{
				final L2ItemInstance gems = ItemTable.getInstance().createItem("gems", _gemIds[Rnd.get(0, 8)], 1, activeChar);
				activeChar.destroyItem("Extract", item, 1, activeChar, false);
				activeChar.getInventory().addItem("create", gems, activeChar, null);
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(gems);
				activeChar.sendPacket(iu);
				SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_ITEM);
				sm.addItemName(gems);				
				activeChar.sendPacket(sm);
			}
		}
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	//S Rare items can be enchanted up to 18 via pack.
	/*private int enchantItemSRare()
	{
		int i = 1;
		for (; i <= 17; i++)
		{
			if (Rnd.get(100) >= _enchantChanceRare)
			{
				break;
			}
		}
		return i;
	}
	
	//S80 Rare items can be enchanted up to 17 via pack.
	private int enchantItemS80Rare()
	{
		int i = 1;
		for (; i <= 16; i++)
		{
			if (Rnd.get(100) >= _enchantChanceRare)
			{
				break;
			}
		}
		return i;
	}
   
	//S Epic items can be enchanted up to 11 via pack.
	private int enchantItemSEpic()
	{
		int i = 1;
		for (; i <= 10; i++)
		{
			if (Rnd.get(100) >= _enchantChanceEpic)
			{
				break;
			}
		}
		return i;
	}*/
}
