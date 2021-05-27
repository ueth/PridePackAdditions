package net.sf.l2j.gameserver.model;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;
import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_ADENA;
//import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.MAX_SCROLL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GeoData;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.AugmentationData;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.instancemanager.ItemsOnGroundManager;
import net.sf.l2j.gameserver.instancemanager.MercTicketManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.knownlist.NullKnownList;
//import net.sf.l2j.gameserver.model.itemcontainer.PcInventory;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.AbstractRefinePacket;
import net.sf.l2j.gameserver.network.clientpackets.RequestEnchantItem;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.DropItem;
import net.sf.l2j.gameserver.network.serverpackets.GetItem;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SpawnItem;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.funcs.Func;
import net.sf.l2j.gameserver.templates.item.L2Armor;
import net.sf.l2j.gameserver.templates.item.L2EtcItem;
import net.sf.l2j.gameserver.templates.item.L2EtcItemType;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.templates.item.L2Weapon;
import net.sf.l2j.gameserver.util.Broadcast;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

public final class L2ItemInstance extends L2Object
{
protected static final Logger _log = Logger.getLogger(L2ItemInstance.class.getName());
private static final Logger _logItems = Logger.getLogger("item");

/** Enumeration of locations for item */
public static enum ItemLocation {
VOID,
INVENTORY,
PAPERDOLL,
WAREHOUSE,
CLANWH,
PET,
PET_EQUIP,
LEASE,
FREIGHT,
NPC
}

/** ID of the owner */
private int _ownerId;

/** ID of who dropped the item last, used for knownlist */
private int _dropperObjectId = 0;

/** Quantity of the item */
private long _count;
/** Initial Quantity of the item */
private long _initCount;
/** Remaining time (in miliseconds) */
private long _time;
/** Quantity of the item can decrease */
private boolean _decrease = false;

/** ID of the item */
private final int _itemId;

/** Object L2Item associated to the item */
private final L2Item _item;

/** Location of the item : Inventory, PaperDoll, WareHouse */
private ItemLocation _loc;

/** Slot where item is stored : Paperdoll slot, inventory order ...*/
private int _locData;

/** Level of enchantment of the item */
private int _enchantLevel;

/** Wear Item */
private boolean _wear;

/** Augmented Item */
private L2Augmentation _augmentation=null;

/** Shadow item */
private int _mana=-1;
private boolean _consumingMana = false;
private static final int MANA_CONSUMPTION_RATE = 60000;

/** Custom item types (used loto, race tickets) */
private int _type1;
private int _type2;

private long _dropTime;

private boolean _chargedFishtshot =	false;

private boolean _protected;

public static final int UNCHANGED = 0;
public static final int ADDED = 1;
public static final int REMOVED = 3;
public static final int MODIFIED = 2;
private int _lastChange = 2;	//1 ??, 2 modified, 3 removed
private boolean _existsInDb; // if a record exists in DB.
private boolean _storedInDb; // if DB data is up-to-date.

public String _source;
public String _instanceDroppedFrom;

private final ReentrantLock _dbLock = new ReentrantLock();

private Elementals _elementals = null;

private ScheduledFuture<?> itemLootShedule = null;
public ScheduledFuture<?> _lifeTimeTask;

private long _untradeableTime = 0;
/**
 * Constructor of the L2ItemInstance from the objectId and the itemId.
 * @param objectId : int designating the ID of the object in the world
 * @param itemId : int designating the ID of the item
 */
public L2ItemInstance(int objectId, int itemId)
{
	super(objectId);
	_itemId = itemId;
	_item = ItemTable.getInstance().getTemplate(itemId);
	if (_itemId == 0 || _item == null)
		throw new IllegalArgumentException();
	super.setName(_item.getName());
	setCount(1);
	_loc = ItemLocation.VOID;
	_type1 = 0;
	_type2 = 0;
	_dropTime = 0;
	_mana = _item.getDuration();
	_source = null;
	_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long)_item.getTime()*60*1000);
	scheduleLifeTimeTask();
	
	if (_item.isHeroItem() && _item instanceof L2Weapon)
		_enchantLevel = 16;
	/*		else if (automaticPlus12())
			_enchantLevel = 12;
		else if (automaticPlus3())
			_enchantLevel = 3;*/
}

public L2ItemInstance(int objectId, int itemId, String process, String from)
{
	super(objectId);
	_itemId = itemId;
	_item = ItemTable.getInstance().getTemplate(itemId);
	if (_itemId == 0 || _item == null)
		throw new IllegalArgumentException();
	
	_source = from;
	
	if (_source == null)
		_source = process;
	
	int unpermTimer = 0;
	
	if (_item.getPermChance() > -1)
	{
		int permChance = _item.getPermChance();
		
		if (process.equalsIgnoreCase("Multisell"))
		{ 	//default perm chance
		}
		else
		{
			if (process.equalsIgnoreCase("Loot")) //dropped by mobs
			{
				/*permChance = (int) ((permChance + 5)*5.5);*/
			}
			else
				permChance = 100;   //100% chance through other means (such as GM spawn)
		}
		
		final boolean perm;
		
		if (permChance < 1)
			perm = false;
		else
			perm = Rnd.get(100) < permChance;
		
		if (!perm)
		{
			if (Rnd.get(10) == 0)
				unpermTimer = 45 * 24 * 60; //45 days in MINUTES in 1/10 chance
			else
				unpermTimer = 30 * 24 * 60; //30 days in MINUTES
		}
	}
	else if (from != null && from.startsWith("shadow npc"))
	{
		switch ((int)_item.getUniqueness())
		{
		case 0:
			unpermTimer = 168 * 61; //7 days in MINUTES
			break;
		case 1:
			unpermTimer = 168 * 60; //7 days in MINUTES
			break;
		case 2:
			unpermTimer = 168 * 59; //7 days in MINUTES
			break;
		case 3:
			unpermTimer = 168 * 58; //7 days in MINUTES
			break;
		default:
			unpermTimer = 168 * 57; //7 days in MINUTES
			break;
		}
	}
	else if (from != null && from.startsWith("donation npc"))
	{
		final long newTime = System.currentTimeMillis() + (Config.UNTRADEABLE_DONATE*60*60*1000);
		
		setUntradeableTimer(newTime);
		
		addAutoAugmentationDonation();
	}
	
	super.setName(_item.getName());
	setCount(1);
	_loc = ItemLocation.VOID;
	_type1 = 0;
	_type2 = 0;
	_dropTime = 0;
	_mana = _item.getDuration();
	
	if (unpermTimer > 0)
		_time = System.currentTimeMillis() + ((long)unpermTimer*60*1000);
	else
		_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long)_item.getTime()*60*1000);
	
	scheduleLifeTimeTask();
	
	if (_item.isHeroItem() && _item instanceof L2Weapon)
		_enchantLevel = 16;
	/*		else if (automaticPlus12())
			_enchantLevel = 12;
		else if (automaticPlus3())
			_enchantLevel = 3;*/
}

/*	public boolean automaticPlus12()
	{
		return _item.getItemGrade() < L2Item.CRYSTAL_S80 && _item.getItemGrade() > L2Item.CRYSTAL_NONE && !isARestrictedItem() && _item.getBodyPart() != L2Item.SLOT_UNDERWEAR;
	}
	public boolean automaticPlus3()
	{
		return (_item.getBodyPart() == L2Item.SLOT_UNDERWEAR && _item.getCrystalType() < L2Item.CRYSTAL_S80) || _item.getName().contains("Pride");
	}*/

/**
 * Constructor of the L2ItemInstance from the objetId and the description of the item given by the L2Item.
 * @param objectId : int designating the ID of the object in the world
 * @param item : L2Item containing informations of the item
 */
public L2ItemInstance(int objectId, L2Item item)
{
	super(objectId);
	_itemId = item.getItemId();
	_item = item;
	if (_itemId == 0)
		throw new IllegalArgumentException();
	super.setName(_item.getName());
	setCount(1);
	_loc = ItemLocation.VOID;
	_mana = _item.getDuration();
	_time = _item.getTime() == -1 ? -1 : System.currentTimeMillis() + ((long)_item.getTime()*60*1000);
	scheduleLifeTimeTask();
	
	if (_item.isHeroItem() && _item instanceof L2Weapon)
		_enchantLevel = 16;
	/*		else if (automaticPlus12())
			_enchantLevel = 12;
		else if (automaticPlus3())
			_enchantLevel = 3;*/
	
	_source = null;
}

@Override
public void initKnownList()
{
	setKnownList(new NullKnownList(this));
}

/**
 * Remove a L2ItemInstance from the world and send server->client GetItem packets.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Send a Server->Client Packet GetItem to player that pick up and its _knowPlayers member </li>
 * <li>Remove the L2Object from the world</li><BR><BR>
 *
 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T REMOVE the object from _allObjects of L2World </B></FONT><BR><BR>
 *
 * <B><U> Assert </U> :</B><BR><BR>
 * <li> this instanceof L2ItemInstance</li>
 * <li> _worldRegion != null <I>(L2Object is visible at the beginning)</I></li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Do Pickup Item : PCInstance and Pet</li><BR><BR>
 *
 * @param player Player that pick up the item
 *
 */
