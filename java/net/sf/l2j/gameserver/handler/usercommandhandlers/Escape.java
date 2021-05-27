package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SetupGauge;

public class Escape implements IUserCommandHandler
{
private static final int[] COMMAND_IDS =
{
	52
};

/**
 * 
 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#useUserCommand(int, net.sf.l2j.gameserver.model.actor.instance.L2PcInstance)
 */
public boolean useUserCommand(int id, L2PcInstance activeChar)
{
	if (!L2TeleporterInstance.checkIfCanTeleport(activeChar))
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return false;
	}
	// Thanks nbd
	if (!TvTEvent.onEscapeUse(activeChar.getObjectId()))
	{
		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
		return false;
	}
	
	if (activeChar.isInFunEvent())
	{
		activeChar.sendMessage("You may not escape from an event.");
		return false;
	}
	
	if (activeChar.getInstanceId() != 0)
	{
		activeChar.sendMessage("You may not escape from an instance.");
		return false;
	}
	
	final int unstuckTimer = (activeChar.getAccessLevel().isGm() ? 1000 : Config.UNSTUCK_INTERVAL * 1000);
	
	activeChar.forceIsCasting(GameTimeController.getGameTicks() + unstuckTimer / GameTimeController.MILLIS_IN_TICK);
	
	L2Skill escape = SkillTable.getInstance().getInfo(2099, 1); // 5 minutes escape
	L2Skill GM_escape = SkillTable.getInstance().getInfo(2100, 1); // 1 second escape
	
	if (activeChar.getAccessLevel().isGm())
	{
		if (GM_escape != null)
		{
			activeChar.doCast(GM_escape);
			return true;
		}
		activeChar.sendMessage("You use Escape: 1 second.");
	}
	else if (Config.UNSTUCK_INTERVAL == 300 && escape  != null)
	{
		activeChar.doCast(escape);
		return true;
	}
	
	activeChar.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
	//SoE Animation section
	activeChar.setTarget(activeChar);
	activeChar.disableAllSkills();
	
	MagicSkillUse msk = new MagicSkillUse(activeChar, 1050, 1, unstuckTimer, 0);
	activeChar.broadcastPacket(msk);
	SetupGauge sg = new SetupGauge(0, unstuckTimer);
	activeChar.sendPacket(sg);
	//End SoE Animation section
	
	EscapeFinalizer ef = new EscapeFinalizer(activeChar);
	// continue execution later
	activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleGeneral(ef, unstuckTimer));
	
	return true;
}

static class EscapeFinalizer implements Runnable
{
private L2PcInstance _activeChar;

EscapeFinalizer(L2PcInstance activeChar)
{
	_activeChar = activeChar;
}

public void run()
{
	if (_activeChar.isAlikeDead())
		return;
	
	_activeChar.setIsIn7sDungeon(false);
	_activeChar.enableAllSkills();
	_activeChar.setIsCastingNow(false);
	_activeChar.setInstanceId(0);
	
	try
	{
		_activeChar.teleToLocation(MapRegionTable.TeleportWhereType.Town);
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "", e);
	}
}
}

/**
 * 
 * @see net.sf.l2j.gameserver.handler.IUserCommandHandler#getUserCommandList()
 */
public int[] getUserCommandList()
{
	return COMMAND_IDS;
}
}