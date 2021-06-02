package net.sf.l2j.gameserver.network.clientpackets;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GmListTable;
import net.sf.l2j.gameserver.SevenSigns;
import net.sf.l2j.gameserver.TaskPriority;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.communitybbs.Manager.RegionBBSManager;
import net.sf.l2j.gameserver.datatables.AdminCommandAccessRights;
import net.sf.l2j.gameserver.datatables.CharSchemesTable;
import net.sf.l2j.gameserver.datatables.MapRegionTable.TeleportWhereType;
import net.sf.l2j.gameserver.datatables.SkillTable;
//import net.sf.l2j.gameserver.handler.itemhandlers.Gem;
import net.sf.l2j.gameserver.instancemanager.CastleManager;
import net.sf.l2j.gameserver.instancemanager.ClanHallManager;
import net.sf.l2j.gameserver.instancemanager.CoupleManager;
import net.sf.l2j.gameserver.instancemanager.CrownManager;
import net.sf.l2j.gameserver.instancemanager.DimensionalRiftManager;
import net.sf.l2j.gameserver.instancemanager.FortManager;
import net.sf.l2j.gameserver.instancemanager.FortSiegeManager;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instancemanager.PetitionManager;
import net.sf.l2j.gameserver.instancemanager.QuestManager;
import net.sf.l2j.gameserver.instancemanager.SiegeManager;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.ClanHall;
import net.sf.l2j.gameserver.model.entity.Couple;
import net.sf.l2j.gameserver.model.entity.Fort;
import net.sf.l2j.gameserver.model.entity.FortSiege;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.entity.L2Event;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.TvTEvent;
import net.sf.l2j.gameserver.model.events.CTF;
import net.sf.l2j.gameserver.model.events.DM;
import net.sf.l2j.gameserver.model.events.FOS;
import net.sf.l2j.gameserver.model.events.TvT;
import net.sf.l2j.gameserver.model.events.VIP;
import net.sf.l2j.gameserver.model.olympiad.Olympiad;
import net.sf.l2j.gameserver.model.quest.Quest;
import net.sf.l2j.gameserver.model.quest.QuestState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.Die;
import net.sf.l2j.gameserver.network.serverpackets.EtcStatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ExBasicActionList;
import net.sf.l2j.gameserver.network.serverpackets.ExBrExtraUserInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import net.sf.l2j.gameserver.network.serverpackets.ExStorageMaxCount;
import net.sf.l2j.gameserver.network.serverpackets.FriendList;
import net.sf.l2j.gameserver.network.serverpackets.HennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListAll;
import net.sf.l2j.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import net.sf.l2j.gameserver.network.serverpackets.PledgeSkillList;
import net.sf.l2j.gameserver.network.serverpackets.PledgeStatusChanged;
import net.sf.l2j.gameserver.network.serverpackets.QuestList;
import net.sf.l2j.gameserver.network.serverpackets.SSQInfo;
import net.sf.l2j.gameserver.network.serverpackets.ShortCutInit;
import net.sf.l2j.gameserver.network.serverpackets.SkillCoolTime;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.UserInfo;
import cz.nxs.interf.NexusEvents;


