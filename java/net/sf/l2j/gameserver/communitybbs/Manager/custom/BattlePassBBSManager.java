package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.custom.battlepass.BattlePass;
import net.sf.l2j.gameserver.custom.battlepass.BattlePassPlayer;
import net.sf.l2j.gameserver.custom.battlepass.BattlePassTable;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class BattlePassBBSManager {
    public static BattlePassBBSManager getInstance() {
        return BattlePassBBSManager.SingletonHolder._instance;
    }

    public void parsecmd(final String command, final L2PcInstance activeChar) {
        final String path = "data/html/CommunityBoard/customs/battlePasses/";
        String filepath = "";
        String content = "";
        if (command.equals("_bbsBattlePassMain")) {
            filepath = path + "battlePass.html";
            content = HtmCache.getInstance().getHtm(null, filepath);
            content = this.replaceVars(activeChar, content);
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassPlayer ")){
            final int pageNum = Integer.parseInt(command.substring(21));
            filepath = path + "battlePassesList.html";
            content = HtmCache.getInstance().getHtm(filepath);
            sendListOfBattlePasses(content, activeChar, pageNum);
        }
        else if(command.startsWith("_bbsBattlePassBuyPlayer ")){
            final int id = Integer.parseInt(command.substring(24));
            filepath = path + "battlePassesList.html";
            content = HtmCache.getInstance().getHtm(filepath);

            for(BattlePass battlePass : activeChar.getBattlePass().getBattlePasses()){
                if(battlePass.getId() == id){
                    if(battlePass.isAvailable() || battlePass.getPoints()!=0){
                        activeChar.sendMessage("You already own this Battle Pass");
                        sendListOfBattlePasses(content, activeChar, 0);
                        return;
                    }
                }
            }

            int itemIdNeed = BattlePassTable.getBattlePasses().get(id).getItemId();
            int amountNeed = BattlePassTable.getBattlePasses().get(id).getPrice();

            if(activeChar.destroyItemByItemId("Consume", itemIdNeed, amountNeed, activeChar, false))
                activeChar.getBattlePass().saveBattlePass(id, activeChar);
            else
                activeChar.sendMessage("You do not have the required items");

            sendListOfBattlePasses(content, activeChar, 0);
        }
        else if(command.equals("_bbsBattlePasses")){

            filepath = path + "battlePasses.html";
            content = HtmCache.getInstance().getHtm(filepath);
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassPreview ")){
            final int id = Integer.parseInt(command.substring(22));

            filepath = path + "previewBattlePassRewards.html";
            content = HtmCache.getInstance().getHtm(filepath);

            activeChar.getBattlePass().getBattlePassPages().fillRewards(id);

            content = content.replace("%replace%", activeChar.getBattlePass().getBattlePassPages().getRewardPage(0));
            content = content.replace("%nextPageButtons%", activeChar.getBattlePass().getBattlePassPages().fillNextPageRewardButtons());
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassPreviewNextPage ")){
            final int id = Integer.parseInt(command.substring(30));

            filepath = path + "previewBattlePassRewards.html";
            content = HtmCache.getInstance().getHtm(filepath);

            content = content.replace("%replace%", activeChar.getBattlePass().getBattlePassPages().getRewardPage(id));
            content = content.replace("%nextPageButtons%", activeChar.getBattlePass().getBattlePassPages().fillNextPageRewardButtons());
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassClan ")){
            final int pageNum = Integer.parseInt(command.substring(19));
            filepath = path + "battlePassesList.html";

            L2Clan clan = activeChar.getClan();
            if(clan == null)return;

            content = HtmCache.getInstance().getHtm(filepath);
            sendListOfBattlePassesClan(content, activeChar, pageNum, clan);
        }
        else if(command.startsWith("_bbsBattlePassClanPreview ")){
            final int id = Integer.parseInt(command.substring(26));

            filepath = path + "previewBattlePassRewards.html";
            content = HtmCache.getInstance().getHtm(filepath);

            L2Clan clan = activeChar.getClan();
            if(clan == null)return;

            clan.getBattlePass().getBattlePassClanPages().fillRewards(id);

            content = content.replace("%replace%", clan.getBattlePass().getBattlePassClanPages().getRewardPage(0));
            content = content.replace("%nextPageButtons%", clan.getBattlePass().getBattlePassClanPages().fillNextPageRewardButtons());
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassClanPreviewNextPage ")){
            final int id = Integer.parseInt(command.substring(34));

            L2Clan clan = activeChar.getClan();
            if(clan == null)return;

            filepath = path + "previewBattlePassRewards.html";
            content = HtmCache.getInstance().getHtm(filepath);

            content = content.replace("%replace%", clan.getBattlePass().getBattlePassClanPages().getRewardPage(id));
            content = content.replace("%nextPageButtons%", clan.getBattlePass().getBattlePassClanPages().fillNextPageRewardButtons());
            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsBattlePassBuyClan ")){
            final int id = Integer.parseInt(command.substring(22));
            filepath = path + "battlePassesList.html";
            content = HtmCache.getInstance().getHtm(filepath);

            L2Clan clan = activeChar.getClan();
            if(clan == null || clan.getLeader().getObjectId() != activeChar.getObjectId())return;

            for(BattlePass battlePass : clan.getBattlePass().getBattlePasses()){
                if(battlePass.getId() == id){
                    activeChar.sendMessage("You already own this Battle Pass");
                    sendListOfBattlePassesClan(content, activeChar, 0, clan);
                    return;
                }
            }

            int itemIdNeed = BattlePassTable.getBattlePasses().get(id).getItemId();
            int amountNeed = BattlePassTable.getBattlePasses().get(id).getPrice();

            if(activeChar.destroyItemByItemId("Consume", itemIdNeed, amountNeed, activeChar, true))
                clan.getBattlePass().saveBattlePass(id, clan);
            else
                activeChar.sendMessage("You do not have the required items");

            sendListOfBattlePassesClan(content, activeChar, 0, clan);
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

    private void sendListOfBattlePasses(String content, L2PcInstance player, int id){
        player.getBattlePass().getBattlePassPages().fillPages();

        content = content.replace("%replace%", player.getBattlePass().getBattlePassPages().getPage(id));
        content = content.replace("%nextPageButtons%", player.getBattlePass().getBattlePassPages().fillNextPageButtons());
        content = content.replace("%back%", "<button value=\"Back\" action=\"bypass _bbsBattlePassPlayer 0\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"80\" height=\"31\"/>");


        this.separateAndSend(content, player);
    }

    private void sendListOfBattlePassesClan(String content, L2PcInstance player, int id, L2Clan clan){
        clan.getBattlePass().getBattlePassClanPages().fillPages();

        content = content.replace("%replace%", clan.getBattlePass().getBattlePassClanPages().getPage(id));
        content = content.replace("%nextPageButtons%", clan.getBattlePass().getBattlePassClanPages().fillNextPageButtons());
        content = content.replace("%back%", "<button value=\"Back\" action=\"bypass _bbsBattlePassClan 0\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"80\" height=\"31\"/>");

        this.separateAndSend(content, player);
    }

    private static class SingletonHolder {
        protected static final BattlePassBBSManager _instance;

        static {
            _instance = new BattlePassBBSManager();
        }
    }
}