public final void pickupMe(L2Character player)
{
	if (Config.ASSERT) assert getPosition().getWorldRegion() != null;
	
	L2WorldRegion oldregion = getPosition().getWorldRegion();
	
	// Create a server->client GetItem packet to pick up the L2ItemInstance
	GetItem gi = new GetItem(this, player.getObjectId());
	player.broadcastPacket(gi);
	
	synchronized (this)
	{
		setIsVisible(false);
		getPosition().setWorldRegion(null);
	}
	
	// if this item is a mercenary ticket, remove the spawns!
	int itemId = getItemId();
	
	if (MercTicketManager.getInstance().getTicketCastleId(itemId) > 0)
	{
		MercTicketManager.getInstance().removeTicket(this);
		ItemsOnGroundManager.getInstance().removeObject(this);
	}
	
	/*if (itemId == 57 || itemId == 6353)
        {
        	L2PcInstance actor = player.getActingPlayer();
        	if (actor != null)
        	{
        		QuestState qs = actor.getQuestState("255_Tutorial");
            	if (qs != null)
            		qs.getQuest().notifyEvent("CE"+itemId+"",null, actor);
        	}
        }*/
	// outside of synchronized to avoid deadlocks
	// Remove the L2ItemInstance from the world
	L2World.getInstance().removeVisibleObject(this, oldregion);
}

/**
 * Sets the ownerID of the item
 * @param process : String Identifier of process triggering this action
 * @param owner_id : int designating the ID of the owner
 * @param creator : L2PcInstance Player requesting the item creation
 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
 */
public void setOwnerId(String process, int owner_id, L2PcInstance creator, L2Object reference)
{
	setOwnerId(owner_id);
	
	if (Config.LOG_ITEMS)
	{
		LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
		record.setLoggerName("item");
		record.setParameters(new Object[]{this, creator, reference});
		_logItems.log(record);
	}
	
	if (creator != null)
	{
		if (creator.isGM())
		{
			String referenceName = "no-reference";
			if (reference != null)
			{
				referenceName = (reference.getName() != null?reference.getName():"no-name");
			}
			String targetName = (creator.getTarget() != null?creator.getTarget().getName():"no-target");
			if (Config.GMAUDIT)
				GMAudit.auditGMAction(creator.getAccountName() + " - " + creator.getName(), process + "(id: "+getItemId()+" name: "+getName()+" - "+getObjectId()+")", targetName, "reference: " + referenceName);
		}
	}
}

/**
 * Sets the ownerID of the item
 * @param owner_id : int designating the ID of the owner
 */
public void setOwnerId(int owner_id)
{
	if (owner_id == _ownerId) return;
	
	_ownerId = owner_id;
	_storedInDb = false;
}

/**
 * Returns the ownerID of the item
 * @return int : ownerID of the item
 */
public int getOwnerId()
{
	return _ownerId;
}

/**
 * Sets the location of the item
 * @param loc : ItemLocation (enumeration)
 */
public void setLocation(ItemLocation loc)
{
	setLocation(loc, 0);
}

/**
 * Sets the location of the item.<BR><BR>
 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
 * @param loc : ItemLocation (enumeration)
 * @param loc_data : int designating the slot where the item is stored or the village for freights
 */
public void setLocation(ItemLocation loc, int loc_data)
{
	if (loc == _loc && loc_data == _locData)
		return;
	_loc = loc;
	_locData = loc_data;
	_storedInDb = false;
}

public ItemLocation getLocation()
{
	return _loc;
}

/**
 * Sets the quantity of the item.<BR><BR>
 * @param count the new count to set
 */
public void setCount(long count)
{
	if (getCount() == count)
	{
		return;
	}
	
	/*if (count > PcInventory.MAX_SCROLL)
	{
		if (isScroll())
			count = PcInventory.MAX_SCROLL;
	}*/
	
	_count = count >= -1 ? count : 0;
	_storedInDb = false;
}


public boolean isScroll()
{
	return (getItem() instanceof L2EtcItem && getEtcItem().getItemType() == L2EtcItemType.SCROLL);
}

/**
 * @return Returns the count.
 */
public long getCount()
{
	return _count;
}

/**
 * Sets the quantity of the item.<BR><BR>
 * <U><I>Remark :</I></U> If loc and loc_data different from database, say datas not up-to-date
 * @param process : String Identifier of process triggering this action
 * @param count : int
 * @param creator : L2PcInstance Player requesting the item creation
 * @param reference : L2Object Object referencing current action like NPC selling item or previous item in transformation
 */
public void changeCount(String process, long count, L2PcInstance creator, L2Object reference)
{
	if (count == 0)
	{
		return;
	}
	
	long max = Integer.MAX_VALUE;
	
	if (getItemId() == ADENA_ID)
	{
		max = MAX_ADENA;
	}
	/*else if (isScroll())
	{
		max = MAX_SCROLL;
	}*/
	
	if ( count > 0 && getCount() > max - count)
	{
		setCount(max);
	}
	else
	{
		setCount(getCount() + count);
	}
	
	if (getCount() < 0)
	{
		setCount(0);
	}
	
	_storedInDb = false;
	
	if (Config.LOG_ITEMS && process != null)
	{
		LogRecord record = new LogRecord(Level.INFO, "CHANGE:" + process);
		record.setLoggerName("item");
		record.setParameters(new Object[]{this, creator, reference});
		_logItems.log(record);
	}
	
	if (creator != null)
	{
		if (creator.isGM())
		{
			String referenceName = "no-reference";
			if (reference != null)
			{
				referenceName = (reference.getName() != null?reference.getName():"no-name");
			}
			String targetName = (creator.getTarget() != null?creator.getTarget().getName():"no-target");
			if (Config.GMAUDIT)
				GMAudit.auditGMAction(creator.getAccountName() + " - " + creator.getName(), process + "(id: "+getItemId()+" objId: "+getObjectId()+" name: "+getName()+" count: "+count+")", targetName, "Reference: " + referenceName);
		}
	}
}

// No logging (function designed for shots only)
public void changeCountWithoutTrace(int count, L2PcInstance creator, L2Object reference)
{
	changeCount(null, count, creator, reference);
}

/**
 * Returns if item is equipable
 * @return boolean
 */
public boolean isEquipable()
{
	return !(_item.getBodyPart() == 0 || _item instanceof L2EtcItem );
}

/**
 * Returns if item is equipped
 * @return boolean
 */
public boolean isEquipped()
{
	return _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP;
}

/**
 * Returns the slot where the item is stored
 * @return int
 */
public int getLocationSlot()
{
	if (Config.ASSERT) assert _loc == ItemLocation.PAPERDOLL || _loc == ItemLocation.PET_EQUIP || _loc == ItemLocation.FREIGHT || _loc == ItemLocation.INVENTORY;
	return _locData;
}

