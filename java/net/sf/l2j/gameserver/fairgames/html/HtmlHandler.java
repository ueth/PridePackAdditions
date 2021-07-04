package net.sf.l2j.gameserver.fairgames.html;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.build.SkillsManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class HtmlHandler {

    public void showSkillsBoard(L2PcInstance player, int id){
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

    protected void separateAndSend(final String html, final L2PcInstance acha) {
        if (html == null) {
            return;
        }
        acha.sendPacket(new ShowBoard(html, "101"));
    }
}
