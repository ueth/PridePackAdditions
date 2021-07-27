package net.sf.l2j.gameserver.fairgames.build;

import net.sf.l2j.gameserver.Item;
import net.sf.l2j.gameserver.datatables.IconTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.fairgames.entities.FGItem;
import net.sf.l2j.gameserver.fairgames.enums.BuildStage;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemsPages {

    private List<String> _itemsPages = new ArrayList<>();
    private int _currentPage = 0;
    private L2PcInstance _player;

    public ItemsPages(L2PcInstance player) {_player = player; }

    public void fillItemsPages(){
        _itemsPages.clear();
        StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;

        Map<Integer, FGItem> fgMap = new HashMap<>();
        fgMap.putAll(getCurrentMap());

        /*For armor and jewels we divide their map size by 5(number of parts)*/
        if(_player.getPlayerHandler().getBuildStage() == BuildStage.ARMOR_CHOOSE){
            fgMap.clear();

            int length = getCurrentMap().size()/5;
            int startingPoint = (_player.getPlayerHandler().getArmorCounter() * length)+1;
            int endPoint = startingPoint + length;

            for(int i = startingPoint; i<endPoint; i++)
                fgMap.put(i,getCurrentMap().get(i));
        }

        if(_player.getPlayerHandler().getBuildStage() == BuildStage.JEWELS_CHOOSE){
            fgMap.clear();

            int length = getCurrentMap().size()/5;
            int startingPoint = (_player.getPlayerHandler().getJewelCounter() * length)+1;
            int endPoint = startingPoint + length;

            for(int i = startingPoint; i<endPoint; i++)
                fgMap.put(i,getCurrentMap().get(i));
        }

        for(Integer i : fgMap.keySet()){
            FGItem fgItem = fgMap.get(i);

            L2Item item = ItemTable.getInstance().getTemplate(fgItem.getItemId());

            String icon = IconTable.getInstance().getIcon(fgItem.getItemId());
            String desc = fgItem.getDesc();
            String name = item.getName();

            String tableColor = "252525";

            if(counter%2==0)
                StringUtil.append(sb, "<table><tr>");

            StringUtil.append(sb, "<td><table cellpadding=0 cellspacing=0 width=370 height=36 bgcolor=" + tableColor + "><tr>" +
                    "<td FIXWIDTH=5></td><td FIXWIDTH=250>" +
                    "<font name=\"hs9\">Stats: " + desc + " </font></td>" +
                    "<td FIXWIDTH=170 align=center valign=top bgcolor=8B0000><br1>" +
                    "<font name=\"hs9\">" + name + " </font><img src=\"" + icon +  " \" width=32 height=32><br1>" +
                    "<button value=\"Select\" action=\"bypass _bbsFGSelectItem " + i + "\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"32\" />" +
                    "</td><td FIXWIDTH=5></td></tr></table></td>");

            if(counter%2==0)
                StringUtil.append(sb, "<td width=5></td>");

            if(counter%2!=0)
                StringUtil.append(sb, "</tr></table></br>");

            counter++;

            if(counter != 1 && counter%10 == 0){
                _itemsPages.add(sb.toString());
                sb.setLength(0);
            }
        }

        if(counter%2==0)
            StringUtil.append(sb, "<td width=5></td>");
        if(counter%2!=0)
            StringUtil.append(sb, "</tr></table></br>");

        if(counter%10!=0){
            _itemsPages.add(sb.toString());
        }
    }

    public String fillNextPageButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        for(int i = 0; i< _itemsPages.size(); i++){
            StringUtil.append(sb, "<td width=40 align=left valign=top bgcolor=8B0000>" +
                    "<button value=\""+ (i+1) + "\" action=\"bypass _bbsFGPage " + i + " \" back=\"l2ui_ct1.button.button_df_small_down\" " +
                    "fore=\"l2ui_ct1.button.button_df_small\" width=\"32\" height=\"32\" /></td>");
        }

        return sb.toString();
    }

    private Map<Integer,FGItem> getCurrentMap(){
        switch (_player.getPlayerHandler().getBuildStage()){
            case WEAPON_CHOOSE:
                return ItemsManager.getWeapons();

            case ARMOR_CHOOSE:
                return ItemsManager.getArmors();

            case JEWELS_CHOOSE:
                return ItemsManager.getJewels();

            case TATTOO_CHOOSE:
                return ItemsManager.getTattoos();

            default:
                return null;
        }
    }

    public String getPage(int i){ return _itemsPages.get(i); }

    public int getCurrentPage() { return _currentPage; }

    public void setCurrentPage(int _currentPage) { this._currentPage = _currentPage; }
}
