package net.sf.l2j.gameserver.fairgames.html;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.build.SkillsManager;
import net.sf.l2j.gameserver.fairgames.classes.Archer;
import net.sf.l2j.gameserver.fairgames.classes.Assassin;
import net.sf.l2j.gameserver.fairgames.classes.Mage;
import net.sf.l2j.gameserver.fairgames.classes.Warrior;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

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

        if(player.getPlayerHandler().getFGClass().getMaxSkills() == player.getPlayerHandler().getFGClass().getSkillCounter()){
            player.getPlayerHandler().switchBuildStage();
            filepath = path + "skills.html";
            content = HtmCache.getInstance().getHtm(filepath);
            separateAndSend(content, player);
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

    public void chooseClassBoard(L2PcInstance player, String className){
        final String path = "data/html/fairGames/";
        String filepath = path + "skills.html";
        String content = null;

        switch (className){
            case "archer": player.getPlayerHandler().setClass(new Archer());break;
            case "warrior": player.getPlayerHandler().setClass(new Warrior());break;
            case "mage": player.getPlayerHandler().setClass(new Mage());break;
            case "assassin": player.getPlayerHandler().setClass(new Assassin());break;
        }
        player.getPlayerHandler().switchBuildStage();

        showSkillsBoard(player,0);
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
