package net.sf.l2j.gameserver.model.zone.type;

import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;

public class L2ChaoticZone extends L2ZoneType
{
public L2ChaoticZone(final int id)
{
	super(id);
}

@Override
protected void onEnter(final L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)character;
		player.setInsideZone(L2Character.ZONE_CHAOTIC, true);
		player.sendMessage("You have entered a chaotic zone, where no penalty is applied upon death with karma and karma gain is reduced by 40%");
		
		if (_id == 11037) //PI
		{
			player.setIsInPI(true);
		}
		else if (_id == 51000 && !player.isInHellbound()) //Hellbound
		{
			player.setIsInHellbound(true);
			player.sendMessage("You have entered Hellbound");
			if (!player.isGM())
			{
				if (player.isTransformed())
				{
					player.stopTransformation(null);
					player.sendMessage("Transformations are not allowed in Hellbound");
				}
				if (player.isMounted())
				{
					player.dismount();
					player.sendMessage("Cannot use your mount in Hellbound");
				}
			}
		}
		else if (_id == 50000 && !player.isInFT()) //FT
		{
			player.setIsInFT(true);
			
			player.sendMessage("You have entered Forgotten Temple grounds, you cannot use S grade or below weapons or armors");
			
			if (!player.isGM())
			{
				boolean update = false;
				
				for (L2ItemInstance item : player.getInventory().getItems())
				{
					if (item != null && item.isEquipped())
					{
						if (item.isARestrictedItemFT())
						{
							if (item.isAugmented())
								item.getAugmentation().removeBonus(player);
							
							L2ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(player.getInventory().getSlotFromItem(item));
							InventoryUpdate iu = new InventoryUpdate();
							for (L2ItemInstance element : unequiped)
								iu.addModifiedItem(element);
							player.sendPacket(iu);
							update = true;
						}
					}
				}
				
				if (update)
					player.broadcastUserInfo();
			}
		}
	}
}

@Override
protected void onExit(final L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)character;
		player.setInsideZone(L2Character.ZONE_CHAOTIC, false);
		player.sendMessage("You have left a chaotic zone");
		
		if (_id == 11037) //PI
		{
			player.setIsInPI(false);
		}
		else if (_id == 51000 && player.isInHellbound()) //Hellbound
		{
			player.setIsInHellbound(false);
		}
		else if (_id == 50000) //FT
		{
			player.setIsInFT(false);
		}
	}
}

@Override
public void onDieInside(final L2Character character)
{
}

@Override
public void onReviveInside(final L2Character character)
{
}
}