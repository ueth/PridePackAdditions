package net.sf.l2j.gameserver.fairgames.html;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.fairgames.build.SkillsManager;
import net.sf.l2j.gameserver.fairgames.classes.Archer;
import net.sf.l2j.gameserver.fairgames.classes.Assassin;
import net.sf.l2j.gameserver.fairgames.classes.Mage;
import net.sf.l2j.gameserver.fairgames.classes.Warrior;
import net.sf.l2j.gameserver.fairgames.enums.BuildStage;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;
import net.sf.l2j.gameserver.util.StringUtil;

public class FGHtmlHandler {
    private static FGHtmlHandler _instance = null;

    public void learnAndShowSkillsBoard(L2PcInstance player, int id){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        int pageNum = player.getPlayerHandler().getSkillPages().getCurrentPage();
        String className = player.getPlayerHandler().getClassName();

        for(L2Skill skill : player.getAllSkills())
            if(skill.getId() == SkillsManager.getClassSkills(className).get(id).getId()){
                player.getPlayerHandler().getSkillPages().fillSkillsPages();
                content = HtmCache.getInstance().getHtm(filepath);
                content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getPage(pageNum));
                content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextPageButtons());
                separateAndSend(content, player);
                return;
            }


        player.addSkill(SkillsManager.getClassSkills(className).get(id).getId());

        player.getPlayerHandler().getSkillPages().fillSkillsPages();

        content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getPage(pageNum));
        content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextPageButtons());

        player.getPlayerHandler().getFGClass().incSkillCounter();

        /*If we surpass the max skills for a class then we show the weapons board*/
        if(player.getPlayerHandler().getFGClass().getMaxSkills() == player.getPlayerHandler().getFGClass().getSkillCounter()){
            player.getPlayerHandler().switchBuildStage();
            showItemsBoard(player,0);
            return;
        }

        separateAndSend(content, player);
    }

    public void showSkillsBoard(L2PcInstance player, int pageNum){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        player.getPlayerHandler().getSkillPages().setCurrentPage(pageNum);
        player.getPlayerHandler().getSkillPages().fillSkillsPages();

        content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getPage(pageNum));
        content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextPageButtons());

        this.separateAndSend(content, player);
    }

    public void giveBuffAndShowBoard(L2PcInstance player, int id){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        int pageNum = player.getPlayerHandler().getSkillPages().getCurrentPage();

        for(L2Effect effect : player.getAllEffects())
            if(effect.getSkill().getId() == SkillsManager.getFGBuff(id).getId()){
                player.getPlayerHandler().getSkillPages().fillBuffsPages();
                content = HtmCache.getInstance().getHtm(filepath);
                content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getBuffPage(pageNum));
                content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextBuffPageButtons());
                separateAndSend(content, player);
                return;
            }

        SkillTable.getInstance().getInfo(SkillsManager.getFGBuff(id).getId(), 1).getEffects(player, player);//adding buff to player

        player.getPlayerHandler().getSkillPages().fillBuffsPages();

        content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getBuffPage(pageNum));
        content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextBuffPageButtons());

        player.getPlayerHandler().getFGClass().incBuffCounter();

        /*If we surpass the max buffs for a class then we show nothing*/
        if(player.getPlayerHandler().getFGClass().getMaxBuffs() == player.getPlayerHandler().getFGClass().getBuffCounter()){
            player.getPlayerHandler().switchBuildStage();
            return;
        }

        separateAndSend(content, player);
    }

    public void showBuffsBoard(L2PcInstance player, int pageNum){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        player.getPlayerHandler().getSkillPages().setCurrentPage(pageNum);
        player.getPlayerHandler().getSkillPages().fillBuffsPages();

        content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%skillPage%", player.getPlayerHandler().getSkillPages().getBuffPage(pageNum));
        content = content.replace("%nextPage%", player.getPlayerHandler().getSkillPages().fillNextBuffPageButtons());

        this.separateAndSend(content, player);
    }

    public void chooseItem(L2PcInstance player, int id){
        player.getPlayerHandler().addItem(id);
        player.getPlayerHandler().switchBuildStage();

        if(checkIfItemStage(player))
            showItemsBoard(player, 0);
        else
            showBuffsBoard(player, 0);
    }

    public void showItemsBoard(L2PcInstance player, int pageNum){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        player.getPlayerHandler().getItemPages().setCurrentPage(pageNum);
        player.getPlayerHandler().getItemPages().fillItemsPages();

        content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%skillPage%", player.getPlayerHandler().getItemPages().getPage(pageNum));
        content = content.replace("%nextPage%", player.getPlayerHandler().getItemPages().fillNextPageButtons());

        this.separateAndSend(content, player);
    }

    public void chooseClassBoard(L2PcInstance player, String className){
        switch (className){
            case "archer": player.getPlayerHandler().setClass(new Archer());break;
            case "warrior": player.getPlayerHandler().setClass(new Warrior());break;
            case "mage": player.getPlayerHandler().setClass(new Mage());break;
            case "assassin": player.getPlayerHandler().setClass(new Assassin());break;
        }
        //player.getPlayerHandler().switchBuildStage();

        /*After a player chooses class, show skills*/
        //showSkillsBoard(player,0);
    }

    boolean checkIfItemStage(L2PcInstance player){
        switch (player.getPlayerHandler().getBuildStage()){
            case WEAPON_CHOOSE:
            case  ARMOR_CHOOSE:
            case JEWELS_CHOOSE:
            case TATTOO_CHOOSE:
                return true;
            default:
                return false;
        }
    }

    public void showFairGamesStats(L2PcInstance player){
        final String path = "data/html/fairGames/stats/";
        String filepath = path + "stats.html";
        String content = HtmCache.getInstance().getHtm(filepath);

        content = content.replace("%gamesDone%", String.valueOf(player.getPlayerStats().getGamesDone()));
        content = content.replace("%compWon%", String.valueOf(player.getPlayerStats().getCompetitionWon()));
        content = content.replace("%compLost%", String.valueOf(player.getPlayerStats().getCompetitionLost()));
        content = content.replace("%points%", String.valueOf(player.getPlayerStats().getPoints()));
        content = content.replace("%streak%", String.valueOf(player.getPlayerStats().getStreak()));

        double winrate = ((double)player.getPlayerStats().getCompetitionWon() / (double)player.getPlayerStats().getGamesDone())*100;
        String winratecolor = "009933";

        if(winrate<50)
            winratecolor = "cc0000";

        StringBuilder sb = StringUtil.startAppend(1000, "");

        for(String match : player.getPlayerStats().getMatchHistory()){
            StringUtil.append(sb, "<tr>" +
                    "<td width=80 align=center>" +
                    "<font name=\"hs9\">" + match + "</font>" +
                    "</td>" +
                    "</tr>");
        }

        content = content.replace("%matchHistory%", sb.toString());

        content = content.replace("%winRateColor%", winratecolor);
        content = content.replace("%winRate%", winrate + "%");

        this.separateAndSend(content, player);
    }

    public void showFairGamesRankings(L2PcInstance player){
        final String path = "data/html/fairGames/stats/";
        String filepath = path + "rankings.html";

        String content = HtmCache.getInstance().getHtm(filepath);


        this.separateAndSend(content, player);
    }

    public void showFairGamesScheme(L2PcInstance player){
        final String path = "data/html/fairGames/";
        String filepath = path + "scheme.html";
        String content = HtmCache.getInstance().getHtm(filepath);
        this.separateAndSend(content, player);
    }

    public void showFairGamesClassStats(L2PcInstance player){
        final String path = "data/html/fairGames/stats/";
        String filepath = path + "classStats.html";
        String content = HtmCache.getInstance().getHtm(filepath);
        this.separateAndSend(content, player);
    }

    protected void separateAndSend(final String html, final L2PcInstance acha) {
        if (html == null) {
            return;
        }
        acha.sendPacket(new ShowBoard(html, "101"));
    }

    public static FGHtmlHandler getInstance(){
        if (_instance == null)
            synchronized (FGHtmlHandler.class) {
                if (_instance == null)
                    _instance = new FGHtmlHandler();
            }

        return _instance;
    }
}