/*public String getEquippedItemIcon()
{
    switch (getItemId())
    {
    case 57:
	    return "icon.etc_adena_i00";
    case 959:
	    return "icon.etc_scroll_of_enchant_weapon_i05";
    case 960:
	    return "icon.etc_scroll_of_enchant_armor_i05";
    case 961:
	    return "icon.etc_scroll_of_enchant_weapon_i01";
    case 962:
	    return "icon.etc_scroll_of_enchant_armor_i01";
    case 1538:
	    return "icon.etc_scroll_of_return_i01";
    case 2624:
	    return "L2PrideHonorIcons.dual_books";
    case 3936:
	    return "icon.etc_scroll_of_resurrection_i01";
    case 4355:
	    return "icon.etc_magic_coin_blue_i01";
    case 4356:
	    return "icon.etc_magic_coin_gold_i02";
    case 4357:
	    return "icon.etc_magic_coin_silver_i02";
    case 4358:
	    return "icon.etc_magic_coin_blood_i02";
    case 4676:
	    return "L2PrideHonorIcons.medal_of_victory";
    case 4708:
	    return "icon.weapon_samurai_longsword_i01";
    case 5129:
		return "L2PrideHonorIcons.doll_knives";
    case 5283:
		return "icon.etc_mochi_i00";
    case 6392:
	    return "icon.etc_event_medal_i00";
    case 6393:
	    return "icon.etc_event_glitter_medal_i00";
    case 6577:
	    return "icon.etc_blessed_scrl_of_ench_wp_s_i05";
    case 6578:
	    return "icon.etc_blessed_scrl_of_ench_am_s_i05";
    case 6583:
	    return "icon.weapon_forgotten_blade_i01"; //Forgotten Blade
    case 6622:
	    return "icon.etc_codex_of_giant_i00";
    case 6673:
	    return "icon.etc_coin_of_fair_i00";
    case 8742:
	    return "icon.etc_mineral_special_i03"; //Mid 76 LS
    case 8752:
	    return "icon.etc_mineral_rare_i03"; //High 76 LS
    case 8762:
	    return "icon.etc_mineral_unique_i03"; //Top 76 LS
    case 9546:
	    return "icon.etc_fire_stone_i00";
    case 9547:
	    return "icon.etc_water_stone_i00";
    case 9548:
	    return "icon.etc_earth_stone_i00";
    case 9549:
	    return "icon.etc_wind_stone_i00";
    case 9550:
	    return "icon.etc_unholy_stone_i00";
    case 9551:
	    return "icon.etc_holy_stone_i00";
    case 9552:
	    return "icon.etc_fire_crystal_i00";
    case 9553:
	    return "icon.etc_water_crystal_i00";
    case 9554:
	    return "icon.etc_earth_crystal_i00";
    case 9555:
	    return "icon.etc_wind_crystal_i00";
    case 9556:
	    return "icon.etc_unholy_crystal_i00";
    case 9557:
	    return "icon.etc_holy_crystal_i00";   
    case 9574:
	    return "icon.etc_mineral_special_i03"; //Mid 80 LS
    case 9575:
	    return "icon.etc_mineral_rare_i03"; //High 80 LS
    case 9576:
	    return "icon.etc_mineral_unique_i03"; //Top 80 LS
    case 9592:
	    return "icon.etc_rbracelet_s_i03"; //Mithril Bracelet
    case 9625:
	    return "icon.etc_codex_of_giant_i01";
    case 9626:
	    return "icon.etc_codex_of_giant_i02";
    case 9627:
	    return "icon.etc_codex_of_giant_i03";
    case 10170:
		return "icon.accessary_red_cresent_earing_i00"; //Baylors Earring
    case 10314:
		return "icon.accessary_ring_of_anguish_i00"; //Beleths Ring
    case 10413:
		return "icon.etc_armor_soul_i00"; //Dynasty Essence
    case 10484:
	    return "icon.etc_mineral_special_i03"; //Mid 82 LS
    case 10485:
	    return "icon.etc_mineral_rare_i03"; //High 82 LS
    case 10486:
	    return "icon.etc_mineral_unique_i03"; //Top 82 LS
    case 10567:
	    return "icon.etc_spell_books_element_i00"; //FS-Dread Pool
    case 13002:
		return "icon.etc_crystal_blue_i00"; //Essence of Kamaloka
    case 13237:
		return "icon.accessory_ar_karm_i00";
    case 13420:
		return "icon.etc_five_color_honey_cake_i00";
    case 13421:
		return "icon.etc_cake_of_wheatlfour_i00";
    case 13796:
		return "icon.etc_crystal_blue_i00"; //Nucleous
    case 13895:
		return "icon.armor_belt_i01"; //Leather belt
    case 13896:
		return "icon.armor_belt_i02"; //Iron belt
    case 13986:
		return "icon.weapon_dual_sword_i00"; //Dual Forgotten Blades
    case 14167:
	    return "icon.etc_mineral_special_i03"; //Mid 84 LS
    case 14168:
	    return "icon.etc_mineral_rare_i03"; //High 84 LS
    case 14169:
	    return "icon.etc_mineral_unique_i03"; //Top 84 LS
    case 14181:
	    return "icon.etc_spell_books_element_i00"; //FS-Dread Pool #2
    case 14609:
	    return "icon.vesper_cloack_i00"; //Ancient Cloak
    case 22014:
	    return "br_cashtex.item.br_cash_cry_of_ench_wp_b_i00"; 
    case 22016:
	    return "br_cashtex.item.br_cash_cry_of_ench_am_b_i00"; 
    case 22018:
	    return "br_cashtex.item.br_cash_blessed_cry_of_ench_wp_b_i00";
    case 22020:
	    return "br_cashtex.item.br_cash_blessed_cry_of_ench_am_b_i00";
    case 50000:
	    return "l2prideicons.iron_ore"; //Iron Ore
    case 50001:
	    return "l2prideicons.silver_ore"; //Silver Ore
    case 50002:
	    return "l2prideicons.gold_ore"; //Gold Ore
    case 50003:
	    return "l2prideicons.chrysolite_ore"; //Chrysolite Ore
    case 50004:
	    return "l2prideicons.damascus_ore"; //Damascus Ore
    case 50005:
	    return "l2prideicons.mithril_ore"; //Mithril Ore
    case 50006:
	    return "l2prideicons.adamantium_ore"; //Adamantium Ore
    case 50007:
	    return "l2prideicons.titanium_ore"; //Titanium Ore
    case 50008:
	    return "l2prideicons.meteoric_ore"; //Meteoric Ore
    case 50009:
	    return "l2prideicons.eternium_ore"; //Eternium Ore
    case 50010:
	    return "l2prideicons.dread_ore"; //Dread Shard
    case 50022:
	    return "L2PrideHonorIcons.porphyric_essence"; //Porphyric Essence
    case 50023:
	    return "l2prideicons.vesper_essence"; //Vesper Essence
    case 50024:
	    return "l2prideicons.titanium_rune"; //Titanium Essence
    case 50025:
	    return "l2prideicons.dread_rune"; //Vesper Essence
    case 50026:
	    return "l2prideicons.rykros_dust"; //Rykros Dust
    case 50027:
	    return "icon.etc_crest_red_i00"; //Instance Scroll
    case 50028:
	    return "l2prideicons.legendary_deed";
    case 50029:
	    return "l2prideicons.forbidden_deed";
    case 50100:
	    return "l2prideicons.iron_bar"; //Iron Bar
    case 50101:
	    return "l2prideicons.silver_bar"; //Silver Bar
    case 50102:
	    return "l2prideicons.gold_bar"; //Gold Bar
    case 50103:
	    return "l2prideicons.chrysolite_bar"; //Chrysolite Bar
    case 50104:
	    return "l2prideicons.damascus_bar"; //Damascus Bar
    case 50105:
	    return "l2prideicons.mithril_bar"; //Mithril Bar
    case 50106:
	    return "l2prideicons.adamantium_bar"; //Adamantium Bar
    case 50107:
	    return "l2prideicons.titanium_bar"; //Titanium Bar
    case 50108:
	    return "l2prideicons.meteoric_bar"; //Meteoric Bar
    case 50109:
	    return "l2prideicons.eternium_bar"; //Eternium Bar
    case 50110:
	    return "l2prideicons.dread_bar"; //Dread Bar
    case 50200:
	    return "l2prideicons.diamond";
    case 50201:
	    return "l2prideicons.ruby";
    case 50202:
	    return "l2prideicons.sapphire";
    case 50203:
	    return "l2prideicons.emerald";
    case 50204:
	    return "l2prideicons.amethyst";
    case 50205:
	    return "l2prideicons.citrine";
    case 50206:
	    return "l2prideicons.zircon";
    case 50207:
	    return "l2prideicons.pearl";
    case 50208:
	    return "l2prideicons.garnet";
    case 50209:
	    return "l2prideicons.jade";
    case 60004:
	    return "icon.etc_broken_crystal_silver_i00"; //Pure Silver..
    case 60008:
	    return "icon.etc_add_buffslot_i03";
    case 70000:
		return "icon.accessary_dragon_necklace_i00"; //Valakas Fiery
    case 70001:
		return "icon.accessary_sanddragons_earing_i00"; //Antharas Tremor
    case 70002:
		return "icon.accessary_ring_of_aurakyria_i00"; //Baium's Anger
    case 70003:
		return "icon.accessary_nassens_earing_i00"; //Zaken's Dementia
    case 70004:
		return "icon.accessary_ring_of_blessing_i00"; //Queen's Grasp
    case 70005:
		return "icon.accessary_another_worlds_necklace_i00"; //Frintezza's Phylactery
    case 70006:
		return "l2prideicons.exile"; //Exile
    case 70007:
		return "l2prideicons.soulscream"; //Soulscream
    case 70008:
		return "l2prideicons.fortified"; //Fortified
    case 77005:
		return "icon.amor_goodness_cloak"; //Custom Cloak
    case 77007:
		return "icon.amor_goodness_cloak"; //Custom Cloak
    case 77009:
		return "icon.amor_goodness_cloak"; //Custom Cloak
    case 77010:
		return "icon.amor_goodness_cloak"; //Custom Cloak
    case 77011:
		return "icon.amor_goodness_cloak"; //Custom Cloak
    case 80105:
		return "aionweapontex.bw_r012_i00"; //Arlefane
    case 80116:
		return "ancientwps.ancient_sls_i00"; //kurenai
    case 81017:
		return "icon.weapon_samurai_longsword_i00"; //osafune
    case 81116:
		return "aionweapontex.da_r010_i00"; //Life Deleter
    case 82100:
		return "c3.godsblade_icon"; //gods blade
    case 82101:
		return "c3.dragons_tooth_i00"; //dragon's tooth
    case 91000:
	    return "L2PrideHonorIcons.lidia_1";
    case 91001:
	    return "L2PrideHonorIcons.dread_bane";
    case 91002:
	    return "L2PrideHonorIcons.halisha";
    case 91003:
	    return "L2PrideHonorIcons.kukri";
    case 91004:
	    return "L2PrideHonorIcons.flesh_ripper";
    case 91005:
	    return "L2PrideHonorIcons.vampire_sword";
    case 91006:
	    return "L2PrideHonorIcons.ketra_axe";
    case 91007:
	    return "L2PrideHonorIcons.dusk";
    case 91009:
	    return "L2PrideHonorIcons.tomb_savant";
    case 91010:
	    return "icon.weapon_dual_sword_i00";
    case 91011:
	    return "L2PrideHonorIcons.nephilim_lance";
    case 91012:
	    return "L2PrideHonorIcons.grail_bow";
    case 91013:
	    return "wolverineclawbychandy.wolverine_griffe_i01";
    case 91014:
	    return "L2PrideHonorIcons.death_reaver";
    case 91015:
	    return "L2PrideHonorIcons.soul_linker";
    case 91016:
	    return "L2PrideHonorIcons.clandestine";
    case 91017:
	    return "L2PrideHonorIcons.drac_firebrand";
    case 91018:
	    return "L2PrideHonorIcons.drac_shield";
    case 91019:
	    return "L2PrideHonorIcons.apostle_sword";
    case 91020:
	    return "L2PrideHonorIcons.dusk_shield";
    case 91021:
	    return "L2PrideHonorIcons.imperial_war_shield";
    case 91022:
	    return "L2PrideHonorIcons.monk_shield";
    case 91023:
	    return "L2PrideHonorIcons.grand_ravager";
    case 91024:
	    return "L2PrideHonorIcons.obli_pike";
    case 91025:
	    return "darion.icons";
    case 91026:
	    return "darion.icon";
    case 91027:
	    return "L2PrideHonorIcons.silenos_staff";
    case 91028:
	    return "icon.weapon_dual_sword_i00";
    case 91050:
	    return "L2PrideHonorIcons.gods_blade";
    case 98020:
	    return "L2PrideHonorIcons.pvp_point";
    case 98021:
	    return "L2PrideHonorIcons.pvp_final";
    case 98022:
	    return "icon.etc_bloodpledge_point_i00"; //clan rep
    case 99000:
	    return "icon.etc_magic_coin_blood_i00";
    case 99001:
	    return "icon.etc_bereths_blood_dragon_i00";
    case 99002:
	    return "icon.etc_magic_coin_silver_i00";
    case 99003:
	    return "icon.etc_bereths_silver_dragon_i00";
    case 99004:
	    return "icon.etc_magic_coin_gold_i00";
    case 99005:
	    return "icon.etc_bereths_gold_dragon_i00";	
    case 99900:
	    return "L2PrideHonorIcons.s_rare_pack";
    case 99901:
	    return "L2PrideHonorIcons.s80_rare_pack";
    case 99903:
	    return "L2PrideHonorIcons.epic_pack";
    case 99912:
	    return "icon.etc_pi_gift_box_i01";
    case 99914:
	    return "L2PrideHonorIcons.gemstone_pack";
    case 99950:
	    return "icon.etc_blessed_scrl_of_ench_wp_d_i01";
    case 99951:
	    return "icon.etc_blessed_scrl_of_ench_am_d_i01";
    case 99954:
	    return "icon.etc_blessed_scrl_of_ench_wp_s_i05";
    case 99955:
	    return "icon.etc_blessed_scrl_of_ench_am_s_i05";
    case 99975:
	    return "icon.scroll_of_verification_i05";
    case 99976:
	    return "icon.etc_charm_of_courage_i02";
    case 99977:
	    return "icon.etc_charm_of_courage_i01";
    case 99978:
	    return "icon.etc_charm_of_courage_i03";
    case 99979:
	    return "icon.etc_charm_of_courage_i04";
    case 99980:
	    return "icon.skill0116";
    case 99981:
	    return "icon.skill0763";
    case 99982:
	    return "icon.Weapon_SS10";
    case 99983:
	    return "icon.Item_Normal92";
    case 99987:
	    return "icon.stone_10_i00";
    case 99989:
	    return "icon.scroll_of_verification_i06";
    case 99990:
	    return "icon.etc_charm_of_courage_i05";
    case 99991:
	    return "icon.etc_exp_point_i00";
    case 99992:
	    return "icon.etc_coke_i00";
    case 99993:
	    return "icon.etc_bloodpledge_point_i00";
    case 99994:
	    return "icon.etc_quest_pkcount_reward_i00";
    case 99995:
	    return "icon.turkey_buff_i00";
    case 99996:
	    return "icon.etc_figure_FELF_i00";
    case 99997:
	    return "icon.etc_quest_subclass_reward_i00";
    case 99998:
	    return "icon.etc_royal_membership_i00";	
    case 99999:
	    return "icon.etc_scroll_of_memory_i00";
    default:
	    return "icon.item_system03";
    }
}*/

