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
package net.sf.l2j.gameserver.model.quest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcQuestHtmlMessage;
import net.sf.l2j.gameserver.scripting.ManagedScript;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;
import net.sf.l2j.gameserver.util.GMAudit;
import net.sf.l2j.util.Rnd;

/**
 * @author Luis Arias
 *
 */
public class Quest extends ManagedScript
{
protected static final Logger _log = Logger.getLogger(Quest.class.getName());

/** HashMap containing events from String value of the event */
private static Map<String, Quest> _allEventsS = new FastMap<String, Quest>();
/** HashMap containing lists of timers from the name of the timer */
private final Map<String, FastList<QuestTimer>> _allEventTimers = new FastMap<String, FastList<QuestTimer>>();

private final ReentrantReadWriteLock _rwLock = new ReentrantReadWriteLock();

private final int _questId;
private final String _name;
private final String _descr;
private final byte _initialState = State.CREATED;
private boolean _onEnterWorld = false;
// NOTE: questItemIds will be overridden by child classes.  Ideally, it should be
// protected instead of public.  However, quest scripts written in Jython will
// have trouble with protected, as Jython only knows private and public...
// In fact, protected will typically be considered private thus breaking the scripts.
// Leave this as public as a workaround.
public int[] questItemIds = null;

/**
 * Return collection view of the values contains in the allEventS
 * @return Collection<Quest>
 */
public static Collection<Quest> findAllEvents()
{
	return _allEventsS.values();
}

/**
 * (Constructor)Add values to class variables and put the quest in HashMaps.
 * @param questId : int pointing out the ID of the quest
 * @param name : String corresponding to the name of the quest
 * @param descr : String for the description of the quest
 */
public Quest(int questId, String name, String descr)
{
	_questId = questId;
	_name = name;
	_descr = descr;
	
	if (questId != 0)
	{
		QuestManager.getInstance().addQuest(Quest.this);
	}
	else
	{
		_allEventsS.put(name, this);
	}
	init_LoadGlobalData();
}

/**
 * The function init_LoadGlobalData is, by default, called by the constructor of all quests.
 * Children of this class can implement this function in order to define what variables
 * to load and what structures to save them in.  By default, nothing is loaded.
 */
protected void init_LoadGlobalData()
{
	
}

/**
 * The function saveGlobalData is, by default, called at shutdown, for all quests, by the QuestManager.
 * Children of this class can implement this function in order to convert their structures
 * into <var, value> tuples and make calls to save them to the database, if needed.
 * By default, nothing is saved.
 */
public void saveGlobalData()
{
	
}

public static enum QuestEventType
{
ON_FIRST_TALK(false), // control the first dialog shown by NPCs when they are clicked (some quests must override the default npc action)
QUEST_START(true), // onTalk action from start npcs
ON_TALK(true), // onTalk action from npcs participating in a quest
ON_ATTACK(true), // onAttack action triggered when a mob gets attacked by someone
ON_KILL(true), // onKill action triggered when a mob gets killed.
ON_SPAWN(true), // onSpawn action triggered when an NPC is spawned or respawned.
ON_SKILL_SEE(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
ON_FACTION_CALL(true), // NPC or Mob saw a person casting a skill (regardless what the target is).
ON_AGGRO_RANGE_ENTER(true), // a person came within the Npc/Mob's range
ON_SPELL_FINISHED(true), // on spell finished action when npc finish casting skill
ON_SKILL_LEARN(false), // control the AcquireSkill dialog from quest script
ON_ENTER_ZONE(true), // on zone enter
ON_EXIT_ZONE(true); // on zone exit


// control whether this event type is allowed for the same npc template in multiple quests
// or if the npc must be registered in at most one quest for the specified event
private boolean _allowMultipleRegistration;

QuestEventType(boolean allowMultipleRegistration)
{
	_allowMultipleRegistration = allowMultipleRegistration;
}

public boolean isMultipleRegistrationAllowed()
{
	return _allowMultipleRegistration;
}

}

/**
 * Return ID of the quest
 * @return int
 */
public int getQuestIntId()
{
	return _questId;
}

/**
 * Add a new QuestState to the database and return it.
 * @param player
 * @return QuestState : QuestState created
 */
public QuestState newQuestState(L2PcInstance player)
{
	QuestState qs = new QuestState(this, player, getInitialState());
	Quest.createQuestInDb(qs);
	return qs;
}

/**
 * Return initial state of the quest
 * @return State
 */
public byte getInitialState()
{
	return _initialState;
}

/**
 * Return name of the quest
 * @return String
 */
public String getName()
{
	return _name;
}

/**
 * Return description of the quest
 * @return String
 */
public String getDescr()
{
	return _descr;
}

/**
 * Add a timer to the quest, if it doesn't exist already
 * @param name: name of the timer (also passed back as "event" in onAdvEvent)
 * @param time: time in ms for when to fire the timer
 * @param npc:  npc associated with this timer (can be null)
 * @param player: player associated with this timer (can be null)
 */
public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player)
{
	startQuestTimer(name, time, npc, player, false);
}

/**
 * Add a timer to the quest, if it doesn't exist already.  If the timer is repeatable,
 * it will auto-fire automatically, at a fixed rate, until explicitly canceled.
 * @param name: name of the timer (also passed back as "event" in onAdvEvent)
 * @param time: time in ms for when to fire the timer
 * @param npc:  npc associated with this timer (can be null)
 * @param player: player associated with this timer (can be null)
 * @param repeatable: indicates if the timer is repeatable or one-time.
 */
public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
{
	// Add quest timer if timer doesn't already exist
	FastList<QuestTimer> timers = getQuestTimers(name);
	// no timer exists with the same name, at all
	if (timers == null)
	{
		timers = new FastList<QuestTimer>();
		timers.add(new QuestTimer(this, name, time, npc, player, repeating));
		_allEventTimers.put(name, timers);
	}
	// a timer with this name exists, but may not be for the same set of npc and player
	else
	{
		// if there exists a timer with this name, allow the timer only if the [npc, player] set is unique
		// nulls act as wildcards
		if (getQuestTimer(name, npc, player) == null)
		{
			try
			{
				_rwLock.writeLock().lock();
				timers.add(new QuestTimer(this, name, time, npc, player, repeating));
			}
			finally
			{
				_rwLock.writeLock().unlock();
			}
		}
	}
}

public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
{
	FastList<QuestTimer> qt = getQuestTimers(name);
	
	if (qt == null || qt.isEmpty())
		return null;
	try
	{
		_rwLock.readLock().lock();
		for (QuestTimer timer : qt)
		{
			if (timer != null)
			{
				if (timer.isMatch(this, name, npc, player))
					return timer;
			}
		}
		
	}
	finally
	{
		_rwLock.readLock().unlock();
	}
	return null;
}

private FastList<QuestTimer> getQuestTimers(String name)
{
	return _allEventTimers.get(name);
}

public void cancelQuestTimers(String name)
{
	FastList<QuestTimer> timers = getQuestTimers(name);
	if (timers == null)
		return;
	try
	{
		_rwLock.writeLock().lock();
		for (QuestTimer timer : timers)
		{
			if (timer != null)
			{
				timer.cancel();
			}
		}
	}
	finally
	{
		_rwLock.writeLock().unlock();
	}
}

public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
{
	QuestTimer timer = getQuestTimer(name, npc, player);
	if (timer != null)
		timer.cancel();
}

public void removeQuestTimer(QuestTimer timer)
{
	if (timer == null)
		return;
	FastList<QuestTimer> timers = getQuestTimers(timer.getName());
	if (timers == null)
		return;
	try
	{
		_rwLock.writeLock().lock();
		timers.remove(timer);
	}
	finally
	{
		_rwLock.writeLock().unlock();
	}
}

// these are methods to call from java
public final boolean notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
{
	String res = null;
	try
	{
		res = onAttack(npc, attacker, damage, isPet, skill);
	}
	catch (Exception e)
	{
		return showError(attacker, e);
	}
	return showResult(attacker, res);
}

public final boolean notifyDeath(L2Character killer, L2Character victim, QuestState qs)
{
	String res = null;
	try
	{
		res = onDeath(killer, victim, qs);
	}
	catch (Exception e)
	{
		return showError(qs.getPlayer(), e);
	}
	return showResult(qs.getPlayer(), res);
}

public final boolean notifySpellFinished(L2Npc instance, L2PcInstance player, L2Skill skill)
{
	String res = null;
	try
	{
		res = onSpellFinished(instance, player, skill);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
	
}
public final boolean notifySpawn(L2Npc npc)
{
	try
	{
		onSpawn(npc);
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "", e);
		return true;
	}
	return false;
}

public final boolean notifyEvent(String event, L2Npc npc, L2PcInstance player)
{
	String res = null;
	try
	{
		res = onAdvEvent(event, npc, player);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
}

public final boolean notifyEnterWorld(L2PcInstance player)
{
	String res = null;
	try
	{
		res = onEnterWorld(player);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
}

public final boolean notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	String res = null;
	try
	{
		res = onKill(npc, killer, isPet);
	}
	catch (Exception e)
	{
		return showError(killer, e);
	}
	return showResult(killer, res);
}

public final boolean notifyTalk(L2Npc npc, QuestState qs)
{
	String res = null;
	try
	{
		res = onTalk(npc, qs.getPlayer());
	}
	catch (Exception e)
	{
		return showError(qs.getPlayer(), e);
	}
	qs.getPlayer().setLastQuestNpcObject(npc.getObjectId());
	return showResult(qs.getPlayer(), res);
}

// override the default NPC dialogs when a quest defines this for the given NPC
public final boolean notifyFirstTalk(L2Npc npc, L2PcInstance player)
{
	String res = null;
	try
	{
		res = onFirstTalk(npc, player);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	// if the quest returns text to display, display it.
	if (res != null && res.length() > 0)
		return showResult(player, res);
	// else tell the player that
	else
		player.sendPacket(ActionFailed.STATIC_PACKET);
	// note: if the default html for this npc needs to be shown, onFirstTalk should
	// call npc.showChatWindow(player) and then return null.
	return true;
}

public final boolean notifyAcquireSkillList(L2Npc npc, L2PcInstance player)
{
	String res = null;
	try
	{
		res = onAcquireSkillList(npc, player);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
}

public final boolean notifyAcquireSkillInfo(L2Npc npc, L2PcInstance player, L2Skill skill)
{
	String res = null;
	try
	{
		res = onAcquireSkillInfo(npc, player, skill);
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
}

public final boolean notifyAcquireSkill(L2Npc npc, L2PcInstance player, L2Skill skill)
{
	String res = null;
	try
	{
		res = onAcquireSkill(npc, player, skill);
		if (res == "true")
			return true;
		else if (res == "false")
			return false;
	}
	catch (Exception e)
	{
		return showError(player, e);
	}
	return showResult(player, res);
}

public class tmpOnSkillSee implements Runnable
{
private final L2Npc _npc;
private final L2PcInstance _caster;
private final L2Skill _skill;
private final L2Object[] _targets;
private final boolean _isPet;

public tmpOnSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
{
	_npc = npc;
	_caster = caster;
	_skill = skill;
	_targets = targets;
	_isPet = isPet;
}

public void run()
{
	String res = null;
	try
	{
		res = onSkillSee(_npc, _caster, _skill, _targets, _isPet);
	}
	catch (Exception e)
	{
		showError(_caster, e);
	}
	showResult(_caster, res);
	
}
}

public final boolean notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
{
	ThreadPoolManager.getInstance().executeAi(new tmpOnSkillSee(npc, caster, skill, targets, isPet));
	return true;
}

public final boolean notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
{
	String res = null;
	try
	{
		res = onFactionCall(npc, caller, attacker, isPet);
	}
	catch (Exception e)
	{
		return showError(attacker, e);
	}
	return showResult(attacker, res);
}

public class tmpOnAggroEnter implements Runnable
{
private final L2Npc _npc;
private final L2PcInstance _pc;
private final boolean _isPet;

public tmpOnAggroEnter(L2Npc npc, L2PcInstance pc, boolean isPet)
{
	_npc = npc;
	_pc = pc;
	_isPet = isPet;
}

public void run()
{
	String res = null;
	try
	{
		res = onAggroRangeEnter(_npc, _pc, _isPet);
	}
	catch (Exception e)
	{
		showError(_pc, e);
	}
	showResult(_pc, res);
	
}
}
public final boolean notifyAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
{
	ThreadPoolManager.getInstance().executeAi(new tmpOnAggroEnter(npc, player, isPet));
	return true;
}

public final boolean notifyEnterZone(L2Character character, L2ZoneType zone)
{
	L2PcInstance player = character.getActingPlayer();
	String res = null;
	try
	{
		res = onEnterZone(character, zone);
	}
	catch (Exception e)
	{
		if (player != null)
			return showError(player, e);
	}
	if (player != null)
		return showResult(player, res);
	return true;
}

public final boolean notifyExitZone(L2Character character, L2ZoneType zone)
{
	L2PcInstance player = character.getActingPlayer();
	String res = null;
	try
	{
		res = onExitZone(character, zone);
	}
	catch (Exception e)
	{
		if (player != null)
			return showError(player, e);
	}
	if (player != null)
		return showResult(player, res);
	return true;
}

// these are methods that java calls to invoke scripts
public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
{
	return null;
}

public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
{
	return onAttack(npc, attacker, damage, isPet);
}

public String onDeath(L2Character killer, L2Character victim, QuestState qs)
{
	if (killer instanceof L2Npc)
		return onAdvEvent("", (L2Npc) killer, qs.getPlayer());
	else
		return onAdvEvent("", null, qs.getPlayer());
}

public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
{
	// if not overridden by a subclass, then default to the returned value of the simpler (and older) onEvent override
	// if the player has a state, use it as parameter in the next call, else return null
	QuestState qs = player.getQuestState(getName());
	if (qs != null)
		return onEvent(event, qs);
	
	return null;
}

public String onEvent(String event, QuestState qs)
{
	return null;
}

public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
{
	return null;
}

public String onTalk(L2Npc npc, L2PcInstance talker)
{
	return null;
}

public String onFirstTalk(L2Npc npc, L2PcInstance player)
{
	return null;
}

public String onAcquireSkillList(L2Npc npc, L2PcInstance player)
{
	return null;
}

public String onAcquireSkillInfo(L2Npc npc, L2PcInstance player, L2Skill skill)
{
	return null;
}

public String onAcquireSkill(L2Npc npc, L2PcInstance player, L2Skill skill)
{
	return null;
}

public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
{
	return null;
}

public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
{
	return null;
}

public String onSpawn(L2Npc npc)
{
	return null;
}

public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
{
	return null;
}

public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
{
	return null;
}

public String onEnterWorld(L2PcInstance player)
{
	return null;
}

public String onEnterZone(L2Character character, L2ZoneType zone)
{
	return null;
}

public String onExitZone(L2Character character, L2ZoneType zone)
{
	return null;
}

/**
 * Show message error to player who has an access level greater than 0
 * @param player : L2PcInstance
 * @param t : Throwable
 * @return boolean
 */
public boolean showError(L2PcInstance player, Throwable t)
{
	_log.log(Level.WARNING, getScriptFile().getAbsolutePath(), t);
	if (player != null && player.getAccessLevel().isGm())
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.close();
		String res = "<html><body><title>Script error</title>" + sw.toString() + "</body></html>";
		return showResult(player, res);
	}
	return false;
}

/**
 * Show a message to player.<BR><BR>
 * <U><I>Concept : </I></U><BR>
 * 3 cases are managed according to the value of the parameter "res" :<BR>
 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
 * <LI><U>otherwise :</U> the message held in "res" is shown in chat box</LI>
 * @param qs : QuestState
 * @param res : String pointing out the message to show at the player
 * @return boolean
 */
public boolean showResult(L2PcInstance player, String res)
{
	if (res == null || res.isEmpty() || player == null)
		return true;
	if (res.endsWith(".htm"))
	{
		showHtmlFile(player, res);
	}
	else if (res.startsWith("<html>"))
	{
		NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
		npcReply.setHtml(res);
		npcReply.replace("%playername%", player.getName());
		player.sendPacket(npcReply);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	else
	{
		player.sendMessage(res);
	}
	return false;
}

/**
 * Add quests to the L2PCInstance of the player.<BR><BR>
 * <U><I>Action : </U></I><BR>
 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
 * @param player : Player who is entering the world
 */
public final static void playerEnter(L2PcInstance player)
{
	
	Connection con = null;
	try
	{
		// Get list of quests owned by the player from database
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		
		PreparedStatement invalidQuestData = con.prepareStatement("DELETE FROM character_quests WHERE charId=? and name=?");
		PreparedStatement invalidQuestDataVar = con.prepareStatement("delete FROM character_quests WHERE charId=? and name=? and var=?");
		
		statement = con.prepareStatement("SELECT name,value FROM character_quests WHERE charId=? AND var=?");
		statement.setInt(1, player.getObjectId());
		statement.setString(2, "<state>");
		ResultSet rs = statement.executeQuery();
		while (rs.next())
		{
			
			// Get ID of the quest and ID of its state
			String questId = rs.getString("name");
			String statename = rs.getString("value");
			
			// Search quest associated with the ID
			Quest q = QuestManager.getInstance().getQuest(questId);
			if (q == null)
			{
				_log.finer("Unknown quest " + questId + " for player " + player.getName());
				if (Config.AUTODELETE_INVALID_QUEST_DATA)
				{
					invalidQuestData.setInt(1, player.getObjectId());
					invalidQuestData.setString(2, questId);
					invalidQuestData.executeUpdate();
				}
				continue;
			}
			
			// Create a new QuestState for the player that will be added to the player's list of quests
			new QuestState(q, player, State.getStateId(statename));
		}
		rs.close();
		invalidQuestData.close();
		statement.close();
		
		// Get list of quests owned by the player from the DB in order to add variables used in the quest.
		statement = con.prepareStatement("SELECT name,var,value FROM character_quests WHERE charId=? AND var<>?");
		statement.setInt(1, player.getObjectId());
		statement.setString(2, "<state>");
		rs = statement.executeQuery();
		while (rs.next())
		{
			String questId = rs.getString("name");
			String var = rs.getString("var");
			String value = rs.getString("value");
			// Get the QuestState saved in the loop before
			QuestState qs = player.getQuestState(questId);
			if (qs == null)
			{
				_log.finer("Lost variable " + var + " in quest " + questId + " for player " + player.getName());
				if (Config.AUTODELETE_INVALID_QUEST_DATA)
				{
					invalidQuestDataVar.setInt(1, player.getObjectId());
					invalidQuestDataVar.setString(2, questId);
					invalidQuestDataVar.setString(3, var);
					invalidQuestDataVar.executeUpdate();
				}
				continue;
			}
			// Add parameter to the quest
			qs.setInternal(var, value);
		}
		rs.close();
		invalidQuestDataVar.close();
		statement.close();
		
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not insert char quest:", e);
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
	
	// events
	for (String name : _allEventsS.keySet())
	{
		player.processQuestEvent(name, "enter");
	}
}

/**
 * Insert (or Update) in the database variables that need to stay persistant for this quest after a reboot.
 * This function is for storage of values that do not related to a specific player but are
 * global for all characters.  For example, if we need to disable a quest-gatekeeper until
 * a certain time (as is done with some grand-boss gatekeepers), we can save that time in the DB.
 * @param var : String designating the name of the variable for the quest
 * @param value : String designating the value of the variable for the quest
 */
public final void saveGlobalQuestVar(String var, String value)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("REPLACE INTO quest_global_data (quest_name,var,value) VALUES (?,?,?)");
		statement.setString(1, getName());
		statement.setString(2, var);
		statement.setString(3, value);
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not insert global quest variable:", e);
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

/**
 * Read from the database a previously saved variable for this quest.
 * Due to performance considerations, this function should best be used only when the quest is first loaded.
 * Subclasses of this class can define structures into which these loaded values can be saved.
 * However, on-demand usage of this function throughout the script is not prohibited, only not recommended.
 * Values read from this function were entered by calls to "saveGlobalQuestVar"
 * @param var : String designating the name of the variable for the quest
 * @return String : String representing the loaded value for the passed var, or an empty string if the var was invalid
 */
public final String loadGlobalQuestVar(String var)
{
	String result = "";
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("SELECT value FROM quest_global_data WHERE quest_name = ? AND var = ?");
		statement.setString(1, getName());
		statement.setString(2, var);
		ResultSet rs = statement.executeQuery();
		if (rs.first())
			result = rs.getString(1);
		rs.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not load global quest variable:", e);
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
	return result;
}

/**
 * Permanently delete from the database a global quest variable that was previously saved for this quest.
 * @param var : String designating the name of the variable for the quest
 */
public final void deleteGlobalQuestVar(String var)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ? AND var = ?");
		statement.setString(1, getName());
		statement.setString(2, var);
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not delete global quest variable:", e);
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

/**
 * Permanently delete from the database all global quest variables that was previously saved for this quest.
 */
public final void deleteAllGlobalQuestVars()
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("DELETE FROM quest_global_data WHERE quest_name = ?");
		statement.setString(1, getName());
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not delete global quest variables:", e);
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

/**
 * Insert in the database the quest for the player.
 * @param qs : QuestState pointing out the state of the quest
 * @param var : String designating the name of the variable for the quest
 * @param value : String designating the value of the variable for the quest
 */
public static void createQuestVarInDb(QuestState qs, String var, String value)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("INSERT INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)");
		statement.setInt(1, qs.getPlayer().getObjectId());
		statement.setString(2, qs.getQuestName());
		statement.setString(3, var);
		statement.setString(4, value);
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not insert char quest:", e);
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

/**
 * Update the value of the variable "var" for the quest.<BR><BR>
 * <U><I>Actions :</I></U><BR>
 * The selection of the right record is made with :
 * <LI>charId = qs.getPlayer().getObjectID()</LI>
 * <LI>name = qs.getQuest().getName()</LI>
 * <LI>var = var</LI>
 * <BR><BR>
 * The modification made is :
 * <LI>value = parameter value</LI>
 * @param qs : Quest State
 * @param var : String designating the name of the variable for quest
 * @param value : String designating the value of the variable for quest
 */
public static void updateQuestVarInDb(QuestState qs, String var, String value)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND name=? AND var = ?");
		statement.setString(1, value);
		statement.setInt(2, qs.getPlayer().getObjectId());
		statement.setString(3, qs.getQuestName());
		statement.setString(4, var);
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not update char quest:", e);
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

/**
 * Delete a variable of player's quest from the database.
 * @param qs : object QuestState pointing out the player's quest
 * @param var : String designating the variable characterizing the quest
 */
public static void deleteQuestVarInDb(QuestState qs, String var)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=? AND var=?");
		statement.setInt(1, qs.getPlayer().getObjectId());
		statement.setString(2, qs.getQuestName());
		statement.setString(3, var);
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not delete char quest:", e);
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

/**
 * Delete the player's quest from database.
 * @param qs : QuestState pointing out the player's quest
 */
public static void deleteQuestInDb(QuestState qs)
{
	Connection con = null;
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=? AND name=?");
		statement.setInt(1, qs.getPlayer().getObjectId());
		statement.setString(2, qs.getQuestName());
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.WARNING, "could not delete char quest:", e);
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

/**
 * Create a record in database for quest.<BR><BR>
 * <U><I>Actions :</I></U><BR>
 * Use fucntion createQuestVarInDb() with following parameters :<BR>
 * <LI>QuestState : parameter sq that puts in fields of database :
 * 	 <UL type="square">
 *     <LI>charId : ID of the player</LI>
 *     <LI>name : name of the quest</LI>
 *   </UL>
 * </LI>
 * <LI>var : string "&lt;state&gt;" as the name of the variable for the quest</LI>
 * <LI>val : string corresponding at the ID of the state (in fact, initial state)</LI>
 * @param qs : QuestState
 */
public static void createQuestInDb(QuestState qs)
{
	createQuestVarInDb(qs, "<state>", State.getStateName(qs.getState()));
}

/**
 * Update informations regarding quest in database.<BR>
 * <U><I>Actions :</I></U><BR>
 * <LI>Get ID state of the quest recorded in object qs</LI>
 * <LI>Test if quest is completed. If true, add a star (*) before the ID state</LI>
 * <LI>Save in database the ID state (with or without the star) for the variable called "&lt;state&gt;" of the quest</LI>
 * @param qs : QuestState
 */
public static void updateQuestInDb(QuestState qs)
{
	String val = State.getStateName(qs.getState());
	updateQuestVarInDb(qs, "<state>", val);
}

/**
 * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.<BR><BR>
 * @param npcId : id of the NPC to register
 * @param eventType : type of event being registered
 * @return L2NpcTemplate : Npc Template corresponding to the npcId, or null if the id is invalid
 */
public L2NpcTemplate addEventId(int npcId, QuestEventType eventType)
{
	try
	{
		L2NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
		if (t != null)
		{
			t.addQuestEvent(eventType, this);
		}
		return t;
	}
	catch (Exception e)
	{
		e.printStackTrace();
		return null;
	}
}

/**
 * Add the quest to the NPC's startQuest
 * @param npcId
 * @return L2NpcTemplate : Start NPC
 */
public L2NpcTemplate addStartNpc(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.QUEST_START);
}

/**
 * Add the quest to the NPC's first-talk (default action dialog)
 * @param npcId
 * @return L2NpcTemplate : Start NPC
 */
public L2NpcTemplate addFirstTalkId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_FIRST_TALK);
}

/**
 * Add the NPC to the AcquireSkill dialog
 * @param npcId
 * @return L2NpcTemplate : NPC
 */
public L2NpcTemplate addAcquireSkillId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_SKILL_LEARN);
}

/**
 * Add this quest to the list of quests that the passed mob will respond to for Attack Events.<BR><BR>
 * @param attackId
 * @return int : attackId
 */
public L2NpcTemplate addAttackId(int attackId)
{
	return addEventId(attackId, Quest.QuestEventType.ON_ATTACK);
}

/**
 * Add this quest to the list of quests that the passed mob will respond to for Kill Events.<BR><BR>
 * @param killId
 * @return int : killId
 */
public L2NpcTemplate addKillId(int killId)
{
	return addEventId(killId, Quest.QuestEventType.ON_KILL);
}

/**
 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.<BR><BR>
 * @param talkId : ID of the NPC
 * @return int : ID of the NPC
 */
public L2NpcTemplate addTalkId(int talkId)
{
	return addEventId(talkId, Quest.QuestEventType.ON_TALK);
}

/**
 * Add this quest to the list of quests that the passed npc will respond to for Spawn Events.<BR><BR>
 * @param talkId : ID of the NPC
 * @return int : ID of the NPC
 */
public L2NpcTemplate addSpawnId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_SPAWN);
}

