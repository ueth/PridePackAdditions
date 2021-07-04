package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.classes.Archer;
import net.sf.l2j.gameserver.fairgames.classes.Assassin;
import net.sf.l2j.gameserver.fairgames.classes.Mage;
import net.sf.l2j.gameserver.fairgames.classes.Warrior;
import net.sf.l2j.gameserver.fairgames.html.HtmlHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class FairGamesBBSManager {
    public static FairGamesBBSManager getInstance() { return FairGamesBBSManager.SingletonHolder._instance; }

    public void parsecmd(final String command, final L2PcInstance player) {
        final String path = "data/html/fairGames/";
        String filepath = "";
        String content = "";
        if (command.startsWith("_bbsFGChooseSkill ")) {
            final int id = Integer.parseInt(command.substring(18));
            HtmlHandler.getInstance().learnAndShowSkillsBoard(player, id);
        }
        else if(command.startsWith("_bbsFGSkillPage ")){
            final int pageNum = Integer.parseInt(command.substring(16));
            HtmlHandler.getInstance().showSkillsBoard(player, pageNum);
        }
        else if(command.startsWith("_bbsFGSelectClass ")){
            final String className = command.substring(18);
            HtmlHandler.getInstance().chooseClassBoard(player, className);
        }

        else {
            final ShowBoard sb = new ShowBoard("<html><body><br><br><center>the command: " + command
                    + " is not implemented yet</center><br><br></body></html>",
                    "101");
            player.sendPacket(sb);
            player.sendPacket(new ShowBoard(null, "102"));
            player.sendPacket(new ShowBoard(null, "103"));
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


