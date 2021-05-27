package net.sf.l2j.gameserver.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.util.Rnd;

public class ItemLists
{
protected static final Logger _log = Logger.getLogger(ItemLists.class.getName());
private FastMap<String, FastList<Integer>> _itemLists;

public static ItemLists getInstance()
{
	return SingletonHolder._instance;
}

private ItemLists()
{
	loadLists();
}

public void loadLists()
{
	_itemLists = new FastMap<String, FastList<Integer>>();
	
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement = con.prepareStatement("SELECT * FROM itemlists");
		ResultSet result = statement.executeQuery();
		
		int count = 0;
		
		while (result.next())
		{
			String list = result.getString("list");
			if (list == null)
				continue;
			
			if (list.equalsIgnoreCase(""))
				list = "0";
			
			final StringTokenizer st = new StringTokenizer(list, ";");
			FastList<Integer> fastlist = new FastList<Integer>();
			
			while (st.hasMoreTokens())
			{
				int itemId = 0;
				
				try
				{
					itemId = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					itemId = 0;
				}
				
				if (itemId != 0)
					fastlist.addLast(itemId);
			}
			
			final String name = result.getString("name");
			
			if (!_itemLists.containsKey(name))
			{
				_itemLists.put(name, fastlist);
				count++;
			}
		}
		
		result.close();
		statement.close();
		
		_log.config("Loaded " + count + " item lists from the database.");
		
		statement = con.prepareStatement("SELECT name, include FROM itemlists");
		result = statement.executeQuery();
		
		count = 0;
		
		while (result.next())
		{
			String include = result.getString("include");
			
			if (include == null || include.equalsIgnoreCase("0"))
				continue;
			
			final StringTokenizer st = new StringTokenizer(include, ";");
			FastList<Integer> fastlist = new FastList<Integer>();
			
			while (st.hasMoreTokens())
			{
				int listId = 0;
				
				try
				{
					listId = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					listId = 0;
				}
				
				if (listId != 0)
				{
					fastlist.addAll(_itemLists.get(getListName(listId)));
				}
			}
			
			_itemLists.get(result.getString("name")).addAll(fastlist);
			count++;
		}
		
		_log.config("....and loaded " + count + " combined item lists from the database.");
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error loading item lists.", e);
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

public String getListName(int listId)
{
	int count = 1;
	
	if (listId > 1000000)
		listId -= 1000000;
	
	for (String val : _itemLists.keySet())
	{
		if (count == listId)
			return val;
		
		count++;
	}
	
	_log.warning("getListName() of ItemLists returned null!!!!!!!!!!!");
	return null;
}

public String getListImage(int listId)
{
	switch (listId)
	{
	case 1000001:
		return "icon.etc_fire_stone_i00";
	case 1000002:
		return "icon.etc_fire_crystal_i00";
	case 1000003:
		return "icon.etc_crystal_ball_silver_i00";
	case 1000005:
		return "icon.weapon_arcana_mace_i01";
	case 1000006:
		return "icon.weapon_dynasty_blade_i01";
	case 1000007:
		return "icon.weapon_icarus_spiter_i01";
	case 1000008:
		return "icon.weapon_vesper_slasher_i01";
	case 1000009:
		return "tysandyweaponsbyChandy.tysandy_crusher_i";
	case 1000010:
		return "blackvesperwp.weapon_vesper_thrower_i00";
	case 1000011:
		return "aionweapontex.st_r013_i00";
	case 1000012:
		return "aionweapontex.ts_c004_i00";
	case 1000014:
		return "ancientwps.ancient_db_i00";
	case 1000017:
		return "icon.armor_t70_u_i00";
	case 1000018:
		return "icon.armor_t76_ul_i00";
	case 1000019:
		return "icon.armor_t89_ul_i00";
	case 1000020:
		return "icon.armor_t89_ul_i02";
	case 1000021:
		return "icon.armor_t91_uf_i00"; //Dynasty Armors
	case 1000022:
		return "icon.armor_t91_u_i02"; //Dynasty 2.5 Armors
	case 1000023:
		return "icon.armor_t95_u_i00";
	case 1000024:
		return "icon.armor_t1005_ul_i00";
	case 1000025:
		return "icon.armor_t79_u_i00";
	case 1000026:
		return "l2prideicons.armor_t1004_u_i00";
	case 1000028:
		return "icon.accessory_necklace_of_valakas_i00";
	case 1000029:
		return "icon.accessary_dynasty_necklace_i00";
	case 1000030:
		return "icon.vesper_necklace_i00";
	case 1000031:
		return "icon.accessary_dragon_necklace_i00";
	case 1000032:
		return "l2prideicons.exile";
	case 1000033:
		return "l2prideicons.pridenecklace";
	case 1000035:
		return "l2prideicons.amethyst";
	case 1000036:
		return "icon.etc_mineral_general_i03";
	case 1000037:
		return "icon.etc_mineral_general_i03";
	case 1000038:
		return "icon.etc_mineral_general_i03";
	case 1000039:
		return "icon.etc_mineral_general_i03";
	case 1000040:
		return "icon.armor_t15_u_i00";
	case 1000041:
		return "icon.accessory_ice_queen_i00";
	case 1000042:
		return "icon.accessory_iron_circlet_i00";
	case 1000045:
		return "icon.accessory_hair_feeler_i00";
	case 1000049:
		return "icon.accessory_full_mask_i01";
	case 1000050:
		return "BranchSys.icon.br_mask_i00";
	case 1000051:
		return "icon.accessory_ar_karm_i00";
	case 1000052:
		return "l2prideicons.dynastyhelm";
	case 1000053:
		return "icon.accessory_pledge_hair7_i00";
	case 1000056:
		return "icon.etc_spell_books_element_i00";
	case 1000057:
		return "icon.etc_raid_a_i02";
	case 1000058:
		return "icon.etc_bugle_i00";
	case 1000059:
		return "L2PrideHonorIcons.dusk";
	case 1000061:
		return "l2prideicons.eternium_ore";
	case 1000062:
		return "icon.armor_t95_u_i01";
	case 1000063:
		return "icon.armor_cotton_robe_i02";
	case 1000064:
		return "l2prideicons.airrave"; //Forbidden Skills
	case 1000065:
		return "AuraKyriadScarletArmor.Icon.armor_t97_u_i00"; //Porphyria Armors
	case 1000066:
		return "c3.godsblade_icon"; //Legacy Weapons
	default:
		return "icon.item_system03";
	}
}

public int generateRandomItemFromList(int listId)
{
	final String name = getListName(listId);
	
	if (name != null)
	{
		FastList<Integer> val = _itemLists.get(name);
		
		if (val != null && !val.isEmpty())
			return val.get(Rnd.get(val.size()));
	}
	
	_log.warning("generateRandomItemFromList() of ItemLists returned 0!!!!!!!!!!! list id: " + listId);
	return 0;
}

public FastList<Integer> getFirstListByItemId(int itemId)
{
	for (FastList<Integer> list : _itemLists.values())
	{
		if (list != null && list.size() > 0)
		{
			if (list.contains(itemId))
				return list;
		}
	}
	
	return null;
}

public void debug()
{
	System.out.println(_itemLists.toString());
}

@SuppressWarnings("synthetic-access")
private static class SingletonHolder
{
protected static final ItemLists _instance = new ItemLists();
}
}