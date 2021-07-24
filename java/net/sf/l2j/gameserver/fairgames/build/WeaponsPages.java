package net.sf.l2j.gameserver.fairgames.build;

import net.sf.l2j.gameserver.Item;
import net.sf.l2j.gameserver.datatables.IconTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.fairgames.entities.ClassSkill;
import net.sf.l2j.gameserver.fairgames.entities.FGItem;
import net.sf.l2j.gameserver.model.L2ItemInstance;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class WeaponsPages {

    private List<String> _skillsPages = new ArrayList<>();
    private L2PcInstance _player;
    private int _currentPage = 0;

    public WeaponsPages(L2PcInstance _player) {
        this._player = _player;
    }

    public void fillWeaponsPages(){
        _skillsPages.clear();
        StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;

        for(Integer i : ItemsManager.getWeapons().keySet()){
            FGItem fgItem = ItemsManager.getWeapons().get(i);

            String icon = IconTable.getInstance().getIcon(fgItem.getItemId());
            String desc = fgItem.getDesc();

            L2Item item = ItemTable.getInstance().getTemplate(fgItem.getItemId());

            String name = item.getName();

            String tableColor = "252525";

            if(counter%2==0)
                StringUtil.append(sb, "<table><tr>");

            StringUtil.append(sb, "<td><table cellpadding=0 cellspacing=0 width=370 height=36 bgcolor=" + tableColor + "><tr>" +
                    "<td FIXWIDTH=5></td><td FIXWIDTH=250>" +
                    "<font name=\"hs9\">Stats: " + desc + " </font></td>" +
                    "<td FIXWIDTH=170 align=center valign=top bgcolor=8B0000><br1>" +
                    "<font name=\"hs9\">" + name + " </font><img src=\"" + icon +  " \" width=32 height=32><br1>" +
                    "<button value=\"Select\" action=\"bypass _bbsFGChooseSkill " + i + "\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"32\" />" +
                    "</td><td FIXWIDTH=5></td></tr></table></td>");

            if(counter%2==0)
                StringUtil.append(sb, "<td width=5></td>");

            if(counter%2!=0)
                StringUtil.append(sb, "</tr></table></br>");

            counter++;

            if(counter != 1 && counter%10 == 0){
                _skillsPages.add(sb.toString());
                sb.setLength(0);
            }
        }

        if(counter%2==0)
            StringUtil.append(sb, "<td width=5></td>");
        if(counter%2!=0)
            StringUtil.append(sb, "</tr></table></br>");

        if(counter%10!=0){
            _skillsPages.add(sb.toString());
        }
    }

    public String fillNextPageButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        for(int i=0; i<_skillsPages.size(); i++){
            StringUtil.append(sb, "<td width=40 align=left valign=top bgcolor=8B0000>" +
                    "<button value=\""+ (i+1) + "\" action=\"bypass _bbsFGSkillPage " + i + " \" back=\"l2ui_ct1.button.button_df_small_down\" " +
                    "fore=\"l2ui_ct1.button.button_df_small\" width=\"32\" height=\"32\" /></td>");
        }

        return sb.toString();
    }

    public String getPage(int i){ return _skillsPages.get(i); }

    public int getCurrentPage() { return _currentPage; }

    public void setCurrentPage(int _currentPage) { this._currentPage = _currentPage; }
}
