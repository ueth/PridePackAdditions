package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class RunesBBSManager {
    public static RunesBBSManager getInstance() { return RunesBBSManager.SingletonHolder._instance; }

    public void parsecmd(final String command, final L2PcInstance activeChar) {
        final String path = "data/html/CommunityBoard/customs/runes/";
        String filepath = "";
        String content = "";
        if (command.equals("_bbsRunesMain")) {
            filepath = path + "runes.html";
            content = HtmCache.getInstance().getHtm(null, filepath);
            content = this.replaceVars(activeChar, content);
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassPlayer ")){
            final int pageNum = Integer.parseInt(command.substring(21));
            filepath = path + "battlePassesList.html";
            content = HtmCache.getInstance().getHtm(filepath);
        }

        else {
            final ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
                    + " is not implemented yet</center><br><br></body></html>",
                    "101");
            activeChar.sendPacket(sb);
            activeChar.sendPacket(new ShowBoard(null, "102"));
            activeChar.sendPacket(new ShowBoard(null, "103"));
        }
    }

    private String replaceVars(final L2PcInstance activeChar, String content) {
        // %iplogs%
        String html = content;

        html = html.replace("%iplogs%", "asdf");

        return html;
    }

    protected void separateAndSend(final String html, final L2PcInstance acha) {
        if (html == null) {
            return;
        }
        acha.sendPacket(new ShowBoard(html, "101"));
    }

    private static class SingletonHolder {
        protected static final RunesBBSManager _instance;

        static {
            _instance = new RunesBBSManager();
        }
    }
}