public class EnterWorld extends L2GameClientPacket
{
private static final String _C__03_ENTERWORLD = "[C] 03 EnterWorld";

public TaskPriority getPriority()
{
	return TaskPriority.PR_URGENT;
}

@Override
protected void readImpl()
{
}

@Override
protected void runImpl()
{
	final L2PcInstance activeChar = getClient().getActiveChar();
	
	if (activeChar == null)
	{
		_log.warning("EnterWorld failed! activeChar returned 'null'.");
		getClient().closeNow();
		return;
	}
	
	// Restore to instanced area if enabled
	if (Config.RESTORE_PLAYER_INSTANCE)
		activeChar.setInstanceId(InstanceManager.getInstance().getPlayerInstanceId(activeChar.getObjectId()));
	else
	{
		int instanceId = InstanceManager.getInstance().getPlayerInstanceId(activeChar.getObjectId());
		if (instanceId > 0)
			InstanceManager.getInstance().getInstance(instanceId).removePlayer(activeChar.getObjectId());
	}
	
	if (L2World.getInstance().findObject(activeChar.getObjectId()) != null)
	{
		if (Config.DEBUG)
			_log.warning("User already exists in Object ID map! User "+activeChar.getName()+" is a character clone.");
	}
	
	// Apply special GM properties to the GM when entering
	if (activeChar.isGM())
	{
		if (Config.GM_STARTUP_INVULNERABLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invul", activeChar))
			activeChar.setIsInvul(true);
		
		if (Config.GM_STARTUP_INVISIBLE && AdminCommandAccessRights.getInstance().hasAccess("admin_invisible", activeChar))
			activeChar.setInvisible(true);
		
		if (Config.GM_STARTUP_SILENCE && AdminCommandAccessRights.getInstance().hasAccess("admin_silence", activeChar) || activeChar.getAccessLevel().getLevel() > 6)
			activeChar.setMessageRefusal(true);
		
		if (activeChar.getAccessLevel().getLevel() > 1)
		{
			BlockList.setBlockAll(activeChar, true);
			activeChar.setTradeRefusal(true);
		}
		
		if (Config.GM_STARTUP_AUTO_LIST && AdminCommandAccessRights.getInstance().hasAccess("admin_gmliston", activeChar) && activeChar.getAccessLevel().getLevel() < 7)
			GmListTable.getInstance().addGm(activeChar, false);
		else
			GmListTable.getInstance().addGm(activeChar, true);
	}
	else
	{
		activeChar.getFloodProtectors().getTradeChat().tryPerformAction("trade chat");
		activeChar.getFloodProtectors().getShout().tryPerformAction("shout");
		activeChar.getFloodProtectors().getHeroVoice().tryPerformAction("hero voice");
	}
	
	activeChar.setNameColorsDueToPVP();
	
	// Set dead status if applies
	if (activeChar.getCurrentHp() < 0.5)
		activeChar.setIsDead(true);
	
	// Set Hero status if it applies
	if (Hero.getInstance().getHeroes() != null && Hero.getInstance().getHeroes().containsKey(activeChar.getObjectId()))
	{
		activeChar.setHero(true);
		activeChar._previousMonthOlympiadGamesPlayed = Olympiad.getInstance().getLastNobleOlympiadGamesPlayed(activeChar.getObjectId());
	}
	
	setPledgeClass(activeChar);
	
	boolean showClanNotice = false;
	
	// Clan related checks are here
	if (activeChar.getClan() != null)
	{
		activeChar.sendPacket(new PledgeSkillList(activeChar.getClan()));
		
		notifyClanMembers(activeChar);
		
		notifySponsorOrApprentice(activeChar);
		
		if (activeChar.isClanLeader())
		{
			final ClanHall clanHall = ClanHallManager.getInstance().getClanHallByOwner(activeChar.getClan());
			
			if (clanHall != null)
			{
				if (!clanHall.getPaid())
					activeChar.sendPacket(new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW));
			}
		}
		
		for (Siege siege : SiegeManager.getInstance().getSieges())
		{
			if (!siege.getIsInProgress())
				continue;
			
			if (siege.checkIsAttacker(activeChar.getClan()))
				activeChar.setSiegeState((byte)1);
			
			else if (siege.checkIsDefender(activeChar.getClan()))
				activeChar.setSiegeState((byte)2);
		}
		
		for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
		{
			if (!siege.getIsInProgress())
				continue;
			
			if (siege.checkIsAttacker(activeChar.getClan()))
				activeChar.setSiegeState((byte)1);
			
			else if (siege.checkIsDefender(activeChar.getClan()))
				activeChar.setSiegeState((byte)2);
		}
		
		sendPacket(new PledgeShowMemberListAll(activeChar.getClan(), activeChar));
		sendPacket(new PledgeStatusChanged(activeChar.getClan()));
		
		// Residential skills support
		if (activeChar.getClan().getHasCastle() > 0)
			CastleManager.getInstance().getCastleByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
		
		if (activeChar.getClan().getHasFort() > 0)
			FortManager.getInstance().getFortByOwner(activeChar.getClan()).giveResidentialSkills(activeChar);
		
		showClanNotice = activeChar.getClan().isNoticeEnabled();
	}
	
	sendPacket(new SSQInfo());
	
	// Updating Seal of Strife Buff/Debuff
	if (SevenSigns.getInstance().isSealValidationPeriod() && SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL)
	{
		if (SevenSigns.getInstance().getPlayerCabal(activeChar) != SevenSigns.CABAL_NULL)
		{
			if (SevenSigns.getInstance().getPlayerCabal(activeChar) == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				activeChar.addSkill(SkillTable.getInstance().getInfo(5074,1));
			else
				activeChar.addSkill(SkillTable.getInstance().getInfo(5075,1));
		}
	}
	else
	{
		activeChar.removeSkill(SkillTable.getInstance().getInfo(5074,1));
		activeChar.removeSkill(SkillTable.getInstance().getInfo(5075,1));
	}
	
	if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
	{
		float points = Config.RATE_RECOVERY_ON_RECONNECT * (System.currentTimeMillis() - activeChar.getLastAccess()) / 60000;
		if (points > 0)
			activeChar.updateVitalityPoints(points, false, true);
	}
	
	final File mainText = new File(Config.DATAPACK_ROOT, "data/html/welcome.htm"); // Return the pathfile of the HTML file
	
	if (mainText.exists())
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/welcome.htm");
		html.replace("%name%", activeChar.getName()); // replaces %name% with activeChar.getName(), so you can say like "welcome to the server %name%"
		sendPacket(html);
	}
	
	// Send Macro List
	activeChar.getMacroses().sendUpdate();
	
	/*	if (activeChar.isNoble() && activeChar.getInventory().getItemByItemId(7694) == null && activeChar.getWarehouse().getItemByItemId(7694) == null)
		activeChar.addItem("Noblesse Circlet", 7694, 1, activeChar, true);*/
	
	// Send GG check
	/*activeChar.queryGameGuard();*/
	
	// Send Teleport Bookmark List
	sendPacket(new ExGetBookMarkInfoPacket(activeChar));
	
	// Wedding Checks
	if (Config.L2JMOD_ALLOW_WEDDING)
	{
		engage(activeChar);
		notifyPartner(activeChar,activeChar.getPartnerId());
		activeChar.giveMarriageSkills();
	}
	
	// check for crowns
	CrownManager.checkCrowns(activeChar);
	
	// Send Item List
	sendPacket(new ItemList(activeChar, false));
	
	activeChar.sendSkillList();
	
	// Send Shortcuts
	sendPacket(new ShortCutInit(activeChar));
	
	// Send Action list
	activeChar.sendPacket(ExBasicActionList.DEFAULT_ACTION_LIST);
	
	// Send Dye Information
	activeChar.sendPacket(new HennaInfo(activeChar));
	
	sendPacket(new UserInfo(activeChar));
	sendPacket(new ExBrExtraUserInfo(activeChar));
	activeChar.canSendUserInfo = true;
	
	Quest.playerEnter(activeChar);
	
	loadTutorial(activeChar);
	
	getClient().loadMarriageStatus();
	
	for (Quest quest : QuestManager.getInstance().getAllManagedScripts())
	{
		if (quest != null && quest.getOnEnterWorld())
			quest.notifyEnterWorld(activeChar);
	}
	
	activeChar.sendPacket(new QuestList());
	
	if (Config.PLAYER_SPAWN_PROTECTION > 0)
		activeChar.setProtection(true);
	
	activeChar.spawnMe(activeChar.getX(), activeChar.getY(), activeChar.getZ());
	
	if (L2Event.active && L2Event.connectionLossData.containsKey(activeChar.getName()) && L2Event.isOnEvent(activeChar))
		L2Event.restoreChar(activeChar);
	else if (L2Event.connectionLossData.containsKey(activeChar.getName()))
		L2Event.restoreAndTeleChar(activeChar);
	
	sendPacket(new SystemMessage(SystemMessageId.WELCOME_TO_LINEAGE));
	
	activeChar.updateEffectIcons();
	activeChar.getBattlePass().loadPreviousBattlePasses(activeChar);
	
	activeChar.sendPacket(new EtcStatusUpdate(activeChar));
	
	//Expand Skill
	activeChar.sendPacket(new ExStorageMaxCount(activeChar));
	
	sendPacket(new FriendList(activeChar));
	
	sendPacket(new SkillCoolTime(activeChar));
	
	SevenSigns.getInstance().sendCurrentPeriodMsg(activeChar);
	Announcements.getInstance().showAnnouncements(activeChar);
	
	if (showClanNotice)
	{
		NpcHtmlMessage notice = new NpcHtmlMessage(1);
		notice.setFile("data/html/clanNotice.htm");
		notice.replace("%clan_name%", activeChar.getClan().getName());
		String notice1 = activeChar.getClan().getNotice();
		notice1 = notice1.replaceAll("\r\n", "<br>");
		notice1 = notice1.replace("[", "\\[");
		notice1 = notice1.replace("]", "\\]");
		notice1 = notice1.replace("(", "\\(");
		notice1 = notice1.replace(")", "\\)");
		notice1 = notice1.replace("&", "\\&");
		notice1 = notice1.replace("@", "\\@");
		notice1 = notice1.replace("{", "\\{");
		notice1 = notice1.replace("}", "\\}");
		notice1 = notice1.replace("?", "\\?");
		notice1 = notice1.replace("+", "\\+");
		notice1 = notice1.replace("-", "\\-");
		notice1 = notice1.replace("=", "\\=");
		notice1 = notice1.replace("^", "\\^");
		notice.replace("%notice_text%", notice1);
		sendPacket(notice);
	}
	else if (Config.SERVER_NEWS)
	{
		String serverNews = HtmCache.getInstance().getHtm("data/html/servnews.htm");
		if (serverNews != null)
			sendPacket(new NpcHtmlMessage(1, serverNews));
	}
	
	if (Config.PETITIONING_ALLOWED)
		PetitionManager.getInstance().checkPetitionMessages(activeChar);
	
	if (activeChar.isAlikeDead()) // dead or fake dead
	{
		// no broadcast needed since the player will already spawn dead to others
		sendPacket(new Die(activeChar));
	}
	
	notifyFriends(activeChar);
	
	CharSchemesTable.getInstance().onPlayerLogin(activeChar.getObjectId());
	
	boolean foundCupidBow = false;
	
	for (L2ItemInstance i : activeChar.getInventory().getItems())
	{
		if (i.isHeroItem())
		{
			if (!activeChar.isHero() || (!activeChar.canUseHeroItems() && i.isWeapon()))
			{
				activeChar.destroyItem("Removing Hero Item", i, activeChar, false);
			}
		}
		else if (i.isTimeLimitedItem())
		{
			i.scheduleLifeTimeTask();
		}
		else if (i.getItemId() == 9140)
		{
			foundCupidBow = true;
			
			if (!activeChar.isGM() && !activeChar.isThisCharacterMarried())
			{
				activeChar.destroyItem("Removing Cupid Bow", i, activeChar, false);
				activeChar.getInventory().updateDatabase();
			}
		}
	}
	
	if (!foundCupidBow)
	{
		if (activeChar.isThisCharacterMarried())
		{
			if (activeChar.getWarehouse().getItemByItemId(9140) == null)
			{
				activeChar.addItem("Cupid Bow", 9140, 1, activeChar, true);
				activeChar.getInventory().updateDatabase();
			}
		}
		else if (!activeChar.isGM())
		{
			final L2ItemInstance bow = activeChar.getWarehouse().getItemByItemId(9140);
			
			if (bow != null)
			{
				activeChar.getWarehouse().destroyItem("Removing Cupid Bow", bow, activeChar, activeChar);
				activeChar.getWarehouse().updateDatabase();
			}
		}
	}
	
	for (L2ItemInstance i : activeChar.getWarehouse().getItems())
	{
		if (i.isHeroItem())
		{
			if (!activeChar.isHero() || (!activeChar.canUseHeroItems() && i.isWeapon()))
			{
				activeChar.destroyItem("Removing Hero Item", i, activeChar, false);
			}
		}
		else if (i.isTimeLimitedItem())
		{
			i.scheduleLifeTimeTask();
		}
	}
	
	if (activeChar.getClanJoinExpiryTime() > System.currentTimeMillis())
		activeChar.sendPacket(new SystemMessage(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED));
	
	// remove combat flag before teleporting
	if (activeChar.getInventory().getItemByItemId(9819) != null)
	{
		Fort fort = FortManager.getInstance().getFort(activeChar);
		
		if (fort != null)
			FortSiegeManager.getInstance().dropCombatFlag(activeChar);
		else
		{
			int slot = activeChar.getInventory().getSlotFromItem(activeChar.getInventory().getItemByItemId(9819));
			activeChar.getInventory().unEquipItemInBodySlotAndRecord(slot);
			activeChar.destroyItem("CombatFlag", activeChar.getInventory().getItemByItemId(9819), null, true);
		}
	}
	
	RegionBBSManager.getInstance().changeCommunityBoard();
	
	TvTEvent.onLogin(activeChar);
	
	String filename = null;
	
	if (activeChar.getSecretCode() == null || activeChar.getSecretCode().equalsIgnoreCase("")) //doesn't have a secret code set
	{
		 filename = "data/html/custom/Gem/account/setsecretcode.htm";

		 NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		 itemReply.setFile(filename);
		 itemReply.replace("%dtn%", "You need to create a secret code or your character will have limited functions!");
		 activeChar.sendPacket(itemReply);
		 activeChar.doLockdown(504);
	     activeChar.sendMessage("Your account is lockdown and has limited functions.");
	}
	else
	{
		 filename = "data/html/custom/Gem/account/secretcodeconfirmation.htm";

		 NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
		 itemReply.setFile(filename);
		 activeChar.sendPacket(itemReply);
		 activeChar.doLockdown(504);
	     activeChar.sendMessage("Your account is locked down and has limited functions.");
	}
	/*
	if ((activeChar.getLevel() >= 20 && activeChar.getClassId().level() == 0) ||
			(activeChar.getLevel() >= 40 && activeChar.getClassId().level() == 1) ||
			(activeChar.getLevel() >= 76 && activeChar.getClassId().level() == 2))
	{
		Gem.sendClassChangeHTML(activeChar);
	}*/
	
	sendPacket(ActionFailed.STATIC_PACKET);
	
	ThreadPoolManager.getInstance().scheduleGeneral(new teleportTask(activeChar), 250);
}

private class teleportTask implements Runnable
{
final L2PcInstance _player;

private teleportTask(L2PcInstance player)
{
	_player = player;
}

public void run()
{
	try
	{
		if (_player != null)
		{
			NexusEvents.onLogin(_player);
			
			if ((TvT._started || TvT._teleport) && TvT._savePlayers.contains(_player.getObjectId()))
			{
				TvT.addDisconnectedPlayer(_player);
			}
			else if ((CTF._started || CTF._teleport) && CTF._savePlayers.contains(_player.getObjectId()))
			{
				CTF.addDisconnectedPlayer(_player);
			}
			else if ((FOS._started || FOS._teleport) && FOS._savePlayers.contains(_player.getObjectId()))
			{
				FOS.addDisconnectedPlayer(_player);
			}
			else if ((DM._started || DM._teleport) && DM._savePlayers.contains(_player.getObjectId()))
			{
				DM.addDisconnectedPlayer(_player);
			}
			else if (VIP._savePlayers.contains(_player.getObjectId()))
			{
				VIP.addDisconnectedPlayer(_player);
			}
			else if (!_player.isGM())
			{
				_player.getWorldRegion().revalidateZones(_player);
				
				if (DimensionalRiftManager.getInstance().checkIfInRiftZone(_player.getX(), _player.getY(), _player.getZ(), false))
				{
					DimensionalRiftManager.getInstance().teleportToWaitingRoom(_player);
				}
				else if (Olympiad.getInstance().playerInStadia(_player))
				{
					_player.sendMessage("You are being ported to town because you are in an Olympiad stadium");
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
				else if (_player.getSiegeState() < 2 && _player.isInsideZone(L2Character.ZONE_SIEGE))
				{
					_player.sendMessage("You are being ported to town because you are in an active siege zone");
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
				else if (_player.isInOrcVillage())
				{
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
				else if (_player.isInsideZone(L2Character.ZONE_PEACE)
						|| _player.isInsideZone(L2Character.ZONE_CLANHALL)
						|| _player.isInsideZone(L2Character.ZONE_FORT)
						|| _player.isInsideZone(L2Character.ZONE_TOWN)
						|| _player.isInsideZone(L2Character.ZONE_CASTLE) || _player.isInJail() || _player.isInGludin())
				{
					//do nothing
				}
				else if (_player.isInsideZone(L2Character.ZONE_NOSUMMONFRIEND))
				{
					_player.sendMessage("You are being ported to town because you are in an no-recall zone");
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
				else if (_player.isInsideZone(L2Character.ZONE_CHAOTIC))
				{
					_player.sendMessage("You are being ported to town because you are in a Chaotic Event Zone");
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
				else if (System.currentTimeMillis() - _player.getLastAccess() >= 2700000) // 45 mins of not logging in
				{
					_player.sendMessage("You are being ported to town due to inactivity");
					_player.setIsPendingRevive(true);
					_player.teleToLocation(TeleportWhereType.Town);
				}
			}
			
			_player.onPlayerEnter();
		}
	}
	catch (Exception e)
	{e.printStackTrace();}
}
}


private void engage(L2PcInstance cha)
{
	int _chaid = cha.getObjectId();
	
	for(Couple cl: CoupleManager.getInstance().getCouples())
	{
		if (cl.getPlayer1Id()==_chaid || cl.getPlayer2Id()==_chaid)
		{
			if (cl.getMaried())
				cha.setIsThisCharacterMarried(true);
			
			cha.setCoupleId(cl.getId());
			
			if (cl.getPlayer1Id()==_chaid)
				cha.setPartnerId(cl.getPlayer2Id());
			
			else
				cha.setPartnerId(cl.getPlayer1Id());
		}
	}
}


private void notifyPartner(L2PcInstance cha,int partnerId)
{
	if (cha.getPartnerId()!=0)
	{
		L2PcInstance partner;
		int objId = cha.getPartnerId();
		
		try
		{
			partner = (L2PcInstance)L2World.getInstance().findObject(cha.getPartnerId());
			
			if (partner != null)
				partner.sendMessage("Your Partner has logged in.");
			
			partner = null;
		}
		catch (ClassCastException cce)
		{
			_log.warning("Wedding Error: ID "+objId+" is now owned by a(n) "+L2World.getInstance().findObject(objId).getClass().getSimpleName());
		}
	}
}


private void notifyFriends(L2PcInstance cha)
{
	Connection con = null;
	
	try
	{
		con = L2DatabaseFactory.getInstance().getConnection();
		PreparedStatement statement;
		statement = con.prepareStatement("SELECT friend_name FROM character_friends WHERE charId=?");
		statement.setInt(1, cha.getObjectId());
		ResultSet rset = statement.executeQuery();
		
		L2PcInstance friend;
		String friendName;
		
		SystemMessage sm = new SystemMessage(SystemMessageId.FRIEND_S1_HAS_LOGGED_IN);
		sm.addString(cha.getName());
		
		while (rset.next())
		{
			friendName = rset.getString("friend_name");
			
			friend = L2World.getInstance().getPlayer(friendName);
			
			if (friend != null) //friend logged in.
			{
				friend.sendPacket(new FriendList(friend));
				friend.sendPacket(sm);
			}
		}
		sm = null;
		
		rset.close();
		statement.close();
	}
	catch (Exception e)
	{
		_log.log(Level.SEVERE, "Error restoring friend data.", e);
	}
	finally
	{
		try {con.close();} catch (Exception e){}
	}
}

/**
 * @param activeChar
 */
private void notifyClanMembers(L2PcInstance activeChar)
{
	L2Clan clan = activeChar.getClan();
	
	// This null check may not be needed anymore since notifyClanMembers is called from within a null check already. Please remove if we're certain it's ok to do so.
	if (clan != null)
	{
		clan.getClanMember(activeChar.getObjectId()).setPlayerInstance(activeChar);
		SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN);
		msg.addString(activeChar.getName());
		clan.broadcastToOtherOnlineMembers(msg, activeChar);
		msg = null;
		clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(activeChar), activeChar);
	}
}

/**
 * @param activeChar
 */
private void notifySponsorOrApprentice(L2PcInstance activeChar)
{
	if (activeChar.getSponsor() != 0)
	{
		L2PcInstance sponsor = (L2PcInstance)L2World.getInstance().findObject(activeChar.getSponsor());
		
		if (sponsor != null)
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
			msg.addString(activeChar.getName());
			sponsor.sendPacket(msg);
		}
	}
	else if (activeChar.getApprentice() != 0)
	{
		L2PcInstance apprentice = (L2PcInstance)L2World.getInstance().findObject(activeChar.getApprentice());
		
		if (apprentice != null)
		{
			SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
			msg.addString(activeChar.getName());
			apprentice.sendPacket(msg);
		}
	}
}

/*	*//**
 *//*
	private String getText(String string)
	{
		try
		{
			String result = new String(Base64.decode(string), "UTF-8");
			return result;
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}*/

private void loadTutorial(L2PcInstance player)
{
	QuestState qs = player.getQuestState("255_Tutorial");
	
	if (qs != null)
		qs.getQuest().notifyEvent("UC", null, player);
}

/* (non-Javadoc)
 * @see net.sf.l2j.gameserver.clientpackets.ClientBasePacket#getType()
 */
@Override
public String getType()
{
	return _C__03_ENTERWORLD;
}

private void setPledgeClass(L2PcInstance activeChar)
{
	int pledgeClass = 0;
	
	// This null check may not be needed anymore since setPledgeClass is called from within a null check already. Please remove if we're certain it's ok to do so.
	if (activeChar.getClan() != null)
		pledgeClass = activeChar.getClan().getClanMember(activeChar.getObjectId()).calculatePledgeClass(activeChar);
	
	if (activeChar.isNoble() && pledgeClass < 5)
		pledgeClass = 5;
	
	if (activeChar.isHero() && pledgeClass < 8)
		pledgeClass = 8;
	
	activeChar.setPledgeClass(pledgeClass);
}
}