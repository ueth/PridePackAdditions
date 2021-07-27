package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.fairgames.build.ItemsManager;
import net.sf.l2j.gameserver.fairgames.enums.BuildStage;
import net.sf.l2j.gameserver.fairgames.html.FGHtmlHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class FairGamesBBSManager {
    public static FairGamesBBSManager getInstance() { return FairGamesBBSManager.SingletonHolder._instance; }

    public void parsecmd(final String command, final L2PcInstance player) {
        if(player.getPlayerHandler().getBuildStage() == BuildStage.NONE)
            return;
        if (command.startsWith("_bbsFGChooseSkill ")) {
            final int id = Integer.parseInt(command.substring(18));

            switch (player.getPlayerHandler().getBuildStage()){
                case SKILLS_CHOOSE:
                    FGHtmlHandler.getInstance().learnAndShowSkillsBoard(player, id);
                    break;

                case BUFFS_CHOOSE:
                    FGHtmlHandler.getInstance().giveBuffAndShowBoard(player, id);
                    break;
            }
        }
        else if(command.startsWith("_bbsFGPage ")){
            final int pageNum = Integer.parseInt(command.substring(11));

            switch (player.getPlayerHandler().getBuildStage()){
                case SKILLS_CHOOSE:
                    FGHtmlHandler.getInstance().showSkillsBoard(player, pageNum);
                    break;

                case WEAPON_CHOOSE:
                case  ARMOR_CHOOSE:
                case JEWELS_CHOOSE:
                case TATTOO_CHOOSE:
                    FGHtmlHandler.getInstance().showItemsBoard(player, pageNum);
                    break;

                case BUFFS_CHOOSE:
                    FGHtmlHandler.getInstance().showBuffsBoard(player, pageNum);
                    break;
            }

        }
        else if(command.startsWith("_bbsFGSelectClass ")){
            final String className = command.substring(18);
            FGHtmlHandler.getInstance().chooseClassBoard(player, className);
        }
        else if(command.startsWith("_bbsFGSelectItem ")){
            final int id = Integer.valueOf(command.substring(17));

            switch (player.getPlayerHandler().getBuildStage()){
                case WEAPON_CHOOSE:
                    FGHtmlHandler.getInstance().chooseItem(player, ItemsManager.getWeapons().get(id).getItemId());
                    break;

                case ARMOR_CHOOSE:
                    FGHtmlHandler.getInstance().chooseItem(player, ItemsManager.getArmors().get(id).getItemId());
                    break;

                case JEWELS_CHOOSE:
                    FGHtmlHandler.getInstance().chooseItem(player, ItemsManager.getJewels().get(id).getItemId());
                    break;

                case TATTOO_CHOOSE:
                    FGHtmlHandler.getInstance().chooseItem(player, ItemsManager.getTattoos().get(id).getItemId());
                    break;
            }
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


