package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class FairGamesBBSManager {
    public static FairGamesBBSManager getInstance() { return FairGamesBBSManager.SingletonHolder._instance; }

    public void parsecmd(final String command, final L2PcInstance activeChar) {
        final String path = "data/html/fairGames/";
        String filepath = "";
        String content = "";
        if (command.equals("_bbsRunesMain")) {
            filepath = path + "skills.html";

            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(1));
            content = content.replace("%nextPage%", activeChar.getRunePlayer().getRunePages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsRunesPage ")){
            final int pageNum = Integer.parseInt(command.substring(14));
            filepath = path + "runes.html";

            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);


            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(pageNum));
            content = content.replace("%nextPage%", activeChar.getRunePlayer().getRunePages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
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

    protected void separateAndSend(final String html, final L2PcInstance acha) {
        if (html == null) {
            return;
        }
        acha.sendPacket(new ShowBoard(html, "101"));
    }

    private static class SingletonHolder {
        protected static final FairGamesBBSManager _instance;

        static {
            _instance = new FairGamesBBSManager();
        }
    }
}