/**
 * Add this quest to the list of quests that the passed npc will respond to for Skill-See Events.<BR><BR>
 * @param talkId : ID of the NPC
 * @return int : ID of the NPC
 */
public L2NpcTemplate addSkillSeeId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_SKILL_SEE);
}

public L2NpcTemplate addSpellFinishedId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_SPELL_FINISHED);
}
/**
 * Add this quest to the list of quests that the passed npc will respond to for Faction Call Events.<BR><BR>
 * @param talkId : ID of the NPC
 * @return int : ID of the NPC
 */
public L2NpcTemplate addFactionCallId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_FACTION_CALL);
}

/**
 * Add this quest to the list of quests that the passed npc will respond to for Character See Events.<BR><BR>
 * @param talkId : ID of the NPC
 * @return int : ID of the NPC
 */
public L2NpcTemplate addAggroRangeEnterId(int npcId)
{
	return addEventId(npcId, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
}

public L2ZoneType addEnterZoneId(int zoneId)
{
	try
	{
		L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
		if (zone != null)
		{
			zone.addQuestEvent(Quest.QuestEventType.ON_ENTER_ZONE, this);
		}
		return zone;
	}
	catch (Exception e)
	{
		e.printStackTrace();
		return null;
	}
}

public L2ZoneType addExitZoneId(int zoneId)
{
	try
	{
		L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
		if (zone != null)
		{
			zone.addQuestEvent(Quest.QuestEventType.ON_EXIT_ZONE, this);
		}
		return zone;
	}
	catch (Exception e)
	{
		e.printStackTrace();
		return null;
	}
}

// returns a random party member's L2PcInstance for the passed player's party
// returns the passed player if he has no party.
public L2PcInstance getRandomPartyMember(L2PcInstance player)
{
	// NPE prevention.  If the player is null, there is nothing to return
	if (player == null)
		return null;
	if ((player.getParty() == null) || (player.getParty().getPartyMembers().isEmpty()))
		return player;
	L2Party party = player.getParty();
	return party.getPartyMembers().get(Rnd.get(party.getPartyMembers().size()));
}

/**
 * Auxilary function for party quests.
 * Note: This function is only here because of how commonly it may be used by quest developers.
 * For any variations on this function, the quest script can always handle things on its own
 * @param player: the instance of a player whose party is to be searched
 * @param value: the value of the "cond" variable that must be matched
 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified
 * 			condition, or null if no match.
 */
public L2PcInstance getRandomPartyMember(L2PcInstance player, String value)
{
	return getRandomPartyMember(player, "cond", value);
}

/**
 * Auxilary function for party quests.
 * Note: This function is only here because of how commonly it may be used by quest developers.
 * For any variations on this function, the quest script can always handle things on its own
 * @param player: the instance of a player whose party is to be searched
 * @param var/value: a tuple specifying a quest condition that must be satisfied for
 *     a party member to be considered.
 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified
 * 				condition, or null if no match.  If the var is null, any random party
 * 				member is returned (i.e. no condition is applied).
 * 				The party member must be within 1500 distance from the target of the reference
 * 				player, or if no target exists, 1500 distance from the player itself.
 */
public L2PcInstance getRandomPartyMember(L2PcInstance player, String var, String value)
{
	// if no valid player instance is passed, there is nothing to check...
	if (player == null)
		return null;
	
	// for null var condition, return any random party member.
	if (var == null)
		return getRandomPartyMember(player);
	
	// normal cases...if the player is not in a party, check the player's state
	QuestState temp = null;
	L2Party party = player.getParty();
	// if this player is not in a party, just check if this player instance matches the conditions itself
	if ((party == null) || (party.getPartyMembers().isEmpty()))
	{
		temp = player.getQuestState(getName());
		if ((temp != null) && (temp.get(var) != null) && ((String) temp.get(var)).equalsIgnoreCase(value))
			return player; // match
		
		return null; // no match
	}
	
	// if the player is in a party, gather a list of all matching party members (possibly
	// including this player)
	FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
	
	// get the target for enforcing distance limitations.
	L2Object target = player.getTarget();
	if (target == null)
		target = player;
	
	for (L2PcInstance partyMember : party.getPartyMembers())
	{
		if (partyMember == null)
			continue;
		temp = partyMember.getQuestState(getName());
		if ((temp != null) && (temp.get(var) != null) && ((String) temp.get(var)).equalsIgnoreCase(value) && partyMember.isInsideRadius(target, 1500, true, false))
			candidates.add(partyMember);
	}
	// if there was no match, return null...
	if (candidates.isEmpty())
		return null;
	
	// if a match was found from the party, return one of them at random.
	return candidates.get(Rnd.get(candidates.size()));
}

/**
 * Auxilary function for party quests.
 * Note: This function is only here because of how commonly it may be used by quest developers.
 * For any variations on this function, the quest script can always handle things on its own
 * @param player: the instance of a player whose party is to be searched
 * @param state: the state in which the party member's queststate must be in order to be considered.
 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified
 * 				condition, or null if no match.  If the var is null, any random party
 * 				member is returned (i.e. no condition is applied).
 */
public L2PcInstance getRandomPartyMemberState(L2PcInstance player, byte state)
{
	// if no valid player instance is passed, there is nothing to check...
	if (player == null)
		return null;
	
	// normal cases...if the player is not in a partym check the player's state
	QuestState temp = null;
	L2Party party = player.getParty();
	// if this player is not in a party, just check if this player instance matches the conditions itself
	if ((party == null) || (party.getPartyMembers().isEmpty()))
	{
		temp = player.getQuestState(getName());
		if ((temp != null) && (temp.getState() == state))
			return player; // match
		
		return null; // no match
	}
	
	// if the player is in a party, gather a list of all matching party members (possibly
	// including this player)
	FastList<L2PcInstance> candidates = new FastList<L2PcInstance>();
	
	// get the target for enforcing distance limitations.
	L2Object target = player.getTarget();
	if (target == null)
		target = player;
	
	for (L2PcInstance partyMember : party.getPartyMembers())
	{
		if (partyMember == null)
			continue;
		temp = partyMember.getQuestState(getName());
		if ((temp != null) && (temp.getState() == state) && partyMember.isInsideRadius(target, 1500, true, false))
			candidates.add(partyMember);
	}
	// if there was no match, return null...
	if (candidates.isEmpty())
		return null;
	
	// if a match was found from the party, return one of them at random.
	return candidates.get(Rnd.get(candidates.size()));
}

/**
 * Show HTML file to client
 * @param fileName
 * @return String : message sent to client
 */
public String showHtmlFile(L2PcInstance player, String fileName)
{
	String questName = getName();
	int questId = getQuestIntId();
	//Create handler to file linked to the quest
	String directory = getDescr().toLowerCase();
	String content = HtmCache.getInstance().getHtm("data/scripts/" + directory + "/" + questName + "/" + fileName);
	
	if (content == null)
		content = HtmCache.getInstance().getHtmForce("data/scripts/quests/" + questName + "/" + fileName);
	
	if (player != null && player.getTarget() != null)
		content = content.replaceAll("%objectId%", String.valueOf(player.getTarget().getObjectId()));
	
	//Send message to client if message not empty
	if (content != null)
	{
		if (questId > 0 && questId < 20000)
		{
			NpcQuestHtmlMessage npcReply = new NpcQuestHtmlMessage(5,questId);
			npcReply.setHtml(content);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
		}
		else
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(5);
			npcReply.setHtml(content);
			npcReply.replace("%playername%", player.getName());
			player.sendPacket(npcReply);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	return content;
}

// =========================================================
//  QUEST SPAWNS
// =========================================================

public class DeSpawnScheduleTimerTask implements Runnable
{
L2Npc _npc = null;

public DeSpawnScheduleTimerTask(L2Npc npc)
{
	_npc = npc;
}

public void run()
{
	_npc.onDecay();
}
}

// Method - Public
/**
 * Add a temporary (quest) spawn
 * Return instance of newly spawned npc
 */
public L2Npc addSpawn(int npcId, L2Character cha)
{
	return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, false);
}

/**
 * Add a temporary (quest) spawn
 * Return instance of newly spawned npc
 * with summon animation
 */
public L2Npc addSpawn(int npcId, L2Character cha, boolean isSummonSpawn)
{
	return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), false, 0, isSummonSpawn);
}

