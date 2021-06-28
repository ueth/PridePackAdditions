package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.Manager;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.MyTargetSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.templates.chars.L2NpcTemplate;

import java.util.StringTokenizer;

public class FairGamesNPCInstance extends L2NpcInstance {
    private static final String PARENT_DIR = "data/html/fairGames/";

    public FairGamesNPCInstance(int objectId, L2NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(L2PcInstance player, String command) {
        StringTokenizer st = new StringTokenizer(command, " ");
        String currentCommand = st.nextToken();

        // initial menu
        if (currentCommand.startsWith("register")) {
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            html.setFile(PARENT_DIR + "registNPC.htm");

            Manager.getInstance().register(player);

            html.replace("%register%", "Register");
            sendHtmlMessage(player, html);
        }

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
                NpcHtmlMessage html = new NpcHtmlMessage(1);
                html.setFile(PARENT_DIR + "registNPC.htm");

                html.replace("%register%", "Register");
                sendHtmlMessage(player, html);
            }
        }
        // Send a Server->Client ActionFailed to the L2PcInstance in order to
        // avoid that the client wait another packet
        player.sendPacket(ActionFailed.STATIC_PACKET);
    }

    private void sendHtmlMessage(L2PcInstance player, NpcHtmlMessage html) {
        html.replace("%objectId%", String.valueOf(getObjectId()));
        html.replace("%npcId%", String.valueOf(getNpcId()));
        player.sendPacket(html);
    }

    private void showReturnPage(L2PcInstance player) {
        String content =
                HtmCache.getInstance().getHtmForce(PARENT_DIR + player.getBufferPage() + ".htm");

        if (content == null) {
            NpcHtmlMessage html = new NpcHtmlMessage(1);
            html.setHtml("<html><body>My Text is missing</body></html>");
            player.sendPacket(html);
        } else {
            NpcHtmlMessage tele = new NpcHtmlMessage(getObjectId());
            tele.setHtml(content);
            sendHtmlMessage(player, tele);
        }
    }
}
