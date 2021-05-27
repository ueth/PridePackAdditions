package net.sf.l2j.gameserver.model.actor.instance;

//import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
//import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

public class L2NoblesseInstance extends L2NpcInstance
{
	
	private static int NOBLESS_CC = 7694;
	private static int ANCIENT_CLOAK = 14609;
	private static int ITEM_REQ = 6673;
	private static int ITEM_QUAN = 25;
	
	@Override
	public void onBypassFeedback(L2PcInstance player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (command.equals("NoblesseMe"))
		{
			makeMeNobless(player);
		}
		else
        {
			super.onBypassFeedback(player, command);
		}
	}
	
	private static void makeMeNobless(L2PcInstance player)
	{
		
		L2ItemInstance item = player.getInventory().getItemByItemId(ITEM_REQ);
		
		if (player.isNoble())
		{
			player.sendMessage("You are already a noblesse");
		    return;		    
		}
		else if (item == null || item.getCount() < ITEM_QUAN)
		{
			player.sendMessage("You need "+ ITEM_QUAN +" Festival Adena in order to become a noblesse");
		    return;		    
		}
		else if (player.getLevel() < 85)
		{
			player.sendMessage("You must be Lvl 85 or more in order to become a noblesse");
		    return;		    
		}
		player.sendMessage("Congratulations! You are now a Noblesse!");
		player.playSound("ItemSound.quest_finish");
		player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
		player.setNoble(true);
		player.setFame(player.getFame()+50);
		player.sendMessage("Your fame increased by 50");
		player.healHP();
		player.destroyItemByItemId("noblesse", ITEM_REQ, ITEM_QUAN, player, true);
		player.addItem("Ancient Cloak", ANCIENT_CLOAK, 1, player, true);
		player.addItem("Noblesse Circlet", NOBLESS_CC, 1, player, true);	
	}
	
	public L2NoblesseInstance(int objectID, L2NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public String getHtmlPath(int npcId, int val)
	{
		return "data/html/custom/Noblesse/noblessemain.htm";
	}

    /* (non-Javadoc)
     * @see net.sf.l2j.gameserver.model.L2Object#isAttackable()
     */
    @Override
	public boolean isAutoAttackable(L2Character attacker)
	{
		return false;
	}}