public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, int despawnDelay)
{
	return addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay, false);
}

public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn)
{
	return addSpawn(npcId, x, y, z, heading, randomOffset, despawnDelay, isSummonSpawn, 0);
}

public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, int despawnDelay, boolean isSummonSpawn, int instanceId)
{
	L2Npc result = null;
	try
	{
		L2NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
		if (template != null)
		{
			// Sometimes, even if the quest script specifies some xyz (for example npc.getX() etc) by the time the code
			// reaches here, xyz have become 0!  Also, a questdev might have purposely set xy to 0,0...however,
			// the spawn code is coded such that if x=y=0, it looks into location for the spawn loc!  This will NOT work
			// with quest spawns!  For both of the above cases, we need a fail-safe spawn.  For this, we use the
			// default spawn location, which is at the player's loc.
			if ((x == 0) && (y == 0))
			{
				_log.log(Level.SEVERE, "Failed to adjust bad locks for quest spawn!  Spawn aborted!");
				return null;
			}
			if (randomOffset)
			{
				int offset;
				
				offset = Rnd.get(2); // Get the direction of the offset
				if (offset == 0)
				{
					offset = -1;
				} // make offset negative
				offset *= Rnd.get(70, 120);
				x += offset;
				
				offset = Rnd.get(2); // Get the direction of the offset
				if (offset == 0)
				{
					offset = -1;
				} // make offset negative
				offset *= Rnd.get(70, 120);
				y += offset;
			}
			L2Spawn spawn = new L2Spawn(template);
			spawn.setInstanceId(instanceId);
			spawn.setHeading(heading);
			spawn.setLocx(x);
			spawn.setLocy(y);
			spawn.setLocz(z + 20);
			spawn.stopRespawn();
			result = spawn.spawnOne(isSummonSpawn);
			
			if (despawnDelay > 0)
				ThreadPoolManager.getInstance().scheduleGeneral(new DeSpawnScheduleTimerTask(result), despawnDelay);
			
			return result;
		}
		else
		{
			_log.severe("Quest.java addSpawn() called a null NPC to be spawned w/ ID: "+npcId);
		}
	}
	catch (Exception e1)
	{
		_log.warning("Could not spawn Npc " + npcId);
	}
	
	return null;
}