/**
 * Returns the characteristics of the item
 * @return L2Item
 */
public L2Item getItem()
{
	return _item;
}

public boolean isEnchantable()
{
	return _item.isEnchantable();
}

public int getCustomType1()
{
	return _type1;
}
public int getCustomType2()
{
	return _type2;
}
public void setCustomType1(int newtype)
{
	_type1=newtype;
}
public void setCustomType2(int newtype)
{
	_type2=newtype;
}
public void setDropTime(long time)
{
	_dropTime=time;
}
public long getDropTime()
{
	return _dropTime;
}

public boolean isWear()
{
	return _wear;
}

public void setWear(boolean newwear)
{
	_wear=newwear;
}
/**
 * Returns the type of item
 * @return Enum
 */
@SuppressWarnings("rawtypes")
public Enum getItemType()
{
	return _item.getItemType();
}

/**
 * Returns the ID of the item
 * @return int
 */
public int getItemId()
{
	return _itemId;
}

/**
 * Returns true if item is an EtcItem
 * @return boolean
 */
public boolean isEtcItem()
{
	return (_item instanceof L2EtcItem);
}

/**
 * Returns true if item is a Weapon/Shield
 * @return boolean
 */
public boolean isWeapon()
{
	return (_item instanceof L2Weapon);
}

/**
 * Returns true if item is an Armor
 * @return boolean
 */
public boolean isArmor()
{
	return (_item instanceof L2Armor);
}

/**
 * Returns the characteristics of the L2EtcItem
 * @return L2EtcItem
 */
public L2EtcItem getEtcItem()
{
	if (_item instanceof L2EtcItem)
	{
		return (L2EtcItem) _item;
	}
	return null;
}

/**
 * Returns the characteristics of the L2Weapon
 * @return L2Weapon
 */
public L2Weapon getWeaponItem()
{
	if (_item instanceof L2Weapon)
	{
		return (L2Weapon) _item;
	}
	return null;
}

/**
 * Returns the characteristics of the L2Armor
 * @return L2Armor
 */
public L2Armor getArmorItem()
{
	if (_item instanceof L2Armor)
	{
		return (L2Armor) _item;
	}
	return null;
}

/**
 * Returns the quantity of crystals for crystallization
 * 
 * @return int
 */
public final int getCrystalCount()
{
	return _item.getCrystalCount(_enchantLevel);
}

public final int getCrystalType()
{
	return _item.getCrystalType();
}

/**
 * Returns the reference price of the item
 * @return int
 */
public int getReferencePrice()
{
	return _item.getReferencePrice();
}

/**
 * Returns the name of the item
 * @return String
 */
public String getItemName()
{
	return _item.getName();
}

/**
 * Returns the last change of the item
 * @return int
 */
public int getLastChange()
{
	return _lastChange;
}

/**
 * Sets the last change of the item
 * @param lastChange : int
 */
public void setLastChange(int lastChange)
{
	_lastChange = lastChange;
}

/**
 * Returns if item is stackable
 * @return boolean
 */
public boolean isStackable()
{
	return _item.isStackable();
}

/**
 * Returns if item is dropable
 * @return boolean
 */
public boolean isDropable()
{
	if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
		return false;
	
	return (isAugmented() && _augmentation.getSkill() != null) ? false : _item.isDropable();
}

public boolean isDropableKarma()
{
	if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
		return false;
	
	return _item.isDropable();
}

/**
 * Returns if item is destroyable
 * @return boolean
 */
public boolean isDestroyable()
{
	return _item.isDestroyable();
}

/**
 * set the timer to when the item can be traded/if ever
 * 
 */
public void setUntradeableTimer(final long timeInMilis)
{
	if (_untradeableTime == timeInMilis)
		return;
	
	_untradeableTime = timeInMilis;
	_storedInDb = false;
}

/**
 * Returns if item is tradeable
 * @return boolean
 */
public boolean isTradeable()
{
	if (_untradeableTime > 0 && _untradeableTime > System.currentTimeMillis())
		return false;
	
	if (isTimeLimitedItem())
		return false;
	
	if (_elementals != null)
	{
		if (isWeapon())
		{
			if (_elementals.getValue() >= 185)
				return false;
		}
		else
		{
			if (_elementals.getValue() >= 84)
				return false;
		}
	}
	
	if (_instanceDroppedFrom != null)
	{
		if (getUniqueness() > 3)
		{
			final L2PcInstance player = ((L2PcInstance)L2World.getInstance().findObject(getOwnerId()));
			
			if (player != null)
			{
				StringTokenizer st = new StringTokenizer(_instanceDroppedFrom, ";");
				
				try
				{
					int id = Integer.valueOf(st.nextToken());
					int id2 = Integer.valueOf(st.nextToken());
					
					if (id2 > 2000) //greater than kamaloka
					{
						if (player.getInstanceId() != id)
							return false;
					}
				}
				catch (NumberFormatException e)
				{
				}
			}
		}
	}
	
	return (isAugmented() && _augmentation.getSkill() != null) ? false : _item.isTradeable();
}

public boolean isFreightable()
{
	if (isTimeLimitedItem())
		return false;
	
	if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
		return false;
	
	return (isAugmented() && _augmentation.getSkill() != null) ? false : true;
}

/**
 * Returns if item is sellable
 * @return boolean
 */
public boolean isSellable()
{
	return (isAugmented() && _augmentation.getSkill() != null) ? false : _item.isSellable();
}

/**
 * Returns if item can be deposited in warehouse or freight
 * @return boolean
 */
public boolean isDepositable(boolean isPrivateWareHouse)
{
	// equipped, hero and quest items
	if (isEquipped() || isHeroItem() || isCastleItem() || _item.getItemType() == L2EtcItemType.QUEST || !_item.isDepositable())
		return false;
	// Staff of Master Yogi
	if (_itemId == 13539)
		return false;
	
	if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
		return false;
	
	if (!isPrivateWareHouse)
	{
		// augmented not tradeable
		if (!isTradeable() || isShadowItem())
			return false;
	}
	
	return true;
}

public boolean isDepositableFreight()
{
	// equipped, hero and quest items
	if (isEquipped() || isHeroItem() || isCastleItem() || _item.getItemType() == L2EtcItemType.QUEST)
		return false;
	// Staff of Master Yogi
	if (_itemId == 13539)
		return false;
	
	if (isStackable() && getUntradeableTime() > System.currentTimeMillis())
		return false;
	
	// augmented not tradeable
	if (!isFreightable() || isShadowItem())
		return false;
	
	return true;
}

/**
 * Returns if item is consumable
 * @return boolean
 */
public boolean isConsumable()
{
	return _item.isConsumable();
}

public boolean isHeroItem()
{
	return _item.isHeroItem();
}

