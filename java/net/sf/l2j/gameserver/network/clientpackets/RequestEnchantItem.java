package net.sf.l2j.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.gameserver.util.Util;
import net.sf.l2j.util.Rnd;

public final class RequestEnchantItem extends AbstractEnchantPacket
{
protected static final Logger _logEnchant = Logger.getLogger("enchant");

private static final String _C__58_REQUESTENCHANTITEM = "[C] 58 RequestEnchantItem";

private int _objectId = 0;

@Override
protected void readImpl()
{
	_objectId = readD();
}

@Override
protected void runImpl()
{
	if (_objectId == 0)
		return;
	
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	if (activeChar.isOnline() == 0 || getClient().isDetached())
	{
		activeChar.setActiveEnchantItem(null);
		return;
	}
	
	if (activeChar.isAccountLockedDown() || activeChar.isInJail())
	{
		activeChar.sendMessage("Your account is in lockdown");
		return;
	}
	
	if (activeChar.isProcessingTransaction() || activeChar.isInStoreMode())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_ENCHANT_WHILE_STORE));
		activeChar.setActiveEnchantItem(null);
		return;
	}
	
	final L2ItemInstance item = activeChar.getInventory().getItemByObjectId(_objectId);
	L2ItemInstance scroll = activeChar.getActiveEnchantItem();
	L2ItemInstance support = activeChar.getActiveEnchantSupportItem();
	
	if (item == null || scroll == null)
	{
		activeChar.setActiveEnchantItem(null);
		return;
	}
	
	// template for scroll
	final EnchantScroll scrollTemplate = getEnchantScroll(scroll);
	
	// scroll not found in list
	if (scrollTemplate == null)
		return;
	
	// template for support item, if exist
	EnchantItem supportTemplate = null;
	
	if (support != null)
		supportTemplate = getSupportItem(support);
	
	// first validation check
	if (!scrollTemplate.isValid(item, supportTemplate) || !isEnchantable(item) || item.getOwnerId() != activeChar.getObjectId())
	{
		activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
		activeChar.setActiveEnchantItem(null);
		activeChar.sendPacket(new EnchantResult(2, 0, 0));
		return;
	}
	
	if (activeChar.isInOlympiadMode() || activeChar.isInFairGame())
	{
		activeChar.sendPacket(new EnchantResult(2, 0, 0));
		activeChar.setActiveEnchantItem(null);
		return;
	}
	
	// fast auto-enchant cheat check
	if (activeChar.getActiveEnchantTimestamp() == 0 || System.currentTimeMillis() - activeChar.getActiveEnchantTimestamp() < 2000)
	{
		/*Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " use autoenchant program ", Config.DEFAULT_PUNISH);*/
		activeChar.setActiveEnchantItem(null);
		activeChar.sendPacket(new EnchantResult(2, 0, 0));
		return;
	}
	
	synchronized (item)
	{
		int chance = scrollTemplate.getChance(item, supportTemplate);
		
		if (chance <= 65 && chance > 0)
		{
			if (activeChar.getClan() != null)
			{
				if (activeChar.getClanId() == 268548968 || activeChar.getClanId() == 271210459) // corrupted - smalls clan and lucy mukuro
				{
					chance -= 6;
					if (chance < 1)
						chance = 1;
				}
			}
		}
		
		// last validation check
		if (item.getOwnerId() != activeChar.getObjectId() || chance <= 0 || !isEnchantable(item))
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITION));
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		// attempting to destroy scroll
		scroll = activeChar.getInventory().destroyItem("Enchant", scroll.getObjectId(), 1, activeChar, item);
		
		if (scroll == null)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
			Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a scroll he doesn't have", Config.DEFAULT_PUNISH);
			activeChar.setActiveEnchantItem(null);
			activeChar.sendPacket(new EnchantResult(2, 0, 0));
			return;
		}
		
		if (!scroll.isTradeable() && scroll.getUntradeableTime() > System.currentTimeMillis())
		{
			if (item.getUntradeableTime() < scroll.getUntradeableTime())
			{
				item.setUntradeableTimer(scroll.getUntradeableTime());
				activeChar.sendMessage("Your "+item.getName()+" is now untradeable for "+scroll.getUntradeableTime()/3600000+"" +
						" hours due to your scroll being temporarily untradeable");
			}
		}
		
		// attempting to destroy support if exist
		if (support != null)
		{
			support = activeChar.getInventory().destroyItem("Enchant", support.getObjectId(), 1, activeChar, item);
			
			if (support == null)
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_ITEMS));
				Util.handleIllegalPlayerAction(activeChar, "Player " + activeChar.getName() + " tried to enchant with a support item he doesn't have", Config.DEFAULT_PUNISH);
				activeChar.setActiveEnchantItem(null);
				activeChar.sendPacket(new EnchantResult(2, 0, 0));
				return;
			}
		}
		
		final byte itemFate = scrollTemplate.determineFateOfItemIfFail(item);
		
		final int val = Rnd.get(100);
		
		if (val < chance/* && !item.isAtOrOverMustBreakEnchantLevel()*/)
		{
			// success
			item.setEnchantLevel(item.getEnchantLevel() + 1);
			
			if (item.isEquipped() && item.getUntradeableTime() < 9999999900000L)
			{
				if (item.isUntradeableAfterEquip())
				{
					item.setUntradeableTimer(9999999900000L);
					activeChar.sendMessage("Your "+item.getName()+" is now untradeable");
				}
			}
			
			item.updateDatabase();
			activeChar.sendPacket(new EnchantResult(0, 0, 0));
			
			if (item.getEnchantLevel() >= item.getItem().getClutchEnchantLevel())
			{
				if (item.getUniqueness() >= 2.5)
				{
					if (item.getUniqueness() >= 3.5 || (item.getUniqueness() >= 3 && item.getEnchantLevel() > item.getItem().getClutchEnchantLevel()) || (item.getUniqueness() >= 2.5 && item.getEnchantLevel() > item.getItem().getClutchEnchantLevel()+2))
					{
						String scrollName = "normal scroll";
						
						if (scroll.getName().contains("Greater Titanium"))
							scrollName = "greater titanium scroll.";
						else if (scroll.getName().contains("Greater Dread"))
							scrollName = "greater dread scroll.";
						else if (scroll.getName().contains("Dread"))
							scrollName = "dread scroll.";
						else if (scrollTemplate.isForbidden())
							scrollName = "forbidden scroll.";
						else if (scrollTemplate.isBlessed())
							scrollName = "blessed scroll.";
						else if (scrollTemplate.isCrystal())
							scrollName = "crystal scroll.";
						else if (scrollTemplate.isLegendary())
							scrollName = "legendary scroll.";
						
						Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName()+" has succeeded with the enchantment of +"+item.getEnchantLevel()+" "+item.getName() + " with a "+scrollName));
						SocialAction atk = new SocialAction(activeChar.getObjectId(), 3);
						activeChar.broadcastPacket(atk);
					}
				}
				
				// fireworks
				final L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
				
				if (skill != null)
				{
					MagicSkillUse MSU = new MagicSkillUse(activeChar, activeChar, 2025, 1, 1, 0);
					activeChar.broadcastPacket(MSU);
				}
			}
			
			if (support != null || scroll.getItemId() > 20000 || (item.getUniqueness() > 3 && item.getEnchantLevel() >= 11) || !item.isStandardShopItem() || (item.getUniqueness() >= 4 && item.getEnchantLevel() > 6))
			{
				auditEnchant(activeChar, scroll, item, "Yes "+val+" < "+chance);
				
				if (support != null)
					_log.log(Level.SEVERE, activeChar.getName()+" enchanted with a support item! "+support.getItemId());
				
				if (scroll.getItemId() > 20000)
					_log.log(Level.SEVERE, activeChar.getName()+" enchanted with a divine scroll! "+scroll.getItemId());
			}
			
			if (Config.LOG_ITEM_ENCHANTS)
			{
				LogRecord record = new LogRecord(Level.INFO, "Success");
				record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
				record.setLoggerName("item");
				_logEnchant.log(record);
			}
			
			if(activeChar.isGM())
			{
				activeChar.sendMessage("(Success) Rolled: "+val+" -vs- item enchant rate: "+chance);
			}
		}
		else //item failed
		{
			if (itemFate != REMAIN_SAME_ENCHANT && item.getEnchantLevel() >= item.getItem().getClutchEnchantLevel() && item.getUniqueness() >= 2.5)
			{
				if (item.getUniqueness() >= 3.5 || (item.getUniqueness() >= 3 && item.getEnchantLevel() > item.getItem().getClutchEnchantLevel()) || (item.getUniqueness() >= 2.5 && item.getEnchantLevel() > item.getItem().getClutchEnchantLevel()+2))
				{
					String scrollName = "normal scroll";
					
					if (scroll.getName().contains("Greater Titanium"))
						scrollName = "greater titanium scroll";
					else if (scroll.getName().contains("Greater Dread"))
						scrollName = "greater dread scroll";
					else if (scroll.getName().contains("Dread"))
						scrollName = "dread scroll";
					else if (scrollTemplate.isForbidden())
						scrollName = "forbidden scroll";
					else if (scrollTemplate.isBlessed())
						scrollName = "blessed scroll";
					else if (scrollTemplate.isCrystal())
						scrollName = "crystal scroll";
					else if (scrollTemplate.isLegendary())
						scrollName = "legendary scroll";
					
					Broadcast.toAllOnlinePlayers(SystemMessage.sendString(activeChar.getName()+" has failed with the enchantment of +"+(item.getEnchantLevel()+1)+" "+item.getName() + " with a "+scrollName));
					SocialAction atk = new SocialAction(activeChar.getObjectId(), 13);
					activeChar.broadcastPacket(atk);
				}
			}
			
			if (itemFate == ENCHANT_TO_10_OR_6_OR_3_OR_0) // returns to + 10 or 6 or 3 or 0
			{
				activeChar.sendPacket(new EnchantResult(5, 0, 0));
				
				int enc = item.getEnchantLevel();
				
				if (enc >= 10)
				{
					enc = 10;
				}
				else if (enc >= 6)
				{
					enc = 6;
				}
				else if (enc >= 3)
				{
					enc = 3;
				}
				else
				{
					enc = 0;
				}
				
				item.setEnchantLevel(enc);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == ENCHANT_TO_12_OR_8_OR_0) // returns to + 12 or 8 or 0
			{
				activeChar.sendPacket(new EnchantResult(5, 0, 0));
				
				int enc = item.getEnchantLevel();
				
				if (enc >= 12)
				{
					enc = 12;
				}
				else if (enc >= 8)
				{
					enc = 8;
				}
				else
				{
					enc = 0;
				}
				
				item.setEnchantLevel(enc);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == REMAIN_SAME_ENCHANT) // safe enchant - remain old value need retail message
			{
				activeChar.sendPacket(new EnchantResult(5, 0, 0));
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == ENCHANT_MINUS_ONE_OR_NEXT_LEVEL) //item enchant - 1 or next lvl
			{
				activeChar.sendPacket(new EnchantResult(5, 0, 0));
				
				int enc = item.getEnchantLevel();
				
				if (enc >= 10)
				{
					enc = Math.max(10, enc-1);
				}
				else if (enc >= 3)
				{
					enc = Math.max(3, enc-1);
				}
				else
				{
					enc = Math.max(0, enc-1);
				}
				
				item.setEnchantLevel(enc);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Blessed Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == ENCHANT_TO_4_OR_0) //item enchant = 4 or 0
			{
				activeChar.sendPacket(new EnchantResult(3, 0, 0));
				
				if (item.getEnchantLevel() >= 4)
					item.setEnchantLevel(4);
				else
					item.setEnchantLevel(0);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == ENCHANT_TO_7_OR_3_OR_0) // returns to + 7 or 3 or 0
			{
				activeChar.sendPacket(new EnchantResult(5, 0, 0));
				
				int enc = item.getEnchantLevel();
				
				if (enc >= 7)
				{
					enc = 7;
				}
				else if (enc >= 3)
				{
					enc = 3;
				}
				else
				{
					enc = 0;
				}
				
				item.setEnchantLevel(enc);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Safe Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else if (itemFate == RETURNS_TO_0) //item enchant = 0
			{
				activeChar.sendPacket(new EnchantResult(3, 0, 0));
				item.setEnchantLevel(0);
				
				if (item.shouldBeNowSetAsTradeable())
				{
					item.setUntradeableTimer(0);
					activeChar.sendMessage("Your "+item.getName()+" is now tradeable");
				}
				
				item.updateDatabase();
				
				if (Config.LOG_ITEM_ENCHANTS)
				{
					LogRecord record = new LogRecord(Level.INFO, "Fail");
					record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
					record.setLoggerName("item");
					_logEnchant.log(record);
				}
			}
			else   //bye bye item
			{
				int crystalId = item.getItem().getCrystalItemId();
				int count = (int) ((item.getCrystalCount() - (item.getItem().getCrystalCount() + 1) / 2) * item.getItem().getUniqueness());
				if (count < 1)
					count = 1;
				
				final L2ItemInstance destroyItem = activeChar.getInventory().destroyItem("Enchant", item, activeChar, null);
				
				if (destroyItem == null)
				{
					// unable to destroy item, cheater ?
					Util.handleIllegalPlayerAction(activeChar, "Unable to delete item on enchant failure from player " + activeChar.getName() + ", possible cheater !", Config.DEFAULT_PUNISH);
					activeChar.setActiveEnchantItem(null);
					activeChar.sendPacket(new EnchantResult(2, 0, 0));
					
					if (Config.LOG_ITEM_ENCHANTS)
					{
						LogRecord record = new LogRecord(Level.INFO, "UNABLE to destroy");
						record.setParameters(new Object[]{activeChar, item, scroll, support, chance});
						record.setLoggerName("item");
						_logEnchant.log(record);
					}
					
					return;
				}
				
				L2ItemInstance crystals = null;
				
				if (crystalId != 0)
				{
					crystals = activeChar.getInventory().addItem("Enchant", crystalId, count, activeChar, destroyItem);
					
					SystemMessage sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
					sm.addItemName(crystals);
					sm.addItemNumber(count);
					activeChar.sendPacket(sm);
				}
				
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					if (destroyItem.getCount() == 0)
						iu.addRemovedItem(destroyItem);
					else
						iu.addModifiedItem(destroyItem);
					if (crystals != null)
						iu.addItem(crystals);
					
					activeChar.sendPacket(iu);
				}
				
				L2World world = L2World.getInstance();
				world.removeObject(destroyItem);
				
				if (crystalId == 0)
					activeChar.sendPacket(new EnchantResult(4, 0, 0));
				else
					activeChar.sendPacket(new EnchantResult(1, crystalId, count));
			}
			
			if (support != null || scroll.getItemId() > 20000 || (item.getUniqueness() > 3 && item.getEnchantLevel() >= 11) || !item.isStandardShopItem() || (item.getUniqueness() >= 4 && item.getEnchantLevel() > 6))
			{
				auditEnchant(activeChar, scroll, item, "No "+val+" >= "+chance);
				
				if (support != null)
					_log.log(Level.SEVERE, activeChar.getName()+" enchanted with a support item! "+support.getItemId());
				
				if (scroll.getItemId() > 20000)
					_log.log(Level.SEVERE, activeChar.getName()+" enchanted with a divine scroll! "+scroll.getItemId());
			}
			
			if(activeChar.isGM())
			{
				activeChar.sendMessage("(Failure) Rolled: "+Math.max(val, chance+1)+" -vs- item enchant rate: "+chance);
			}
		}
		
		activeChar.sendPacket(new ItemList(activeChar, false));
		activeChar.broadcastUserInfo();
		activeChar.setActiveEnchantItem(null);
	}
}

@Override
public String getType()
{
	return _C__58_REQUESTENCHANTITEM;
}

public final static void auditEnchant(final L2PcInstance player, final L2ItemInstance scroll, final L2ItemInstance item, final String success)
{
	final String player_name = player.getAccountName() + " - " + player.getName();
	final String player_IP = player.getIP();
	
	final String scroll_name = scroll != null ? scroll.getItemName()+" - "+scroll.getObjectId() : "PVP";
	
	String itemName = "";
	
	if (item != null)
		itemName = "+"+item.getEnchantLevel()+" "+item.getName()+" "+item.getObjectId();
	
	String today = GMAudit._formatter.format(new Date());
	
	Connection con = null;
	PreparedStatement statement = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement("INSERT INTO audit_enchant(player, player_IP, scroll, item, success, date) VALUES(?,?,?,?,?,?)");
		
		statement.setString(1, player_name);
		statement.setString(2, player_IP);
		statement.setString(3, scroll_name);
		statement.setString(4, itemName);
		statement.setString(5, success);
		statement.setString(6, today);
		
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.fine( "could not audit Enchant, Char name: "+player.getName()+" : "+e.getMessage());
	}
	finally { try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); } }
}
}