public int[] getRegisteredItemIds()
{
	return questItemIds;
}

/**
 * @see net.sf.l2j.gameserver.scripting.ManagedScript#getScriptName()
 */
@Override
public String getScriptName()
{
	return getName();
}

/**
 * @see net.sf.l2j.gameserver.scripting.ManagedScript#setActive(boolean)
 */
@Override
public void setActive(boolean status)
{
	// TODO implement me
}

/**
 * @see net.sf.l2j.gameserver.scripting.ManagedScript#reload()
 */
@Override
public boolean reload()
{
	unload();
	return super.reload();
}

/**
 * @see net.sf.l2j.gameserver.scripting.ManagedScript#unload()
 */
@Override
public boolean unload()
{
	saveGlobalData();
	// cancel all pending timers before reloading.
	// if timers ought to be restarted, the quest can take care of it
	// with its code (example: save global data indicating what timer must
	// be restarted).
	for (FastList<QuestTimer> timers : _allEventTimers.values())
		for (QuestTimer timer : timers)
			timer.cancel();
	_allEventTimers.clear();
	return QuestManager.getInstance().removeQuest(this);
}

/**
 * @see net.sf.l2j.gameserver.scripting.ManagedScript#getScriptManager()
 */
@Override
public ScriptManager<?> getScriptManager()
{
	return QuestManager.getInstance();
}