public boolean isCastleItem()
{
	return _item.isCastleItem();
}

public boolean isCommonItem()
{
	return _item.isCommon();
}

final public boolean isOlyRestrictedItem()
{
	final L2Item item = getItem();
	
	if (item == null) return true;
	
	if (isARestrictedItem()) return true;
	
	if (item instanceof L2Armor || item instanceof L2Weapon)
	{
		if (!isAugmented() && item.getCrystalType() <= L2Item.CRYSTAL_S && item.getCrystalType() >= L2Item.CRYSTAL_B && getEnchantLevel() < 25 && item.getUniqueness() < 1)
		{
			if (!Config.LIST_OLY_RESTRICTED_ITEMS.contains(_itemId))
				return false;
		}
	}
	
	return true;
}

/**
 * Returns if item is available for manipulation
 * @return boolean
 */
public boolean isAvailable(L2PcInstance player, boolean allowAdena, boolean allowNonTradeable)
{
	if (player == null) return false;
	
	if (getItemId() == L2Item.DONATION_TOKEN && player.getAccessLevel().getLevel() < 231)
		return false;
	
	return ((!isEquipped()) // Not equipped
			&& (getItem().getType2() != 3) // Not Quest Item
			&& (getItem().getType2() != 4 || getItem().getType1() != 1) // TODO: what does this mean?
			&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
			&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
			&& (allowAdena || getItemId() != 57) // Not adena
			&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId())
			&& (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId())
			&& (allowNonTradeable || isTradeable())
			&& !isHeroItem()
			);
}

public boolean isAvailableFreight(L2PcInstance player)
{
	if (player == null) return false;
	
	if (getItemId() == L2Item.DONATION_TOKEN && player.getAccessLevel().getLevel() < 231)
		return false;
	
	return ((!isEquipped()) // Not equipped
			&& (getItem().getType2() != 3) // Not Quest Item
			&& (getItem().getType2() != 4 || getItem().getType1() != 1) // TODO: what does this mean?
			&& (player.getPet() == null || getObjectId() != player.getPet().getControlItemId()) // Not Control item of currently summoned pet
			&& (player.getActiveEnchantItem() != this) // Not momentarily used enchant scroll
			&& (player.getCurrentSkill() == null || player.getCurrentSkill().getSkill().getItemConsumeId() != getItemId())
			&& (!player.isCastingSimultaneouslyNow() || player.getLastSimultaneousSkillCast() == null || player.getLastSimultaneousSkillCast().getItemConsumeId() != getItemId())
			&& (isFreightable())
			);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.model.L2Object#onAction(net.sf.l2j.gameserver.model.L2PcInstance)
 * also check constraints: only soloing castle owners may pick up mercenary tickets of their castle
 */
@Override
public void onAction(L2PcInstance player)
{
	// this causes the validate position handler to do the pickup if the location is reached.
	// mercenary tickets can only be picked up by the castle owner.
	int castleId = MercTicketManager.getInstance().getTicketCastleId(_itemId);
	
	if (castleId > 0 &&
			(!player.isCastleLord(castleId) || player.isInParty()))
	{
		if  (player.isInParty())    //do not allow owner who is in party to pick tickets up
			player.sendMessage("You cannot pickup mercenaries while in a party.");
		else
			player.sendMessage("Only the castle lord can pickup mercenaries.");
		
		player.setTarget(this);
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else if (player.isFlying()) // cannot pickup
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else
		player.getAI().setIntention(CtrlIntention.AI_INTENTION_PICK_UP, this);
}
/**
 * Returns the level of enchantment of the item
 * @return int
 */
public int getEnchantLevel()
{
	return _enchantLevel;
}

/**
 * Sets the level of enchantment of the item
 * @param int
 */
public void setEnchantLevel(int enchantLevel)
{
	if (_enchantLevel == enchantLevel)
		return;
	_enchantLevel = enchantLevel;
	_storedInDb = false;
}

/**
 * Returns the physical defense of the item
 * @return int
 */
public int getPDef()
{
	if (_item instanceof L2Armor)
		return ((L2Armor)_item).getPDef();
	return 0;
}

/**
 * Returns whether this item is augmented or not
 * @return true if augmented
 */
public boolean isAugmented()
{
	return _augmentation == null ? false : true;
}

/**
 * Returns the augmentation object for this item
 * @return augmentation
 */
public L2Augmentation getAugmentation()
{
	return _augmentation;
}

/**
 * Sets a new augmentation
 * @param augmentation
 * @return return true if sucessfull
 */
public boolean setAugmentation(L2Augmentation augmentation)
{
	// there shall be no previous augmentation..
	if (_augmentation != null)
		return false;
	_augmentation = augmentation;
	updateItemAttributes();
	return true;
}

/**
 * Remove the augmentation
 *
 */
public void removeAugmentation()
{
	if (_augmentation == null)
		return;
	_augmentation = null;
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = null;
		if (_elementals != null)
		{
			// Item still has elemental enchant, only update the DB
			statement = con.prepareStatement("UPDATE item_attributes SET augAttributes = -1, augSkillId = -1, augSkillLevel = -1 WHERE itemId = ?");
		}
		else
		{
			// Remove the entry since the item also has no elemental enchant
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
		}
		
		statement.setInt(1, getObjectId());
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not remove augmentation for item: "+getObjectId()+" from DB:", e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

public void restoreAttributes()
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT augAttributes,augSkillId,augSkillLevel,elemType,elemValue FROM item_attributes WHERE itemId=?");
		statement.setInt(1, getObjectId());
		ResultSet rs = statement.executeQuery();
		rs = statement.executeQuery();
		if (rs.next())
		{
			int aug_attributes = rs.getInt(1);
			int aug_skillId = rs.getInt(2);
			int aug_skillLevel = rs.getInt(3);
			byte elem_type = rs.getByte(4);
			int elem_value = rs.getInt(5);
			if (elem_type != -1 && elem_value != -1)
				_elementals = new Elementals(elem_type, elem_value);
			if (aug_attributes != -1 && aug_skillId != -1 && aug_skillLevel != -1)
				_augmentation = new L2Augmentation(rs.getInt("augAttributes"), rs.getInt("augSkillId"), rs.getInt("augSkillLevel"));
		}
		rs.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not restore augmentation and elemental data for item " + getObjectId() + " from DB: "+e.getMessage(), e);
	}
	finally
	{
		try
		{
			con.close();
		}
		catch (Exception e)
		{
		}
	}
}

public void updateItemAttributes()
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("REPLACE INTO item_attributes VALUES(?,?,?,?,?,?)");
		statement.setInt(1, getObjectId());
		if (_augmentation == null)
		{
			statement.setInt(2, -1);
			statement.setInt(3, -1);
			statement.setInt(4, -1);
		}
		else
		{
			statement.setInt(2, _augmentation.getAttributes());
			if(_augmentation.getSkill() == null)
			{
				statement.setInt(3, 0);
				statement.setInt(4, 0);
			}
			else
			{
				statement.setInt(3, _augmentation.getSkill().getId());
				statement.setInt(4, _augmentation.getSkill().getLevel());
			}
		}
		if (_elementals == null)
		{
			statement.setByte(5, (byte) -1);
			statement.setInt(6, -1);
		}
		else
		{
			statement.setByte(5, _elementals.getElement());
			statement.setInt(6, _elementals.getValue());
		}
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not remove elemental enchant for item: "+getObjectId()+" from DB:", e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

public Elementals getElementals()
{
	return _elementals;
}

public byte getAttackElementType()
{
	if ((isWeapon() || getItemId() == 14164) && _elementals != null)
		return _elementals.getElement();
	return -2;
}

public int getAttackElementPower()
{
	if ((isWeapon() || getItemId() == 14164) && _elementals != null)
		return _elementals.getValue();
	return 0;
}

public int getElementDefAttr(byte element)
{
	if (isArmor() && _elementals != null && _elementals.getElement() == element)
		return _elementals.getValue();
	return 0;
}

public void setElementAttr(byte element, int value)
{
	if (_elementals == null)
	{
		_elementals = new Elementals(element, value);
	}
	else
	{
		_elementals.setElement(element);
		_elementals.setValue(value);
	}
	updateItemAttributes();
}

public void clearElementAttr()
{
	if (_elementals != null)
	{
		_elementals = null;
	}
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		
		PreparedStatement statement = null;
		if (_augmentation != null)
		{
			// Item still has augmentation, only update the DB
			statement = con.prepareStatement("UPDATE item_attributes SET elemType = -1, elemValue = -1 WHERE itemId = ?");
		}
		else
		{
			// Remove the entry since the item also has no augmentation
			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
		}
		
		statement.setInt(1, getObjectId());
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not remove elemental enchant for item: "+getObjectId()+" from DB:", e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
}

/**
 * Used to decrease mana
 * (mana means life time for shadow items)
 */
public class ScheduleConsumeManaTask implements Runnable
{
private final L2ItemInstance _shadowItem;

public ScheduleConsumeManaTask(L2ItemInstance item)
{
	_shadowItem = item;
}

public void run()
{
	try
	{
		// decrease mana
		if (_shadowItem != null)
			_shadowItem.decreaseMana(true);
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}


/**
 * Returns true if this item is a shadow item
 * Shadow items have a limited life-time
 * @return
 */
public boolean isShadowItem()
{
	return (_mana >= 0);
}

/**
 * Returns the remaining mana of this shadow item
 * @return lifeTime
 */
public int getMana()
{
	return _mana;
}

/**
 * Decreases the mana of this shadow item,
 * sends a inventory update
 * schedules a new consumption task if non is running
 * optionally one could force a new task
 * @param forces a new consumption task if item is equipped
 */
public void decreaseMana(boolean resetConsumingMana)
{
	if (!isShadowItem()) return;
	
	if (_mana > 0) _mana--;
	
	if (_storedInDb) _storedInDb = false;
	if (resetConsumingMana) _consumingMana = false;
	
	final L2PcInstance player = ((L2PcInstance)L2World.getInstance().findObject(getOwnerId()));
	if (player != null)
	{
		SystemMessage sm;
		switch (_mana)
		{
		case 10:
			sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_10);
			sm.addItemName(_item);
			player.sendPacket(sm);
			break;
		case 5:
			sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_5);
			sm.addItemName(_item);
			player.sendPacket(sm);
			break;
		case 1:
			sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_1);
			sm.addItemName(_item);
			player.sendPacket(sm);
			break;
		}
		
		if (_mana == 0) // The life time has expired
		{
			sm = new SystemMessage(SystemMessageId.S1S_REMAINING_MANA_IS_NOW_0);
			sm.addItemName(_item);
			player.sendPacket(sm);
			
			// unequip
			if (isEquipped())
			{
				L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
				InventoryUpdate iu = new InventoryUpdate();
				for (L2ItemInstance item: unequiped)
				{
					iu.addModifiedItem(item);
				}
				player.sendPacket(iu);
			}
			
			if (getLocation() != ItemLocation.WAREHOUSE)
			{
				// destroy
				player.getInventory().destroyItem("L2ItemInstance", this, player, null);
				
				// send update
				InventoryUpdate iu = new InventoryUpdate();
				iu.addRemovedItem(this);
				player.sendPacket(iu);
			}
			else
			{
				player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
			}
			
			// delete from world
			L2World.getInstance().removeObject(this);
		}
		else
		{
			// Reschedule if still equipped
			if (!_consumingMana && isEquipped())
			{
				scheduleConsumeManaTask();
			}
			if (getLocation() != ItemLocation.WAREHOUSE)
			{
				InventoryUpdate iu = new InventoryUpdate();
				iu.addModifiedItem(this);
				player.sendPacket(iu);
			}
		}
	}
}

