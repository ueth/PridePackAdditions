package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.html.FGHtmlHandler;
import net.sf.l2j.gameserver.network.serverpackets.*;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

import java.util.StringTokenizer;

public class FairGamesBuildManagerInstance extends L2NpcInstance {
    private static final String PARENT_DIR = "data/html/fairGames/";

    public FairGamesBuildManagerInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        super.onBypassFeedback(player, command);
    }

    @Override
    public void onAction(L2PcInstance player) {
        player.setLastFolkNPC(this);

        if (!canTarget(player)) return;

        // Check if the L2PcInstance already target the L2NpcInstance
        if (this != player.getTarget()) {
            // Set the target of the L2PcInstance player
            player.setTarget(this);

            // Send a Server->Client packet MyTargetSelected to the L2PcInstance
            // player
            MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
            player.sendPacket(my);

            // Send a Server->Client packet ValidateLocation to correct the
            // L2NpcInstance position and heading on the client
            player.sendPacket(new ValidateLocation(this));
        } else {
            // Calculate the distance between the L2PcInstance and the
            // L2NpcInstance
            if (!canInteract(player)) {
                // Notify the L2PcInstance AI with AI_INTENTION_INTERACT
                // note: commented out so the player must stand close
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
            } else {
                if(!player.isInFairGame())
                    return;

                String content;
                switch(player.getPlayerHandler().getBuildStage()){
                    case CLASS_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"classes.html");
                        separateAndSend(content, player);
                        break;

                    case SKILLS_CHOOSE:
                        FGHtmlHandler.getInstance().showSkillsBoard(player, 0);
                        break;

                    case WEAPON_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"weapons.html");
                        separateAndSend(content, player);
                        break;

                    case ARMOR_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"armors.html");
                        separateAndSend(content, player);
                        break;

                    case BUFFS_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"buffs.html");
                        separateAndSend(content, player);
                        break;

                    case JEWELS_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"jewels.html");
                        separateAndSend(content, player);
                        break;

                    case TATTOO_CHOOSE:
                        content = HtmCache.getInstance().getHtm(PARENT_DIR+"tattoos.html");
                        separateAndSend(content, player);
                        break;
                }
            }
        }
        // Send a Server->Client ActionFailed to the L2PcInstance in order to
        // avoid that the client wait another packet
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }



    protected void separateAndSend(final String html, final L2PcInstance acha) {
        if (html == null) {
            return;
        }
        acha.sendPacket(new ShowBoard(html, "101"));
    }
}
