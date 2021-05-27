package net.sf.l2j.gameserver.model.zone.type;

import javolution.util.FastList;
import net.sf.l2j.Config;
import net.sf.l2j.gameserver.instancemanager.CursedWeaponsManager;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.util.Rnd;

public class L2TownZone extends L2ZoneType
{
private String _townName;
private int _townId;
private int _redirectTownId;
private int _taxById;
private boolean _isPeaceZone;
private int[] _spawnLoc;
private final FastList<int[]> _respawnPoints;

public L2TownZone(int id)
{
	super(id);
	
	_taxById = 0;
	_respawnPoints = new FastList<int[]>();
	_spawnLoc = new int[3];
	
	// Default to Giran
	_redirectTownId = 9;
	
	// Default peace zone
	_isPeaceZone = true;
}

@Override
public void setParameter(String name, String value)
{
	if (name.equals("name"))
	{
		_townName = value;
	}
	else if (name.equals("townId"))
	{
		_townId = Integer.parseInt(value);
	}
	else if (name.equals("redirectTownId"))
	{
		_redirectTownId = Integer.parseInt(value);
	}
	else if (name.equals("taxById"))
	{
		_taxById = Integer.parseInt(value);
	}
	else if (name.equals("spawnX"))
	{
		_spawnLoc[0] = Integer.parseInt(value);
	}
	else if (name.equals("spawnY"))
	{
		_spawnLoc[1] = Integer.parseInt(value);
	}
	else if (name.equals("spawnZ"))
	{
		_spawnLoc[2] = Integer.parseInt(value);
		_respawnPoints.add(_spawnLoc);
		_spawnLoc = new int[3];
	}
	else if (name.equals("isPeaceZone"))
	{
		_isPeaceZone = Boolean.parseBoolean(value);
	}
	else
		super.setParameter(name, value);
}

@SuppressWarnings("unused")
@Override
protected void onEnter(L2Character character)
{
	if (character instanceof L2PcInstance)
	{
		final L2PcInstance player = (L2PcInstance)character;
		
		if (player == null) return;
		
		if (_townId == 5)  //gludin
		{
			if (!player.isInGludin())
				player.setIsInGludin(true);
		}
		else if (_townId == 9) //Giran
		{
			if (!player.isInGiran())
				player.setIsInGiran(true);
		}
		else if (_townId == 4) //orc village
		{
			if (!player.isInOrcVillage())
			{
				player.setInOrcVillage(true);
				
				if (!player.isGM())
				{
					if (player.isInParty())
						player.leaveParty();
					
					try
					{
						if (player.isCursedWeaponEquipped())
							CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).endOfLife();
					}
					catch (Exception e)
					{
					}
					if (player.isMounted())
						player.dismount();
					if (player.isTransformed())
						player.stopTransformation(null);
					
					if (!player.getAppearance().getSex() && player.isSpawned())
					{
						player.broadcastUserInfo();
						
						for (L2PcInstance nigga : player.getKnownList().getKnownPlayers().values())
						{
							if (nigga != null && nigga.getKnownList() != null && nigga.getKnownList().knowsObject(player))
							{
								nigga.getKnownList().removeKnownObject(player);
								nigga.getKnownList().addKnownObject(player);
							}
						}
					}
				}
			}
		}
		/*		else if (_townId == 13) //goddard
			{
			}*/
		
		// PVP possible during siege, now for siege participants only
		// Could also check if this town is in siege, or if any siege is going on
		if (player.getSiegeState() != 0 && Config.ZONE_TOWN == 1)
			return;
		
		//((L2PcInstance)character).sendMessage("You entered "+_townName);
	}
	
	if (_isPeaceZone && Config.ZONE_TOWN != 2)
		character.setInsideZone(L2Character.ZONE_PEACE, true);
	
	character.setInsideZone(L2Character.ZONE_TOWN, true);
}

@Override
protected void onExit(L2Character character)
{
	if (_townId == 5) //gludin
	{
		if (character instanceof L2PcInstance && character.getActingPlayer().isInGludin())
			character.getActingPlayer().setIsInGludin(false);
	}	// TODO: there should be no exit if there was possibly no enter
	else if (_townId == 9) //Giran
	{
		if (character instanceof L2PcInstance && character.getActingPlayer().isInGiran())
			character.getActingPlayer().setIsInGiran(false);
	}	
	else if (_townId == 4) //orc village
	{
		if (character instanceof L2PcInstance && character.getActingPlayer().isInOrcVillage())
		{
			//character.getActingPlayer().setDisguised(false);
			character.getActingPlayer().broadcastUserInfo();
			character.getActingPlayer().setInOrcVillage(false);
		}
	}
	/*		else if (_townId == 13) //goddard
		{
		}*/
	
	if (_isPeaceZone)
	{
		character.setInsideZone(L2Character.ZONE_PEACE, false);
	}
	
	character.setInsideZone(L2Character.ZONE_TOWN, false);
	
	// if (character instanceof L2PcInstance)
	//((L2PcInstance)character).sendMessage("You left "+_townName);
}

@Override
public void onDieInside(L2Character character)
{
}

@Override
public void onReviveInside(L2Character character)
{
}

/**
 * Returns this town zones name
 * @return
 */
@Deprecated
public String getName()
{
	return _townName;
}

/**
 * Returns this zones town id (if any)
 * @return
 */
public int getTownId()
{
	return _townId;
}

/**
 * Gets the id for this town zones redir town
 * @return
 */
@Deprecated
public int getRedirectTownId()
{
	return _redirectTownId;
}

/**
 * Returns this zones spawn location
 * @return
 */
public final int[] getSpawnLoc()
{
	final int size = _respawnPoints.size();
	
	if (size == 1)
		return _respawnPoints.get(0);
	else
		return _respawnPoints.get(Rnd.get(size));
}
/**
 * Returns this town zones castle id
 * @return
 */
public final int getTaxById()
{
	return _taxById;
}

public final boolean isPeaceZone()
{
	return _isPeaceZone;
}
}
