package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2RecipeList;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public class Recipes implements IItemHandler
{
	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		L2PcInstance activeChar = (L2PcInstance) playable;
		L2RecipeList rp = RecipeController.getInstance().getRecipeByItemId(item.getItemId());
		if (rp == null)
			return;
		if (activeChar.hasRecipeList(rp.getId()))
			activeChar.sendPacket(new SystemMessage(SystemMessageId.RECIPE_ALREADY_REGISTERED));
		else
		{
			if (rp.isDwarvenRecipe())
			{
				if (activeChar.hasDwarvenCraft())
				{
					if (rp.getLevel() > activeChar.getDwarvenCraft())
					{
						//can't add recipe, becouse create item level too low
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER));
					}
					else if (activeChar.getDwarvenRecipeBook().length >= activeChar.getDwarfRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.getDwarfRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerDwarvenRecipeList(rp, true);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
					}
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
			}
			else
			{
				if (activeChar.hasCommonCraft())
				{
					if (rp.getLevel() > activeChar.getCommonCraft())
					{
						//can't add recipe, becouse create item level too low
						activeChar.sendPacket(new SystemMessage(SystemMessageId.CREATE_LVL_TOO_LOW_TO_REGISTER));
					}
					else if (activeChar.getCommonRecipeBook().length >= activeChar.getCommonRecipeLimit())
					{
						//Up to $s1 recipes can be registered.
						SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_REGISTER);
						sm.addNumber(activeChar.getCommonRecipeLimit());
						activeChar.sendPacket(sm);
					}
					else
					{
						activeChar.registerCommonRecipeList(rp, true);
						activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false);
						activeChar.sendPacket(new SystemMessage(SystemMessageId.S1_ADDED).addItemName(item));
					}
				}
				else
					activeChar.sendPacket(new SystemMessage(SystemMessageId.CANT_REGISTER_NO_ABILITY_TO_CRAFT));
			}
		}
	}
}