public void scheduleConsumeManaTask()
{
	_consumingMana = true;
	ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleConsumeManaTask(this), MANA_CONSUMPTION_RATE);
}

/**
 * Returns false cause item can't be attacked
 * @return boolean false
 */
@Override
public boolean isAutoAttackable(L2Character attacker)
{
	return false;
}

public boolean getChargedFishshot()
{
	return _chargedFishtshot;
}

public void setChargedFishshot(boolean type)
{
	_chargedFishtshot = type;
}

/**
 * This function basically returns a set of functions from
 * L2Item/L2Armor/L2Weapon, but may add additional
 * functions, if this particular item instance is enhanched
 * for a particular player.
 * @param player : L2Character designating the player
 * @return Func[]
 */
public Func[] getStatFuncs(L2Character player)
{
	return getItem().getStatFuncs(this, player);
}

/**
 * Updates the database.<BR>
 */
public void updateDatabase()
{
	updateDatabase(false);
}

/**
 * Updates the database.<BR>
 * 
 * @param force if the update should necessarilly be done.
 */
public void updateDatabase(boolean force)
{
	if (isWear()) //avoid saving weared items
	{
		return;
	}
	
	_dbLock.lock();
	try
	{
		if (_existsInDb)
		{
			if (_ownerId == 0 || _loc == ItemLocation.VOID || (getCount() == 0 && _loc != ItemLocation.LEASE))
			{
				removeFromDb();
			}
			else if (!Config.LAZY_ITEMS_UPDATE || force)
			{
				updateInDb();
			}
		}
		else
		{
			if (getCount() == 0 && _loc != ItemLocation.LEASE)
			{
				return;
			}
			if (_loc == ItemLocation.VOID || _loc == ItemLocation.NPC || _ownerId == 0)
			{
				return;
			}
			insertIntoDb();
		}
	}
	finally
	{
		_dbLock.unlock();
	}
}

/**
 * Returns a L2ItemInstance stored in database from its objectID
 * @param objectId : int designating the objectID of the item
 * @return L2ItemInstance
 */
public static L2ItemInstance restoreFromDb(int ownerId, ResultSet rs)
{
	L2ItemInstance inst = null;
	int objectId, item_id, loc_data, enchant_level, custom_type1, custom_type2, manaLeft;
	long time, count, tradetime;
	String instanceFrom;
	ItemLocation loc;
	try
	{
		objectId = rs.getInt(1);
		item_id = rs.getInt("item_id");
		count = rs.getLong("count");
		loc = ItemLocation.valueOf(rs.getString("loc"));
		loc_data = rs.getInt("loc_data");
		enchant_level = rs.getInt("enchant_level");
		custom_type1 =  rs.getInt("custom_type1");
		custom_type2 =  rs.getInt("custom_type2");
		manaLeft = rs.getInt("mana_left");
		time = rs.getLong("time");
		tradetime = rs.getLong("trade_time");
		instanceFrom = rs.getString("instance");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not restore an item owned by "+ownerId+" from DB:", e);
		return null;
	}
	L2Item item = ItemTable.getInstance().getTemplate(item_id);
	if (item == null
			) {
		_log.severe("Item item_id="+item_id+" not known, object_id="+objectId);
		return null;
	}
	inst = new L2ItemInstance(objectId, item);
	inst._ownerId = ownerId;
	
	inst._enchantLevel = enchant_level;
	inst._type1 = custom_type1;
	inst._type2 = custom_type2;
	
	/*if (inst.isScroll())
	{
		inst.setCount(Math.min(MAX_SCROLL, count));
	}
	else*/
		inst.setCount(count);
	
	inst._loc = loc;
	inst._locData = loc_data;
	inst._existsInDb = true;
	inst._storedInDb = true;
	inst._instanceDroppedFrom = instanceFrom;
	
	// Setup life time for shadow weapons
	inst._mana = manaLeft;
	inst._time = time;
	inst._untradeableTime = tradetime;
	
	// consume 1 mana
	if (inst.isShadowItem() && inst.isEquipped())
	{
		inst.decreaseMana(false);
		// if player still not loaded and not found in the world - force task creation
		inst.scheduleConsumeManaTask();
	}
	
	if (inst.isTimeLimitedItem())
		inst.scheduleLifeTimeTask();
	
	//load augmentation and elemental enchant
	if (inst.isEquipable())
	{
		inst.restoreAttributes();
	}
	
	return inst;
}

/**
 * Init a dropped L2ItemInstance and add it in the world as a visible object.<BR><BR>
 *
 * <B><U> Actions</U> :</B><BR><BR>
 * <li>Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion </li>
 * <li>Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion</li>
 * <li>Add the L2ItemInstance dropped in the world as a <B>visible</B> object</li><BR><BR>
 *
 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T ADD the object to _allObjects of L2World </B></FONT><BR><BR>
 *
 * <B><U> Assert </U> :</B><BR><BR>
 * <li> _worldRegion == null <I>(L2Object is invisible at the beginning)</I></li><BR><BR>
 *
 * <B><U> Example of use </U> :</B><BR><BR>
 * <li> Drop item</li>
 * <li> Call Pet</li><BR>
 *
 */
public class doItemDropTask implements Runnable
{
private int _x,_y,_z;
private final L2Character _dropper;
private final L2ItemInstance _itm;

public doItemDropTask(L2ItemInstance item, L2Character dropper, int x, int y, int z)
{
	_x = x;
	_y = y;
	_z = z;
	_dropper = dropper;
	_itm = item;
}

public final void run()
{
	if (Config.ASSERT)
		assert _itm.getPosition().getWorldRegion() == null;
	
	if (Config.GEODATA > 0 && _dropper != null)
	{
		Location dropDest = GeoData.getInstance().moveCheck(_dropper.getX(), _dropper.getY(), _dropper.getZ(), _x, _y, _z, _dropper.getInstanceId());
		_x = dropDest.getX();
		_y = dropDest.getY();
		_z = dropDest.getZ();
	}
	
	if(_dropper != null)
		setInstanceId(_dropper.getInstanceId()); // Inherit instancezone when dropped in visible world
	else
		setInstanceId(0); // No dropper? Make it a global item...
	
	synchronized (_itm)
	{
		// Set the x,y,z position of the L2ItemInstance dropped and update its _worldregion
		_itm.setIsVisible(true);
		_itm.getPosition().setWorldPosition(_x, _y ,_z);
		_itm.getPosition().setWorldRegion(L2World.getInstance().getRegion(getPosition().getWorldPosition()));
		
		// Add the L2ItemInstance dropped to _visibleObjects of its L2WorldRegion
	}
	
	_itm.getPosition().getWorldRegion().addVisibleObject(_itm);
	_itm.setDropTime(System.currentTimeMillis());
	_itm.setDropperObjectId(_dropper != null ? _dropper.getObjectId() : 0); //Set the dropper Id for the knownlist packets in sendInfo
	
	// this can synchronize on others instancies, so it's out of
	// synchronized, to avoid deadlocks
	// Add the L2ItemInstance dropped in the world as a visible object
	L2World.getInstance().addVisibleObject(_itm, _itm.getPosition().getWorldRegion());
	if (Config.SAVE_DROPPED_ITEM)
		ItemsOnGroundManager.getInstance().save(_itm);
	_itm.setDropperObjectId(0); //Set the dropper Id back to 0 so it no longer shows the drop packet
}
}
public final void dropMe(L2Character dropper, int x, int y, int z)
{
	ThreadPoolManager.getInstance().executeTask(new doItemDropTask(this, dropper, x, y, z));
}

