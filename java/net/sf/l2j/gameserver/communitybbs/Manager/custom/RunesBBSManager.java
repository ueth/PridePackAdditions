package net.sf.l2j.gameserver.communitybbs.Manager.custom;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.custom.runes.Rune;
import net.sf.l2j.gameserver.datatables.IconTable;
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

            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = placeActiveRunes(content, activeChar);
            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(1));
            content = content.replace("%nextPage%", activeChar.getRunePlayer().getRunePages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsRunesPage ")){
            final int pageNum = Integer.parseInt(command.substring(14));
            filepath = path + "runes.html";

            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = placeActiveRunes(content, activeChar);
            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(pageNum));
            content = content.replace("%nextPage%", activeChar.getRunePlayer().getRunePages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsRunesRemove ")){
            final int runeNum = Integer.parseInt(command.substring(16));
            filepath = path + "runes.html";

            if(runeNum!=4){
                activeChar.getRunePlayer().removeRune(activeChar.getRunePlayer().getActiveRunes().get(runeNum-1));
            }else{
                activeChar.getRunePlayer().removeRune(activeChar.getRunePlayer().getForbiddenRune());
            }


            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = placeActiveRunes(content, activeChar);
            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(1));
            content = content.replace("%nextPage%", activeChar.getRunePlayer().getRunePages().fillNextPageButtons());

            this.separateAndSend(content, activeChar);
        }
        else if(command.startsWith("_bbsRunesAdd ")){
            final int runeId = Integer.parseInt(command.substring(13));
            filepath = path + "runes.html";

            Rune rune = activeChar.getRunePlayer().getRune(runeId);

            activeChar.getRunePlayer().wearRune(rune);

            activeChar.getRunePlayer().getRunePages().fillRunePages();

            content = HtmCache.getInstance().getHtm(filepath);

            content = placeActiveRunes(content, activeChar);
            content = content.replace("%runePage%", activeChar.getRunePlayer().getRunePages().getPage(1));
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

    private String placeActiveRunes(String content, L2PcInstance player){
        for(int i=0; i<4; i++){
            String icon = "icon.etc_charm_of_courage_i03";
            int level = 0;
            String stats = "";
            int expWidth = 0;
            int expNeedWidth = 100;
            String name = "Rune Slot ";

            if(player.getRunePlayer().getActiveRunes().size()>i && !player.getRunePlayer().getActiveRunes().isEmpty()) {
                Rune rune = player.getRunePlayer().getActiveRunes().get(i);
                Rune forbiddenRune = player.getRunePlayer().getForbiddenRune();
                if (rune == null) {
                    content = content.replace("%name" + (i+1) + "%", name + (i+1));
                    content = content.replace("%stats" + (i+1) + "%", stats);
                    content = content.replace("%level" + (i+1) + "%", level + "");
                    content = content.replace("%icon" + (i+1) + "%", icon);
                    content = content.replace("%expwidth" + (i+1) + "%", expWidth + "");
                    content = content.replace("%expneedwidth" + (i+1) + "%", expNeedWidth + "");
                } else {
                    icon = IconTable.getInstance().getIcon(rune.getId());
                    content = content.replace("%name" + (i+1) + "%", rune.getName());
                    content = content.replace("%stats" + (i+1) + "%", rune.getDescription());
                    content = content.replace("%level" + (i+1) + "%", rune.getLevel() + "");
                    content = content.replace("%icon" + (i+1) + "%", icon);

                    expNeedWidth = (int) (((rune.getLevel() * 100 - rune.getExp()) / (rune.getLevel() * 100)) * 100);
                    expWidth = 100 - expNeedWidth;

                    content = content.replace("%expwidth" + (i+1) + "%", expWidth + "");
                    content = content.replace("%expneedwidth" + (i+1) + "%", expNeedWidth + "");
                }
                if (i == 3 && forbiddenRune != null) {
                    icon = IconTable.getInstance().getIcon(forbiddenRune.getId());
                    content = content.replace("%name" + (i+1) + "%", forbiddenRune.getName());
                    content = content.replace("%stats" + (i+1) + "%", forbiddenRune.getDescription());
                    content = content.replace("%level" + (i+1) + "%", forbiddenRune.getLevel() + "");
                    content = content.replace("%icon" + (i+1) + "%", icon);

                    expNeedWidth = (int) (((forbiddenRune.getLevel() * 100 - forbiddenRune.getExp()) / (forbiddenRune.getLevel() * 100)) * 100);
                    expWidth = 100 - expNeedWidth;

                    content = content.replace("%expwidth" + (i+1) + "%", expWidth + "");
                    content = content.replace("%expneedwidth" + (i+1) + "%", expNeedWidth + "");
                }
            }else{
                Rune forbiddenRune = player.getRunePlayer().getForbiddenRune();
                if (i == 3 && forbiddenRune != null) {
                    icon = IconTable.getInstance().getIcon(forbiddenRune.getId());
                    content = content.replace("%name" + (i+1) + "%", forbiddenRune.getName());
                    content = content.replace("%stats" + (i+1) + "%", forbiddenRune.getDescription());
                    content = content.replace("%level" + (i+1) + "%", forbiddenRune.getLevel() + "");
                    content = content.replace("%icon" + (i+1) + "%", icon);

                    expNeedWidth = (int) (((forbiddenRune.getLevel() * 100 - forbiddenRune.getExp()) / (forbiddenRune.getLevel() * 100)) * 100);
                    expWidth = 100 - expNeedWidth;

                    content = content.replace("%expwidth" + (i+1) + "%", expWidth + "");
                    content = content.replace("%expneedwidth" + (i+1) + "%", expNeedWidth + "");
                }
                else{
                    content = content.replace("%name" + (i+1) + "%", name + (i+1));
                    content = content.replace("%stats" +(i+1) + "%", stats);
                    content = content.replace("%level" + (i+1) + "%", level + "");
                    content = content.replace("%icon" + (i+1) + "%", icon);
                    content = content.replace("%expwidth" + (i+1) + "%", expWidth + "");
                    content = content.replace("%expneedwidth" + (i+1) + "%", expNeedWidth + "");
                }
            }
        }

        return content;
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