public void setOnEnterWorld(boolean val)
{
	_onEnterWorld = val;
}

public boolean getOnEnterWorld()
{
	return _onEnterWorld;
}

final public static int HALFDAY = 0;
final public static int ONEDAY = 1;
final public static int TWODAYS = 2;
final public static int HALFWEEK = 3;
final public static int WEEK = 4;

public long getNextInstanceTime(int type)
{
	final Calendar calendar = Calendar.getInstance();
	
	switch (type)
	{
	case HALFDAY:
		calendar.set(Calendar.HOUR, 11);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		break;
	case ONEDAY:
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		break;
	case TWODAYS:
		final int day = calendar.get(Calendar.DAY_OF_YEAR);
		if (day % 2 == 0)
			calendar.set(Calendar.DAY_OF_YEAR, day+2);
		else
			calendar.set(Calendar.DAY_OF_YEAR, day+1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		break;
	case HALFWEEK:
		Calendar calendar2 = Calendar.getInstance();
		calendar2.setFirstDayOfWeek(Calendar.SUNDAY);
		
		calendar2.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
		calendar2.set(Calendar.HOUR_OF_DAY, 12);
		calendar2.set(Calendar.MINUTE, 0);
		calendar2.set(Calendar.SECOND, 0);
		calendar2.set(Calendar.MILLISECOND, 0);
		
		if (calendar.compareTo(calendar2) > 0) //done on thur, fri, sat and last half of wed
		{
			calendar.setFirstDayOfWeek(Calendar.SUNDAY);
			calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 999);
		}
		else
		{
			calendar.setFirstDayOfWeek(Calendar.SUNDAY);
			calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
			calendar.set(Calendar.HOUR_OF_DAY, 11);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MILLISECOND, 999);
		}
		break;
	case WEEK:
		calendar.setFirstDayOfWeek(Calendar.SUNDAY);
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		break;
	}
	
	return calendar.getTimeInMillis();
}