/**
 * Update the database with values of the item
 */
private void updateInDb()
{
	if (Config.ASSERT)
		assert _existsInDb;
	
	if (_wear)
		return;
	
	if (_storedInDb)
		return;
	
	Connection con = null;
	PreparedStatement statement = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement(
				"UPDATE items SET owner_id=?,count=?,loc=?,loc_data=?,enchant_level=?,custom_type1=?,custom_type2=?,mana_left=?,time=?,trade_time=? " +
				"WHERE object_id = ?");
		statement.setInt(1, _ownerId);
		statement.setLong(2, getCount());
		statement.setString(3, _loc.name());
		statement.setInt(4, _locData);
		statement.setInt(5, getEnchantLevel());
		statement.setInt(6, getCustomType1());
		statement.setInt(7, getCustomType2());
		statement.setInt(8, getMana());
		statement.setLong(9, getTime());
		statement.setLong(10, _untradeableTime);
		statement.setInt(11, getObjectId());
		statement.executeUpdate();
		_existsInDb = true;
		_storedInDb = true;
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not update item "+getObjectId()+" in DB: Reason: "+e.getMessage(), e);
	}
	finally
	{
		try
		{
			statement.close();
		} catch (Exception e)
		{
		}
		
		try
		{
			con.close();
		} catch (Exception e)
		{
		}
	}
}

/**
 * Insert the item in database
 */
private void insertIntoDb()
{
	if (_itemId == 8190 || _itemId == 8689)		return;
	
	if (_wear)	return;
	
	if (Config.ASSERT) assert !_existsInDb && getObjectId() != 0;
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement(
				"INSERT INTO items (owner_id,item_id,count,loc,loc_data,enchant_level,object_id,custom_type1,custom_type2,mana_left,time,trade_time,source,instance) " +
				"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		statement.setInt(1, _ownerId);
		statement.setInt(2, _itemId);
		statement.setLong(3, getCount());
		statement.setString(4, _loc.name());
		statement.setInt(5, _locData);
		statement.setInt(6, getEnchantLevel());
		statement.setInt(7, getObjectId());
		statement.setInt(8, _type1);
		statement.setInt(9, _type2);
		statement.setInt(10, getMana());
		statement.setLong(11, getTime());
		statement.setLong(12, _untradeableTime);
		statement.setString(13, _source);
		statement.setString(14, _instanceDroppedFrom);
		
		statement.executeUpdate();
		_existsInDb = true;
		_storedInDb = true;
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not insert item "+getObjectId()+" into DB: Reason: "+e.getMessage(), e);
	}
	finally
	{
		try { con.close(); } catch (Exception e) {}
	}
	
	if (_elementals != null)
		updateItemAttributes();
}

/**
 * Delete item from database
 */
private void removeFromDb()
{
	if (Config.ASSERT)
		assert _existsInDb;
	
	if (_wear)
		return;
	
	Connection con = null;
	PreparedStatement statement = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement("DELETE FROM items WHERE object_id=?");
		statement.setInt(1, getObjectId());
		statement.executeUpdate();
		_existsInDb = false;
		_storedInDb = false;
		statement.close();
		
		statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId = ?");
		statement.setInt(1, getObjectId());
		statement.executeUpdate();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Could not delete item "+getObjectId()+" in DB: "+e.getMessage(), e);
	}
	finally
	{
		try
		{
			statement.close();
		} catch (Exception e)
		{
		}
		
		try
		{
			con.close();
		} catch (Exception e)
		{
		}
	}
}

/**
 * Returns the item in String format
 * @return String
 */
@Override
public String toString()
{
	return ""+_item;
}
public void resetOwnerTimer()
{
	if(itemLootShedule != null)
		itemLootShedule.cancel(true);
	itemLootShedule = null;
}
public void setItemLootShedule(ScheduledFuture<?> sf)
{
	itemLootShedule = sf;
}
public ScheduledFuture<?> getItemLootShedule()
{
	return itemLootShedule;
}
public void setProtected(boolean is_protected)
{
	_protected = is_protected;
}
public boolean isProtected()
{
	return _protected;
}
public boolean isNightLure()
{
	return ((_itemId >= 8505 && _itemId <= 8513) || _itemId == 8485);
}

public void setCountDecrease(boolean decrease)
{
	_decrease = decrease;
}

public boolean getCountDecrease()
{
	return _decrease;
}

public void setInitCount(int InitCount)
{
	_initCount = InitCount;
}

public long getInitCount()
{
	return _initCount;
}

public void restoreInitCount()
{
	if(_decrease)
		setCount(_initCount);
}

public boolean isTimeLimitedItem()
{
	return (_time > 0);
}

/**
 * Returns (current system time + time) of this time limited item
 * @return Time
 */
public long getTime()
{
	return _time;
}

public long getRemainingTime()
{
	return _time - System.currentTimeMillis();
}
public void endOfLife()
{
	L2PcInstance player = ((L2PcInstance)L2World.getInstance().findObject(getOwnerId()));
	if (player != null)
	{
		if (isEquipped())
		{
			SystemMessage sm = null;
			
			if (getEnchantLevel() > 0)
			{
				sm = new SystemMessage(SystemMessageId.EQUIPMENT_S1_S2_REMOVED);
				sm.addNumber(getEnchantLevel());
				sm.addItemName(this);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_DISARMED);
				sm.addItemName(this);
			}
			
			player.sendPacket(sm);
			
			L2ItemInstance[] unequiped = player.getInventory().unEquipItemInSlotAndRecord(getLocationSlot());
			InventoryUpdate iu = new InventoryUpdate();
			for (L2ItemInstance item: unequiped)
			{
				iu.addModifiedItem(item);
			}
			player.sendPacket(iu);
			
			player.broadcastUserInfo();
		}
		
		if (getLocation() != ItemLocation.WAREHOUSE)
		{
			// destroy
			player.getInventory().destroyItem("L2ItemInstance", this, player, null);
			
			// send update
			InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(this);
			player.sendPacket(iu);
		}
		else
		{
			player.getWarehouse().destroyItem("L2ItemInstance", this, player, null);
		}
		player.sendPacket(new SystemMessage(SystemMessageId.TIME_LIMITED_ITEM_DELETED));
		// delete from world
		L2World.getInstance().removeObject(this);
	}
}

public void scheduleLifeTimeTask()
{
	if (!isTimeLimitedItem()) return;
	if (getRemainingTime() <= 0)
		endOfLife();
	else
	{
		if (_lifeTimeTask != null)
			_lifeTimeTask.cancel(false);
		_lifeTimeTask = ThreadPoolManager.getInstance().scheduleGeneral(new ScheduleLifeTimeTask(this), getRemainingTime());
	}
}

