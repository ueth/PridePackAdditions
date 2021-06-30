package net.sf.l2j.gameserver.model.actor.appearance;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import cz.nxs.interf.NexusEvents;

public class PcAppearance
{
// =========================================================
// Data Field
private L2PcInstance _owner;

private byte _face;

private byte _hairColor;

private byte _hairStyle;

private boolean _sex = false; // Female true(1)

/** The current visible name of this player, not necessarily the real one */
private String _visibleName;

/** The current visible title of this player, not necessarily the real one */
private String _visibleTitle;

/** The hexadecimal Color of players name (white is 0xFFFFFF) */
private int _nameColor = 0xFFFFFF;

/** The hexadecimal Color of players name (white is 0xFFFFFF) */
private int _titleColor = 0xFFFF77;

// =========================================================
// Constructor
public PcAppearance(byte face, byte hColor, byte hStyle, boolean sex)
{
	_face = face;
	_hairColor = hColor;
	_hairStyle = hStyle;
	_sex = sex;
}

// =========================================================
// Method - Public

// =========================================================
// Method - Private

/**
 * @param visibleName
 * The visibleName to set.
 */
public final void setVisibleName(String visibleName)
{
	_visibleName = visibleName;
}

/**
 * @return Returns the visibleName.
 */
public final String getVisibleName()
{
	if (_owner.isInOlympiadMode())
	{
		return "Olympiader";
	}

	if(_owner.isInFairGame())
	{
		return "Player";
	}
	
	if (NexusEvents.isInEvent(_owner))
	{
		if (_owner.getEventInfo().hasAntifeedProtection())
			return "Player";
	}
	
	if (_owner._inEventDM && DM._started)
	{
		return "Contestant";
	}
	
	if (_visibleName == null)
	{
		if (_owner.isDisguised()/* && !_owner.isInFunEventDisguised()*/)
			return "Disguised";
		
		return _owner.getName();
	}
	
	return _visibleName;
}

/**
 * @param visibleTitle
 * The visibleTitle to set.
 */
public final void setVisibleTitle(String visibleTitle)
{
	_visibleTitle = visibleTitle;
}

/**
 * @return Returns the visibleTitle.
 */
public final String getVisibleTitle()
{
	if (_owner.isInOlympiadMode())
		return "";
	if(_owner.isInFairGame())
		return "";

	if (NexusEvents.isInEvent(_owner))
	{
		if (_owner.getEventInfo().hasAntifeedProtection())
			return "";
	}
	else if (_owner._inEventTvT && TvT._started)
		return "Kills: "+_owner._countTvTkills+" Deaths: "+_owner._countTvTdies;
	else if (_owner._inEventFOS && FOS._started)
	{
		if (FOS._team1Sealers.contains(_owner.getObjectId()))
		{
			return "Siege Leader ["+_owner._countFOSCaps+"]";
		}
		else if (FOS._team2Sealers.contains(_owner.getObjectId()))
		{
			return "Siege Leader ["+_owner._countFOSCaps+"]";
		}
		
		return "Kills: "+_owner._countFOSKills;
	}
	else if (_owner._inEventTvTi)
		return "Kills: "+_owner._countTvTiKills;
	else if (_owner._inEventCTF && CTF._started)
		return "Scored: "+_owner._countCTFflags;
	else if (_owner._inEventDM && DM._started)
		return "Kills: "+_owner._countDMkills;
	else if (_owner._inEventVIP && VIP._started)
	{
		if (_owner._isTheVIP)
			return "The VIP";
	}
	else if (_owner.isDisguised())
	{
		return "";
	}
	
	if (_visibleTitle == null)
		return getOwner().getTitle();
	
	return _visibleTitle;
}

// =========================================================
// Property - Public
public final byte getFace()
{
	return _face;
}

/**
 * @param byte
 *            value
 */
public final void setFace(int value)
{
	_face = (byte) value;
}

public final byte getHairColor()
{
	return _hairColor;
}

/**
 * @param byte
 *            value
 */
public final void setHairColor(int value)
{
	_hairColor = (byte) value;
}

public final byte getHairStyle()
{
	return _hairStyle;
}

/**
 * @param byte
 *            value
 */
public final void setHairStyle(int value)
{
	_hairStyle = (byte) value;
}

public final boolean getSex()
{
	return _sex;
}

/**
 * @param boolean
 *            isfemale
 */
public final void setSex(boolean isfemale)
{
	_sex = isfemale;
}

public int getNameColor()
{
	if (_owner.isInOlympiadMode())
		return Integer.decode("0xFFFFFF"); //White
	if(_owner.isInFairGame())
		return Integer.decode("0xFFFFFF");

	else if (_owner.isInFunEvent())
	{
		try
		{
			if (_owner._inEventTvT && TvT._started)
			{
				int index = TvT._teams.indexOf(_owner._teamNameTvT);
				
				if (index < 0)
					return _nameColor;
				
				return TvT._teamColors.get(index);
			}
			if (_owner._inEventFOS && FOS._started)
			{
				int index = FOS._teams.indexOf(_owner._teamNameFOS);
				
				if (index < 0)
					return _nameColor;
				
				return FOS._teamColors.get(index);
			}
			else if (_owner._inEventCTF && CTF._started)
				return CTF._teamColors.get(CTF._teams.indexOf(_owner._teamNameCTF));
			else if (_owner._inEventVIP && VIP._started)
			{
				if (_owner._isTheVIP)
					return Integer.decode("0x3366ff"); //Orange
				else if (_owner._isVIP)
					return Integer.decode("0x33FFFF"); //Yellow
				else if (_owner._isNotVIP)
					return Integer.decode("0x666666"); //Grey
				else
				{
					System.out.println("L O L WTF "+_owner.getName()+" is considered to be in VIP but has all 3 vars FALSE!!");
				}
			}
			else if (_owner._inEventDM && DM._started)
				return DM._playerColors;
		}
		catch (Exception e)
		{
			return _nameColor;
		}
	}
	else if (_owner.isDisguised())
		return Integer.decode("0xFFFFFF"); //White
	
	return _nameColor;
}

public void setNameColor(int nameColor)
{
	_nameColor = nameColor;
}

public void setNameColor(int red, int green, int blue)
{
	_nameColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
}

public int getTitleColor()
{
	return _titleColor;
}

public void setTitleColor(int titleColor)
{
	_titleColor = titleColor;
}

public void setTitleColor(int red, int green, int blue)
{
	_titleColor = (red & 0xFF) + ((green & 0xFF) << 8) + ((blue & 0xFF) << 16);
}

/**
 * @param owner   The owner to set.
 */
public void setOwner(L2PcInstance owner)
{
	_owner = owner;
}

/**
 * @return Returns the owner.
 */
public L2PcInstance getOwner()
{
	return _owner;
}
}
