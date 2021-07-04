package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.fairgames.build.SkillsManager;
import net.sf.l2j.gameserver.fairgames.classes.Archer;
import net.sf.l2j.gameserver.fairgames.classes.Assassin;
import net.sf.l2j.gameserver.fairgames.classes.Mage;
import net.sf.l2j.gameserver.fairgames.classes.Warrior;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.ShowBoard;

public class FairGamesBBSManager {
    public static FairGamesBBSManager getInstance() { return FairGamesBBSManager.SingletonHolder._instance; }

    public void parsecmd(final String command, final L2PcInstance activeChar) {
        final String path = "data/html/fairGames/";
        String filepath = "";
        String content = "";
        if (command.startsWith("_bbsFGChooseSkill ")) {
            final int id = Integer.parseInt(command.substring(18));
            filepath = path + "skills.html";

            int pageNum = activeChar.getPlayerHandler().getSkillPages().getCurrentPage();
            String className = activeChar.getPlayerHandler().getClassName();

            for(L2Skill skill : activeChar.getAllSkills())
                if(skill.getId() == SkillsManager.getClassSkills(className).get(id).getId()){
                    activeChar.getPlayerHandler().getSkillPages().fillSkillsPages();
                    content = content.replace("%skillPage%", activeChar.getPlayerHandler().getSkillPages().getPage(pageNum));
                    content = content.replace("%nextPage%", activeChar.getPlayerHandler().getSkillPages().fillNextPageButtons());
                    this.separateAndSend(content, activeChar);
                    return;
                }


            activeChar.addSkill(SkillsManager.getClassSkills(className).get(id).getId());

            activeChar.getPlayerHandler().getSkillPages().fillSkillsPages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = content.replace("%skillPage%", activeChar.getPlayerHandler().getSkillPages().getPage(pageNum));
            content = content.replace("%nextPage%", activeChar.getPlayerHandler().getSkillPages().fillNextPageButtons());

            activeChar.getPlayerHandler().getFGClass().incSkillCounter();

            if(activeChar.getPlayerHandler().getFGClass().getMaxSkills() == activeChar.getPlayerHandler().getFGClass().getSkillCounter()){
                activeChar.getPlayerHandler().switchBuildStage();
                filepath = path + "skills.html";
                content = HtmCache.getInstance().getHtm(filepath);
                this.separateAndSend(content, activeChar);
            }

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsFGSkillPage ")){
            final int pageNum = Integer.parseInt(command.substring(14));
            filepath = path + "skills.html";

            activeChar.getPlayerHandler().getSkillPages().setCurrentPage(pageNum);
            activeChar.getPlayerHandler().getSkillPages().fillSkillsPages();

            content = HtmCache.getInstance().getHtm(filepath);


            content = content.replace("%skillPage%", activeChar.getPlayerHandler().getSkillPages().getPage(pageNum));
            content = content.replace("%nextPage%", activeChar.getPlayerHandler().getSkillPages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsFGSelectClass ")){
            final String className = command.substring(18);
            filepath = path + "skills.html";

            switch (className){
                case "archer": activeChar.getPlayerHandler().setClass(new Archer());break;
                case "warrior": activeChar.getPlayerHandler().setClass(new Warrior());break;
                case "mage": activeChar.getPlayerHandler().setClass(new Mage());break;
                case "assassin": activeChar.getPlayerHandler().setClass(new Assassin());break;
            }
            activeChar.getPlayerHandler().switchBuildStage();

            activeChar.getPlayerHandler().getSkillPages().setCurrentPage(0);
            activeChar.getPlayerHandler().getSkillPages().fillSkillsPages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = content.replace("%skillPage%", activeChar.getPlayerHandler().getSkillPages().getPage(0));
            content = content.replace("%nextPage%", activeChar.getPlayerHandler().getSkillPages().fillNextPageButtons());

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


