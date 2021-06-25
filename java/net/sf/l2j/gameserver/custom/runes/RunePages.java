package net.sf.l2j.gameserver.custom.runes;

import net.sf.l2j.gameserver.datatables.IconTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class RunePages {
    private List<String> _runePages;
    private L2PcInstance _player;

    public RunePages (L2PcInstance player) {
        _runePages = new ArrayList<>();
        _player = player;
    }

    public String getPage(int pageNum){
        if(_runePages.isEmpty())
            return "";
        return _runePages.get(pageNum-1);
    }

    public void fillRunePages(){
        _runePages.clear();
        StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;

        for(Rune rune : _player.getRunePlayer().getRunes().values()){
            String icon = IconTable.getInstance().getIcon(rune.getId());
            String name = rune.getName();

            if(counter%5==0)
                StringUtil.append(sb, "<tr>");

            counter++;

            StringUtil.append(sb, "<td width=130 align=center valign=top bgcolor=8B0000><img src=\"" + icon + "\" width=32 height=32>" +
                    "<button value=\" " + name + "\" action=\"bypass _bbsRunesAdd " + rune.getId() + "\" back=\"l2ui_ct1.button.button_df_small_down\" " +
                    "fore=\"l2ui_ct1.button.button_df_small\" width=\"110\" height=\"32\" /></td>");

            if(counter%5==0)
                StringUtil.append(sb, "</tr>");

            if(counter != 1 && counter%5 == 0){
                _runePages.add(sb.toString());
                sb.setLength(0);
            }
        }

        if(counter%5!=0)
            StringUtil.append(sb, "</tr>");

        if(counter%5!=0){
            _runePages.add(sb.toString());
        }
    }

    public String fillNextPageButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        StringUtil.append(sb, "<tr>");

        for(int i=0; i<_runePages.size(); i++){
            StringUtil.append(sb, "<<td width=40 align=left valign=top bgcolor=8B0000><button value=\"" + (i+1) + "\" action=\"bypass _bbsRunesPage " + (i+1) + "\" " +
                    "back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"32\" height=\"32\" /></td>");
        }

        StringUtil.append(sb, "</tr>");

        return sb.toString();
    }
}
