/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.network.clientpackets;

import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import cz.nxs.interf.NexusEvents;

/**
 * This class ...
 *
 * @version $Revision: 1.7.2.1.2.3 $ $Date: 2005/03/27 15:29:30 $
 */
public final class RequestMagicSkillUse extends L2GameClientPacket
{
private static final String _C__2F_REQUESTMAGICSKILLUSE = "[C] 2F RequestMagicSkillUse";
private static Logger _log = Logger.getLogger(RequestMagicSkillUse.class.getName());

private int _magicId;
private boolean _ctrlPressed;
private boolean _shiftPressed;

@Override
protected void readImpl()
{
	_magicId      = readD();              // Identifier of the used skill
	_ctrlPressed  = readD() != 0;         // True if it's a ForceAttack : Ctrl pressed
	_shiftPressed = readC() != 0;         // True if Shift pressed
}

@Override
protected void runImpl()
{
	// Get the current L2PcInstance of the player
	L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
		return;
	
	// Get the level of the used skill
	final int level = activeChar.getSkillLevel(_magicId);
	
	if (level <= 0)
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	if (activeChar.isAfraid())
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return;
	}
	
	// Get the L2Skill template corresponding to the skillID received from the client
	final L2Skill skill = SkillTable.getInstance().getInfo(_magicId, level);
	
	// Check the validity of the skill
	if (skill != null && skill.getSkillType() != L2SkillType.NOTDONE)
	{
		// _log.fine("	skill:"+skill.getName() + " level:"+skill.getLevel() + " passive:"+skill.isPassive());
		// _log.fine("	range:"+skill.getCastRange()+" targettype:"+skill.getTargetType()+" optype:"+skill.getOperateType()+" power:"+skill.getPower());
		// _log.fine("	reusedelay:"+skill.getReuseDelay()+" hittime:"+skill.getHitTime());
		// _log.fine("	currentState:"+activeChar.getCurrentState());	//for debug
		
		// If Alternate rule Karma punishment is set to true, forbid skill Return to player with Karma
		if (skill.getSkillType() == L2SkillType.RECALL && !Config.ALT_GAME_KARMA_PLAYER_CAN_TELEPORT && activeChar.getKarma() > 0)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// players mounted on pets cannot use any toggle skills
		if (skill.isToggle() && activeChar.isMounted())
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		boolean allow = true;
		if(activeChar.isTransformed())
		{
			if(NexusEvents.isInEvent(activeChar))
			{
				int allowSkill = NexusEvents.allowTransformationSkill(activeChar, skill);
				
				if(allowSkill == -1)
					allow = false;
			}
		}
		
		// activeChar.stopMove();
		if (!allow)
		{
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else
			activeChar.useMagic(skill, _ctrlPressed, _shiftPressed);
	}
	else
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (skill == null)
			_log.warning("No skill found with id " + _magicId + " and level " + level + " !!");
	}
}

@Override
public String getType()
{
	return _C__2F_REQUESTMAGICSKILLUSE;
}

@Override
protected boolean triggersOnActionRequest()
{
	return true;
}
}