final protected static void auditInstances(final L2PcInstance player, String template, final int instanceid)
{
	final String player_acct = player.getAccountName();
	final String player_name = player.getName();
	final String player_IP = player.getIP();
	
	final String today = GMAudit._formatter.format(new Date());
	template = template.replaceAll(".xml", "");
	
	Connection con = null;
	PreparedStatement statement = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		statement = con.prepareStatement("INSERT INTO audit_instance(player_acct, player, player_IP, instance, date) VALUES(?,?,?,?,?)");
		
		statement.setString(1, player_acct);
		statement.setString(2, player_name);
		statement.setString(3, player_IP);
		statement.setString(4, template);
		statement.setString(5, today);
		
		statement.executeUpdate();
		statement.close();
	}
	catch (Exception e)
	{
		_log.fine("could not audit instances: "+player_name+" "+e);
	}
	finally { try { if (con != null) con.close(); } catch (SQLException e) { e.printStackTrace(); } }
}


public boolean checkIPs(L2Party party)
{
	if (party != null)
	{
		for (L2PcInstance player : party.getPartyMembers())
		{
			final String IP = player.getIP();
			
			if (IP != null)
			{
				for (L2PcInstance player2 : party.getPartyMembers())
				{
					if (player == player2)
						continue;
					
					if (player2.getIP() == null || IP.equalsIgnoreCase(player2.getIP()))
					{
						party.broadcastMessageToPartyMembers(player2.getName() +" has the same IP as "+player+" and thus cannot enter the instance");
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	return false;
}
}
