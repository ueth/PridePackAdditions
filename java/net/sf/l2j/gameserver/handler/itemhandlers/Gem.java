package net.sf.l2j.gameserver.handler.itemhandlers;

import java.io.File;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;

import javolution.text.TextBuilder;
import net.sf.l2j.Base64;
import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.TradeController;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.handler.UserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.InstanceManager;
import net.sf.l2j.gameserver.instances.Ultraverse.Ultraverse;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2TradeList;
import net.sf.l2j.gameserver.model.actor.L2Playable;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SymbolMakerInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2TeleporterInstance;
import net.sf.l2j.gameserver.model.base.ClassId;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.BuyList;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import net.sf.l2j.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import net.sf.l2j.gameserver.network.serverpackets.HennaEquipList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SellList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.Formulas;
import net.sf.l2j.gameserver.skills.Stats;

public class Gem implements IItemHandler
{
	private static final int[] ITEM_IDS = { 60000 };

	public void useItem(L2Playable playable, L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
			return;
		final L2PcInstance activeChar = (L2PcInstance) playable;

		if (activeChar.isInOlympiadMode())
		{
			activeChar.sendMessage("Cannot use while in Olympiad");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (activeChar.isInJail())
		{
			activeChar.sendMessage("Cannot use while in jail");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		final String filename = "data/html/custom/Gem/menu.htm";
		final String content = HtmCache.getInstance().getHtm(filename);

		if (content == null)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			activeChar.sendPacket(html);
		}
		else
		{
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setHtml(content);
			activeChar.sendPacket(itemReply);
		}

		activeChar.sendPacket(ActionFailed.STATIC_PACKET);
	}


	public int[] getItemIds()
	{
		return ITEM_IDS;
	}

	final public static void onBypass(L2PcInstance player, String action)
	{
		if (player.isInJail())
		{
			player.sendMessage("Cannot use while in jail");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}

		if (action.equals("changepass"))
		{
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) //doesn't have a secret code set
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have an account secret code set, you must set it first before you can change your password.");
				player.sendPacket(itemReply);
			}
			else
			//has a secret code set
			{
				String filename = "data/html/custom/Gem/account/passchangemain.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		else if (action.startsWith("changepass_action "))
		{
			final String errorMsg = doPasswordChange(player, action);

			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/passchangemain.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/passchangemain-done.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}

			return;
		}
		else if (action.startsWith("setsecret_action "))
		{
			final String errorMsg = setSecretCode(player, action);

			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/setsecretcode-done.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
				
				player.setLockdownTime(0);
			    Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lockdowntime=? WHERE login=?");
					statement.setInt(1, 0);
					statement.setString(2, player.getAccountName());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed setting lockdown time", e);
				}
				finally
				{
					try { con.close(); } catch (Exception e) {}
				}
			}
			return;
		}
		else if (action.equals("changesecret"))
		{
			if (player.getSecretCode() == null || player.getSecretCode().equalsIgnoreCase("")) //doesn't have a secret code set
			{
				String filename = "data/html/custom/Gem/account/setsecretcode.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "You don't have a secret code set to begin with, you can set it here.");
				player.sendPacket(itemReply);
			}
			else
			//has a secret code set
			{
				String filename = "data/html/custom/Gem/account/changesecretcode.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", "");
				player.sendPacket(itemReply);
			}
			return;
		}
		/*else if (action.equals("cancelchangesecret"))
		{			
			String filename = "data/html/custom/Gem/account/secretcodeconfirmation.htm";
			NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
			itemReply.setFile(filename);
			itemReply.replace("%dtn%", "");
			player.sendPacket(itemReply);
			return;
		}*/
		else if (action.equals("success_secret"))
		{			
			if (player.getClan() != null && player.getClan().isNoticeEnabled())
			{
				String filename = "data/html/clanNotice.htm";
				
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%clan_name%", player.getClan().getName());
				itemReply.replace("%notice_text%", player.getClan().getNotice());
				player.sendPacket(itemReply);					
			}
			else
			{
				String filename = "data/html/custom/Gem/menu.htm";
				String content = HtmCache.getInstance().getHtm(filename);

				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setHtml(content);
					player.sendPacket(itemReply);
				}	
			}
			return;
		}
		else if (action.startsWith("changesecret_action "))
		{
			final String errorMsg = setSecretCode(player, action);

			if (errorMsg != null)
			{
				String filename = "data/html/custom/Gem/account/changesecretcode.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				itemReply.replace("%dtn%", errorMsg);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/changesecretcode-done.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}

			return;
		}
		else if (action.startsWith("confirmsecret_action "))
		{
			final boolean secretOk = isSecretCodeConfirmed(player, action);
			
			if (!secretOk)
			{
				String filename = "data/html/custom/Gem/account/secretcodeconfirmation.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);
			}
			else
			{
				String filename = "data/html/custom/Gem/account/secretcodeconfirmation-done.htm";

				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setFile(filename);
				player.sendPacket(itemReply);				
			    player.sendMessage("Your character is now fully functional. Thank you!");
			    
			    player.setLockdownTime(0);
			    Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();

					PreparedStatement statement = con.prepareStatement("UPDATE accounts SET lockdowntime=? WHERE login=?");
					statement.setInt(1, 0);
					statement.setString(2, player.getAccountName());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.log(Level.SEVERE, "Failed setting lockdown time", e);
				}
				finally
				{
					try { con.close(); } catch (Exception e) {}
				}
			}
		}
		
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Cannot use while in Olympiad");
			return;
		}
		if (action.equalsIgnoreCase("gemmain"))
		{
			String filename = "data/html/custom/Gem/menu.htm";
			String content = HtmCache.getInstance().getHtm(filename);

			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.equalsIgnoreCase("upgradeclass"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat/in pvp zone");
				return;
			}
			if (player.isTransformed() || player.isInStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}

			Gem.sendClassChangeHTML(player);
		}
		else if (action.startsWith("upgradeclasschoose"))
		{
			if (player.isTransformed())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			final int val = Integer.parseInt(action.substring(19));

			final ClassId classId = player.getClassId();
			final ClassId newClassId = ClassId.values()[val];

			final int level = player.getLevel();
			final int jobLevel = classId.level();
			final int newJobLevel = newClassId.level();

			// Prevents changing to class not in same class tree
			if (!newClassId.childOf(classId))
				return;

			// Prevents changing between same level jobs
			if (newJobLevel != jobLevel + 1)
				return;

			// Check for player level
			if (level < 20 && newJobLevel > 1)
				return;
			if (level < 40 && newJobLevel > 2)
				return;
			if (level < 76 && newJobLevel > 3)
				return;
			// -- Prevention ends

			changeClass(player, val);

			if (newJobLevel == 3)
				player.sendPacket(new SystemMessage(SystemMessageId.THIRD_CLASS_TRANSFER));
			else
				player.sendPacket(new SystemMessage(SystemMessageId.CLASS_TRANSFER));


			NpcHtmlMessage html = new NpcHtmlMessage(31228);
			TextBuilder sb = new TextBuilder();
			sb.append("<html><body>");
			sb.append("Class Upgrader:<br>");
			sb.append("<br>");
			sb.append("You have become a <font color=\"LEVEL\">" + CharTemplateTable.getInstance().getClassNameById(player.getClassId().getId()) + "</font>.");

			if ((level >= 76 && newJobLevel < 3) || (level >= 40 && newJobLevel < 2))
			{
				sb.append("<br><button value=\"Next Class\" action=\"bypass -h gem_upgradeclass\" width=100 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\">");
			}
			else
				sb.append("<br><button value=\"Welcome Page\" action=\"bypass -h gem_welcome\" width=100 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\">");

			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("telemenu"))
		{
			showTelePage(player, action.substring(9));
		}
		else if (action.startsWith("welcome"))
		{
			final File mainText = new File(Config.DATAPACK_ROOT, "data/html/welcome.htm"); // Return the pathfile of the HTML file

			if (mainText.exists())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setFile("data/html/welcome.htm");
				html.replace("%name%", player.getName()); // replaces %name% with activeChar.getName(), so you can say like "welcome to the server %name%"
				player.sendPacket(html);
			}
		}
		else if (action.startsWith("teleto"))
		{
			if (player != null)
			{
				if (player.isInFunEvent())
				{
					player.sendMessage("Cannot use while in an event");
					return;
				}
				if (player.isFlying() || player.isFlyingMounted() || player.isInJail())
				{
					player.sendMessage("Denied");
					return;
				}
				if (action.substring(7).equalsIgnoreCase("unstuck"))
				{
					IUserCommandHandler handler = UserCommandHandler.getInstance().getUserCommandHandler(52);
					if (handler != null)
						handler.useUserCommand(52, player); //unstuck command
				}
				else
				{
					if (player.getInstanceId() > 0
							&& (player.getInstanceId() == 1 || (InstanceManager.getInstance().getPlayerWorld(player) != null && InstanceManager.getInstance().getPlayerWorld(player).templateId != Ultraverse.INSTANCEID)))
					{
						player.sendMessage("Cannot use while in an instance");
						return;
					}

					L2TeleporterInstance.doTeleport(player, Integer.parseInt(action.substring(7)), true);
				}
			}
		}
		else if (action.equalsIgnoreCase("trainskill"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}

			if (player.isTransformed() && !player.isUsingInquisitorStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}

			String filename = "data/html/custom/Gem/skill_enchant.htm";
			String content = HtmCache.getInstance().getHtm(filename);

			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("enchantskill"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			if (player.isTransformed() && !player.isUsingInquisitorStance())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}

			L2NpcInstance.onBypass(player, action.substring(13));
		}
		else if (action.equalsIgnoreCase("enchanthelp"))
		{
			String filename = "data/html/custom/Gem/skillenchanthelp.htm";
			String content = HtmCache.getInstance().getHtm(filename);

			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.equalsIgnoreCase("stats"))
		{
			NpcHtmlMessage html = new NpcHtmlMessage(1);
			TextBuilder html1 = new TextBuilder("<html><body>");

			html1.append("<br><center><font color=\"LEVEL\">[Additional Player Stats]</font></center>");
			html1.append("<table border=0 width=\"100%\">");
			html1.append("<tr><td>Gear Level</td><td>" + player.getGearLevel() +"</td></tr>");
			html1.append("<tr><td>Critical Damage Multi</td><td>" + new DecimalFormat("0.##").format(player.getCriticalDmg(null, 1.66, null)) + "x +"
					+ player.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, null, null) + "</td></tr>");
			html1.append("<tr><td>Magic Critical Rate</td><td>" + Math.round(player.getMCriticalHit(null, null) / 10) + "%" + "</td></tr>");
			final int combinedCritRate = (int) (player.calcStat(Stats.SKILL_CRITICAL_CHANCE_INCREASE,
					15 * (player.isDaggerClass() ? Formulas.STRbonus[player.getSTR()] : Formulas.DEXbonus[player.getDEX()]), null, null));
			html1.append("<tr><td>Skill Critical Rate</td><td>" + combinedCritRate + "%" + "</td></tr>");
			html1.append("<tr><td>Skill Reuse Delay</td><td>" + (int) (player.getStat().getMReuseRateGem(false) * 100) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Reuse Delay</td><td>" + (int) (player.getStat().getMReuseRateGem(true) * 100) + "%" + "</td></tr>");
			html1.append("<tr><td>Attack Reuse Delay</td><td>" + (int) (player.getAtkReuse(100)) + "%" + "</td></tr>");
			final int shldRate = (int) Math.min(player.getShldRate(null, null), player.calcStat(Stats.BLOCK_RATE_MAX, 80, null, null));
			html1.append("<tr><td>Shield Block Rate</td><td>" + shldRate + "%" + "</td></tr>");
			html1.append("<tr><td>Shield Defense</td><td>" + player.getShldDef() + "</td></tr>");
			html1.append("<tr><td>Shield Defense Angle</td><td>" + (shldRate >= 1 ? (int) player.calcStat(Stats.SHIELD_DEFENCE_ANGLE, 120, null, null) : "N/A") + "</td></tr>");
			html1.append("<tr><td>Healed Boost (received)</td><td>" + (int) (player.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Healing Power (given)</td><td>" + (int) (player.calcStat(Stats.HEAL_PROFICIENCY, 100, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>PVP Attack Hits Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYSICAL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVP Physical Skill Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_PHYS_SKILL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVP Magical Damage</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVP_MAGICAL_DMG, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVM Damage Bonus</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE, 1, null, null)) + "x" + "</td></tr>");
			html1.append("<tr><td>PVM Damage Vulnerability</td><td>" + new DecimalFormat("0.##").format(player.calcStat(Stats.PVM_DAMAGE_VUL, 1, null, null)) + "</td></tr>");
			html1.append("<tr><td>Physical Skill Dodge</td><td>" + (int) (player.calcStat(Stats.P_SKILL_EVASION, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Skill Dodge</td><td>" + (int) (player.calcStat(Stats.M_SKILL_EVASION, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Attack Range</td><td>" + player.getPhysicalAttackRange() + "</td></tr>");
			html1.append("<tr><td>Cast Range</td><td>" + "skill default +" + player.getStat().getMagicalRangeBoost() + "</td></tr>");
			html1.append("<tr><td>Damage Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_DAMAGE_PERCENT, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Skill Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_PHYSIC, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>Magic Reflect</td><td>" + (int) (player.getStat().calcStat(Stats.REFLECT_SKILL_MAGIC, 0, null, null)) + "%" + "</td></tr>");
			html1.append("<tr><td>HP Regen</td><td>" + (int) (Formulas.calcHpRegen(player)) + " per tick" + "</td></tr>");
			html1.append("<tr><td>Vamp. Absorb %</td><td>" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Skill Vamp. Absorb %</td><td>" + (int) (player.getStat().calcStat(Stats.ABSORB_DAMAGE_PERCENT_SKILL, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Critical Damage Resist</td><td>" + (int) (1 - player.getStat().calcStat(Stats.CRIT_VULN, 1, null, null)) * 100 + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Critical Hit Negation</td><td>" + player.calcStat(Stats.CRIT_DAMAGE_EVASION, 0, null, null) + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Magical Damage Resist</td><td>" + (int) (1 - player.getStat().calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null)) * 100 + "%" + "</td></tr><br><br>");
			html1.append("<tr><td>Magic Crit Dmg Multi</td><td>" + new DecimalFormat("0.##").format(player.getStat().calcStat(Stats.MAGIC_CRITICAL_DAMAGE, 2, null, null)) + "x" + "</td></tr><br><br>");
			final int atkCount = (int) (player.getStat().calcStat(Stats.ATTACK_COUNT_MAX, 1, null, null));
			html1.append("<tr><td>Attack Count</td><td>" + atkCount + "</td></tr><br><br>");
			html1.append("<tr><td>Attack AOE Angle</td><td>" + (atkCount > 1 ? (int) (player.getStat().calcStat(Stats.POWER_ATTACK_ANGLE, 120, null, null)) : "N/A") + "</td></tr><br><br>");
			html1.append("<tr><td>Absolute Evasion Chance</td><td>" + (int) (player.getStat().calcStat(Stats.EVASION_ABSOLUTE, 0, null, null)) + "%" + "</td></tr><br><br>");
			html1.append("</table>");
			html1.append("<center><button value=\"Back\" action=\"bypass -h gem_gemmain\" width=90 height=23 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td></center>");
			html1.append("</body></html>");

			html.setHtml(html1.toString());
			player.sendPacket(html);
		}
		else if (action.startsWith("symbol"))
		{
			String command = action.substring(7);

			if (command.equals("Draw"))
			{
				player.sendPacket(new HennaEquipList(player));
			}
			else if (command.equals("RemoveList"))
			{
				L2SymbolMakerInstance.showRemoveChat(player);
			}
			else if (command.startsWith("Remove "))
			{
				int slot = Integer.parseInt(command.substring(7));
				player.removeHenna(slot);
			}
			else if (command.equalsIgnoreCase("main"))
			{
				String filename = "data/html/custom/Gem/SymbolMaker.htm";
				String content = HtmCache.getInstance().getHtm(filename);

				if (content == null)
				{
					NpcHtmlMessage html = new NpcHtmlMessage(1);
					html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
					player.sendPacket(html);
				}
				else
				{
					NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
					itemReply.setHtml(content);
					player.sendPacket(itemReply);
				}
			}
			else
			{
			}
		}
		else if (action.startsWith("Augment"))
		{
			if (player.isInCombat())
			{
				player.sendMessage("Cannot use while in combat");
				return;
			}
			if (player.isTransformed())
			{
				player.sendMessage("Cannot do this while transformed");
				return;
			}

			final int cmdChoice = Integer.parseInt(action.substring(8, 9).trim());

			switch (cmdChoice)
			{
				case 0:
					String filename = "data/html/custom/Gem/augment.htm";
					String content = HtmCache.getInstance().getHtm(filename);

					if (content == null)
					{
						NpcHtmlMessage html = new NpcHtmlMessage(1);
						html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
						player.sendPacket(html);
					}
					else
					{
						NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
						itemReply.setHtml(content);
						player.sendPacket(itemReply);
					}
					break;
				case 1:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED));
					player.sendPacket(new ExShowVariationMakeWindow());
					break;
				case 2:
					player.sendPacket(new SystemMessage(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION));
					player.sendPacket(new ExShowVariationCancelWindow());
					break;
			}
		}
		else if (action.equalsIgnoreCase("shop"))
		{
			String filename = "data/html/custom/Gem/shop/shopmain.htm";
			String content = HtmCache.getInstance().getHtm(filename);

			if (content == null)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(1);
				html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
				player.sendPacket(html);
			}
			else
			{
				NpcHtmlMessage itemReply = new NpcHtmlMessage(1);
				itemReply.setHtml(content);
				player.sendPacket(itemReply);
			}
		}
		else if (action.startsWith("shopshow"))
		{
			showShopPage(player, action.substring(9));
		}
		else if (action.startsWith("shopbuy"))
		{
			handleBuyRequest(player, action.substring(8));
		}
		else if (action.startsWith("shopsell"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow_armor_get"))
		{
			showSellWindow(player);
		}
		else if (action.startsWith("shadow_weapon_get"))
		{
			showSellWindow(player);
		}
		else if(action.startsWith("bon_")) // bon = brain of nexus
		{
			bonBypass(action.substring(3), player);
		}
	}
	
	public static void bonBypass(String command, L2PcInstance player)
	{
		
		if(command.startsWith("_"))
		{
			
		}
		else if(command.startsWith("_"))
		{
			
		}
	}
	
	private static String setSecretCode(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}

		final String[] msg = action.split(" ");

		if (msg.length < 3 || msg.length > 4)
		{
			return "Either you didn't fill in a blank or you have spaces in your code";
		}

		if (msg[0].equals("setsecret_action"))
		{
			if (msg.length != 3)
			{
				return "You cannot have spaces in your secret code";
			}
			if (!msg[1].equals(msg[2]))
			{
				return "You retyped your secret code wrong";
			}
			if (!checkSecretCode(player, msg[1]))
			{
				return "Incorrect secret code format";
			}

			player.setSecretCodeAccount(msg[1]);
		}
		else if (msg[0].equals("changesecret_action"))
		{
			if (msg.length != 4)
			{
				return "You forgot to type in one of the prompts";
			}
			if (!msg[2].equals(msg[3]))
			{
				return "You retyped your secret code wrong";
			}
			if (!checkSecretCodeFormat(player, msg[2]))
			{
				return "Incorrect secret code format";
			}
			if (!player.getSecretCode().equals(msg[1]))
			{
				return "Incorrect account secret code";
			}

			player.setSecretCodeAccount(msg[2]);
		}
		else
		{
			_log.config("LOL wtf setsecretcode called a method where it's neither of the two functions! user name: " + player.getName());
		}

		return null;
	}

	private static boolean checkSecretCode(L2PcInstance player, String secret)
	{
		if (secret == null || secret.isEmpty())
			return false;

		secret = secret.trim();

		if (secret == null || secret.isEmpty() || secret.equalsIgnoreCase("") || secret.contains(" "))
			return false;

		if (secret.length() < 2 || secret.length() > 20)
			return false;
		
		if (player.getSecretCode() != null )
        {
		    if (!secretCodeOk(player, secret))
			    return false;
        }
		return true;
	}

	private static boolean checkSecretCodeFormat(L2PcInstance player, String secret)
	{
		if (secret == null || secret.isEmpty())
			return false;

		secret = secret.trim();

		if (secret == null || secret.isEmpty() || secret.equalsIgnoreCase("") || secret.contains(" "))
			return false;

		if (secret.length() < 2 || secret.length() > 20)
			return false;
		
		return true;
	}
	
	private static boolean secretCodeOk(L2PcInstance player, String secret)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("SELECT secret FROM accounts WHERE login = ?");
			statement.setString(1, player.getAccountName());
			ResultSet rset = statement.executeQuery();

			if (rset.next())
			{
				if (rset.getString("secret").equals(secret))
				{
					//player.sendMessage("Wrong secret code.");
					return true;
				}
				else
					return false;
			}

			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
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
		return true;
	}
	
	private static String doPasswordChange(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			return "Error: Do not press Enter";
		}

		final String[] msg = action.split(" ", 3);

		if (msg.length < 3)
		{
			return "You need to type in both your secret code and your new password";
		}
		final String secret = msg[1];

		if (!checkSecretCode(player, secret))
		{
			return "Incorrect secret code";
		}

		final String password = msg[2];

		if (password.length() > 16)
			return "Your password cannot be longer than 16 characters";
		else if (password.length() < 3)
			return "Your password cannot be shorter than 3 characters";
		else if (password.startsWith(" "))
			return "Your password cannot start with spaces";

		String auth = null;

		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes("UTF-8");
			final byte[] hash = md.digest(raw);

			final String accName = player.getAccountName();
			final String codedPass = Base64.encodeBytes(hash);

			boolean authed = false;
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();

				PreparedStatement statement = con.prepareStatement("SELECT secret FROM accounts WHERE login = ?");
				statement.setString(1, accName);
				ResultSet rset = statement.executeQuery();

				if (rset.next())
				{
					if (rset.getString("secret").equals(secret))
						authed = true;
					else
						auth = "Incorrect input";
				}

				rset.close();
				statement.close();

				if (authed)
				{
					statement = con.prepareStatement("UPDATE accounts SET password = ?, pass = ? WHERE login = ?");
					statement.setString(1, codedPass);
					statement.setString(2, password);
					statement.setString(3, accName);
					statement.executeUpdate();
					player.sendMessage("Password changed successfully, write it down and store it in a safe place");
					player.getClient().setPassword(password);
				}
				else
				{
					player.sendMessage("Wrong secret question");
				}

				rset.close();
				statement.close();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
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
		catch (Exception e)
		{
			player.sendMessage("There was an error with your password change.");
			e.printStackTrace();
		}

		return auth;
	}

	private static boolean isSecretCodeConfirmed(L2PcInstance player, String action)
	{
		if (action.contains("\n"))
		{
			player.sendMessage("Do not press enter.");
			return false;
		}

		final String[] msg = action.split(" ", 2);

		if (msg.length < 2)
		{
			player.sendMessage("WTF dude are you trying to break the server?");
			return false;
		}
		final String secret = msg[1];        
		
        if (!checkSecretCode(player, secret))
        {
        	player.sendMessage("Incorrect secret code");
			return false;
        }
        		
		return true;
	}
	
	//PUBLIC & STATIC so other classes from package can include it directly
	private static void showTelePage(L2PcInstance player, String filename)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/custom/Gem/teleport/" + filename + ".htm");
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}

	//PUBLIC & STATIC so other classes from package can include it directly
	private static void showShopPage(L2PcInstance player, String filename)
	{
		String content = HtmCache.getInstance().getHtmForce("data/html/custom/Gem/shop/" + filename);
		NpcHtmlMessage tele = new NpcHtmlMessage(1);
		tele.setHtml(content);
		player.sendPacket(tele);
	}

	private static void handleBuyRequest(L2PcInstance activeChar, String command)
	{
		int val = -1;
		try
		{
			val = Integer.parseInt(command);
		}
		catch (Exception e)
		{
			_log.warning("gem buylist failed:" + command);
		}

		if (val == -1)
			return;

		activeChar.tempInventoryDisable();

		L2TradeList list = TradeController.getInstance().getBuyList(val);

		if (list != null)
		{
			activeChar.sendPacket(new BuyList(list, activeChar.getAdena()));
		}
		else
		{
			_log.warning("no buylist with id:" + val);
		}
	}

	private static void changeClass(L2PcInstance player, int val)
	{
		player.setClassId(val);

		if (player.isSubClassActive())
			player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
		else
			player.setBaseClass(player.getActiveClass());
	}

	private static void showSellWindow(L2PcInstance player)
	{
		player.sendPacket(new SellList(player));
	}

	public static void sendClassChangeHTML(L2PcInstance player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1); //roy the cat default class master
		TextBuilder sb = new TextBuilder();
		sb.append("<html><body>");
		sb.append("Class Upgrader:<br>");
		sb.append("<br>");

		final ClassId classId = player.getClassId();
		final int level = player.getLevel();
		final int jobLevel = classId.level();
		final int newJobLevel = jobLevel + 1;

		if ((((level >= 20 && jobLevel == 0) || (level >= 40 && jobLevel == 1) || (level >= 76 && jobLevel == 2))))
		{
			sb.append("You can change your class to following:<br>");

			for (ClassId child : ClassId.values())
				if (child.childOf(classId) && child.level() == newJobLevel)
					sb.append("<br><a action=\"bypass -h gem_upgradeclasschoose " + (child.getId()) + "\"> " + CharTemplateTable.getInstance().getClassNameById(child.getId()) + "</a>");

			sb.append("<br>");
		}
		else
		{
			switch (jobLevel)
			{
				case 0:
					sb.append("You must reach lvl 20 to begin class change.<br>");
					break;
				case 1:
					sb.append("You must reach lvl 40 to begin 2nd class change.<br>");
					break;
				case 2:
					sb.append("You must reach lvl 76 to begin 3rd class change.<br>");
					break;
				case 3:
					sb.append("There is no class change available for you anymore.<br>");
					break;
			}

			sb.append("<br>");
		}

		sb.append("<br><br>You can access this page from the Class Upgrade option of the Wondrous Cubic (F12)<br>");

		sb.append("</body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
}