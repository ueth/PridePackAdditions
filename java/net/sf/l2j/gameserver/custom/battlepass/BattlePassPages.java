package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class BattlePassPages {
    private List<String> _playerBattlePassPages;
    private L2PcInstance _player;

    public BattlePassPages (L2PcInstance player) {
        _playerBattlePassPages = new ArrayList<>();
        _player = player;
    }

    public String fillPages(String html){
        _playerBattlePassPages.clear();
        final StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;

        for(BattlePass battlePass : BattlePassTable.getBattlePasses().values()){
            if(counter%2==0)
                StringUtil.append(sb, "<table><tr>");

            StringUtil.append(sb, "<td><table width=371 height=36 bgcolor=333333><tr><td width=371 align=center><font name=\"hs12\">" + battlePass.getName() + "</font></td></tr></table>");
            StringUtil.append(sb, "<table cellpadding=0 cellspacing=10 width=371 height=36 bgcolor=252525><tr><td width=360 align=center><font name=\"hs12\">Cost: </font>" +
                    "<font name=\"hs9\">" + battlePass.getPrice() + " Donation Tokens</font></td></tr></table>");
            StringUtil.append(sb, "<table cellpadding=0 cellspacing=10 width=371 height=36 bgcolor=252525><tr><td width=179 align=center>" +
                    "<button value=\"Preview\" action=\"bypass _bbsBattlePassPlayer\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"31\"/>" +
                    "</td><td width=180 align=center><button value=\"Buy\" action=\"bypass _bbsBattlePassPlayer\" back=\"l2ui_ct1.button.button_df_small_down\" " +
                    "fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"31\"/></td></tr></table></td>");

            StringUtil.append(sb, "<td width=2></td><td width=2></td>");

            if(counter%2!=0)
                StringUtil.append(sb, "</tr></table><table height=16></table>");

            counter++;

            if(counter != 1 && counter%6 == 0){
                _playerBattlePassPages.add(sb.toString());
            }
        }

        if(counter%2!=0)
            StringUtil.append(sb, "</tr></table><table height=16></table>");

        if(counter%6!=0){
            _playerBattlePassPages.add(sb.toString());
        }

        StringUtil.append(sb, fillNextPageButtons());

        html = html.replace("%replace%", _playerBattlePassPages.get(0));

        return html;
    }

    public String fillNextPageButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        StringUtil.append(sb, "<table width=780><tr>");

        for(int i=0; i<_playerBattlePassPages.size(); i++){
            StringUtil.append(sb, "<td width=10 align=center><button value=\"" + (i+1) + "\" action=\"bypass _bbsBattlePassPlayer\"" +
                    " back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"31\" height=\"31\"/></td>");
        }

        return sb.toString();
    }

    public void displayBattlePassPage(int pageNumber){

    }
}