public class ScheduleLifeTimeTask implements Runnable
{
private final L2ItemInstance _limitedItem;

public ScheduleLifeTimeTask(L2ItemInstance item)
{
	_limitedItem = item;
}

public void run()
{
	try
	{
		if (_limitedItem != null)
			_limitedItem.endOfLife();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

public void updateElementAttrBonus(L2PcInstance player)
{
	if (_elementals == null)
		return;
	_elementals.updateBonus(player, isArmor() && getItemId() != 14164);
}

public void setDropperObjectId(int id)
{
	_dropperObjectId = id;
}

@Override
public void sendInfo(L2PcInstance activeChar)
{
	if (_dropperObjectId != 0)
		activeChar.sendPacket(new DropItem(this, _dropperObjectId));
	else
		activeChar.sendPacket(new SpawnItem(this));
}

public boolean isARestrictedItem()
{
	return (isHeroItem() || isCastleItem() || getItem().getUniqueness() >= 3 ||getCrystalType() > L2Item.CRYSTAL_S80 || getItem().getBodyPart() == L2Item.SLOT_BACK || getItem().getBodyPart() == L2Item.SLOT_BELT ||
			getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || getItem().getBodyPart() == L2Item.SLOT_DECO || getEnchantLevel() > 22);
}

public boolean isARestrictedItemZone(int limit)
{
	switch (limit)
	{
	default: //s
		if (isHairAccessory() && getUniqueness() != 1)
			return true;
		if (getUniqueness() > 2)
			return true;
		if (getCrystalType() > L2Item.CRYSTAL_S)
			return true;
		break;
	case 1: //s80
		if (isHairAccessory() && getUniqueness() > 1)
			return true;
		if (getUniqueness() > 3)
			return true;
		if (getCrystalType() > L2Item.CRYSTAL_S80)
			return true;
		break;
	case 2: //vesper
		if (isHairAccessory() && getUniqueness() >= 3.5)
			return true;
		if (getUniqueness() >= 3.5)
			return true;
		if (getCrystalType() > L2Item.CRYSTAL_S80 && !getName().contains("Vesper"))
			return true;
		break;
	case 3: //titanium up
		if (getUniqueness() > 4)
			return true;
		break;
	case 4: //dread up
		if (getUniqueness() > 4.5)
			return true;
		break;
	case 5: //how could this be
		if (getUniqueness() > 5)
			return true;
		break;
	}
	
	return false;
}

public boolean isARestrictedItemOrcArea()
{
	boolean rares = getItem().getUniqueness() >= 3;
	
	if (!getItem().isRaidbossItem() && getItem().getUniqueness() == 3 && ((getCrystalType() == L2Item.CRYSTAL_S) || (getCrystalType() == L2Item.CRYSTAL_S80)))
		rares = false;
	
	return (rares || isHeroItem() || isCastleItem() || getCrystalType() > L2Item.CRYSTAL_S80 || getItem().getBodyPart() == L2Item.SLOT_L_BRACELET || getItem().getBodyPart() == L2Item.SLOT_DECO /*|| getEnchantLevel() > 24*/
			|| (isHairAccessory() && getUniqueness() != 1));
}

public boolean isARestrictedItemFT()
{
	switch (getItem().getBodyPart())
	{
	case L2Item.SLOT_R_HAND:
	case L2Item.SLOT_L_HAND:
	case L2Item.SLOT_LR_HAND:
	case L2Item.SLOT_CHEST:
	case L2Item.SLOT_LEGS:
	case L2Item.SLOT_FULL_ARMOR:
	case L2Item.SLOT_FEET:
	case L2Item.SLOT_GLOVES:
	case L2Item.SLOT_HEAD:
		if (getCrystalType() <= L2Item.CRYSTAL_S && getEnchantLevel() < 25 && getUniqueness() < 2)
			return true;
	}
	
	return false;
}

public boolean isHairAccessory()
{
	return _item.isHairAccessory();
}

public long getUntradeableTime()
{
	return _untradeableTime;
}

public boolean isUntradeableAfterEquip()
{
	return getItem().isUntradeableAfterEquip() || isUntradeableAfterEquipEnchant();
}

public boolean isUntradeableAfterEquipEnchant()
{
	boolean vesper = getName().contains("Vesper") && getItemId() != 14163 && getItemId() != 14164 && getItemId() != 14165;
	
	if (!vesper)
	{
		vesper = getName().contains("Titanium") && isStandardShopItem() && getUniqueness() == 4;
	}
	
	return getEnchantLevel() >= getItem().getClutchEnchantLevel() + (vesper ? 1 : 0);
}

public boolean shouldBeNowSetAsTradeable()
{
	return !getItem().isUntradeableAfterEquip() && !isUntradeableAfterEquipEnchant() && getUntradeableTime() == 9999999900000L;
}

public final boolean isAtOrOverMustBreakEnchantLevel()
{
	if (getUniqueness() == 4.5)
	{
		if (isStandardShopItem())
			return getEnchantLevel() >= 11;
			else
				return getEnchantLevel() >= 11;
	}
	else if (getUniqueness() == 4)
	{
		if (isStandardShopItem())
			return getEnchantLevel() >= 14;
			else
				return getEnchantLevel() >= 14;
	}
	else if (getUniqueness() == 3.5)
	{
		if (isStandardShopItem())
			return getEnchantLevel() >= 16;
			else
				return getEnchantLevel() >= 15;
	}
	else if (getUniqueness() == 3)
	{
		if (isStandardShopItem())
			return getEnchantLevel() >= 18;
			else
			{
				if (getCrystalType() == L2Item.CRYSTAL_S)
					return getEnchantLevel() >= 17;
					else
						return
								getEnchantLevel() >= 16;
			}
	}
	else if (getUniqueness() == 2.5)
	{
		if (isStandardShopItem())
			return getEnchantLevel() >= 19;
			else
			{
				if (getCrystalType() == L2Item.CRYSTAL_S)
					return getEnchantLevel() >= 17;
					else
						return
								getEnchantLevel() >= 16;
			}
	}
	
	return false;
}

public boolean isStandardShopItem()
{
	return _item.isStandardShopItem();
}

public boolean isRaidbossItem()
{
	return _item.isRaidbossItem();
}

public int getStandardShopItem()
{
	return _item.getStandardShopItem();
}

public void addAutoAugmentation()
{
	if (AbstractRefinePacket.isValidAutoAugment(this))
	{
		int grade = 3; //top
		int level = 84; //lvl 84
		
		switch ((int)getItem().getUniqueness())
		{
		case 0:
			break;
		case 1:
			if (getName().contains("Icarus"))
				level = 80;
			else
				level = 82;
			break;
		case 2:
			if (getItem().getUniqueness() == 2.5)
				level = 76;
			else
				level = 82;
			break;
		case 3:
			if (Rnd.get(100) < 75)
			{
				level = 80;
				grade = 2;
			}
			else
			{
				level = 80;
			}
			break;
		default:
			level = 76;
			grade = 2;
			break;
		}
		
		if (!isStandardShopItem())
		{
			grade -= 1;
			level = 76;
		}
		
		if (getLocationSlot() == L2Item.SLOT_BACK)
		{
			grade -= 1;
			level -= 2;
		}
		
		final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(level, grade,
				getItem().getBodyPart(), isJewelry());
		setAugmentation(aug);
	}
}
public void addAutoAugmentationDonation()
{
	if (AbstractRefinePacket.isValidAutoAugment(this))
	{
		int grade = 3; //top
		int level = 84; //lvl 84
		
		switch ((int)getItem().getUniqueness())
		{
		case 0:
			break;
		case 1:
			if (getName().contains("Icarus"))
				level = 80;
			else
				level = 82;
			break;
		case 2:
			if (getItem().getUniqueness() == 2.5)
				level = 76;
			else
				level = 82;
			break;
		case 3:
			if (Rnd.get(100) < 66)
			{
				level = 82;
				grade = 2;
			}
			else
			{
				level = 80;
				grade = 3;
			}
			break;
		default:
			level = 76;
			grade = 2;
			break;
		}
		
		if (!isStandardShopItem())
		{
			grade = 2;
			level = 76;
		}
		
		if (getLocationSlot() == L2Item.SLOT_BACK)
		{
			grade -= 1;
			level -= 2;
		}
		
		final L2Augmentation aug = AugmentationData.getInstance().generateRandomAugmentation(level, grade,
				getItem().getBodyPart(), isJewelry());
		setAugmentation(aug);
	}
}

public int getSuperEnchantLevel()
{
	return _item.getSuperEnchantLevel();
}

public boolean attemptToIncreaseEnchantViaPVP(L2PcInstance player)
{
	int enchantlvl = getEnchantLevel();
	final float uniqueness = getUniqueness();
	int chance = 0;
	
	if (uniqueness == 0)
	{
		if (enchantlvl < 14)
			chance = 750;
		else if (enchantlvl < 16)
			chance = 500;
		else if (enchantlvl < 18)
			chance = 250;
		else if (enchantlvl < 20)
			chance = 60;
	}
	else if (uniqueness == 1)
	{
		if (enchantlvl < 15)
			chance = 150;
	}
	else if (uniqueness == 1.5) //masterwork
	{
		if (enchantlvl < 15)
			chance = 70;
	}
	else if (uniqueness == 2) //dynasty
	{
		if (enchantlvl < 16)
			chance = 50;
		else if (enchantlvl < 18)
			chance = 7;
	}
	else if (uniqueness == 2.5) //icarus
	{
		if (enchantlvl < 15)
			chance = 16;
	}
	else if (uniqueness == 3)  //rare weapons, vesper
	{
		if (enchantlvl < 10)
			chance = 15;
		else if (enchantlvl < 15)
		{
			if (getCrystalType() > L2Item.CRYSTAL_S)
			{
				if (!isStandardShopItem())
					chance = 2;
				else
				{
					switch (getItemId())
					{
					case 14163:
					case 14164:
					case 14165:
						if (enchantlvl >= 14)
							chance = 0;
						else
							chance = 3;
						break;
					default:
						chance = 7;
					}
				}
			}
			else
				chance = 6;
		}
	}
	else if (uniqueness == 3.5) //raidboss weapons
	{
		if (enchantlvl < 10)
			chance = 8;
		else if (enchantlvl < 14)
		{
			if (enchantlvl < 13)
			{
				if (Rnd.get(2) == 0)
					chance = 1;
				else
					chance = 0;
			}
			else
			{
				if (Rnd.get(5) == 0)
					chance = 1;
				else chance = 0;
			}
		}
	}
	else if (uniqueness == 4) //titanium, epics
	{
		if (isStandardShopItem())
		{
			if (enchantlvl < 9)
				chance = 6;
			else if (enchantlvl < 11)
				chance = 1;
		}
		else
		{
			if (enchantlvl < 10)
				chance = 3;
		}
	}
	else if (uniqueness == 4.5) //dread
	{
		if (enchantlvl < 7)
			chance = 1;
	}
	else if (uniqueness == 5)
	{
		if (enchantlvl < 5)
			chance = 1;
	}
	
	if (chance > 0 && (chance >= 1350 || Rnd.get(1350) < chance))
	{
		enchantlvl++;
		setEnchantLevel(enchantlvl);
		updateDatabase();
		
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2_SUCCESSFULLY_ENCHANTED);
		
		sm.addNumber(enchantlvl);
		sm.addItemName(this);
		player.sendPacket(sm);
		
		player.broadcastUserInfo();
		
		if (enchantlvl >= getItem().getClutchEnchantLevel())
		{
			Broadcast.toAllOnlinePlayers(SystemMessage.sendString(player.getName()+" has enchanted +"+enchantlvl+" "+getName()+" via PvPing"));
			
			// fireworks
			final L2Skill skill = SkillTable.getInstance().getInfo(2025,1);
			
			if (skill != null)
			{
				MagicSkillUse MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
				player.broadcastPacket(MSU);
				player.useMagic(skill, false, false);
			}
			
			RequestEnchantItem.auditEnchant(player, null, this, "Yes (PVP)");
		}
		
		return true;
	}
	
	return false;
}

public float getUniqueness()
{
	return _item.getUniqueness();
}

public boolean isJewelry()
{
	return _item.isJewelry();
}

public void setInstanceDroppedFrom(String string)
{
	_instanceDroppedFrom = string;
}

public boolean isDread()
{
	return _item.isDread();
}

public boolean isStarterItem()
{
	return getUntradeableTime() == 9999999900004L;
}
}