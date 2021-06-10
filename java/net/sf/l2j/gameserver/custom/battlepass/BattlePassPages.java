package net.sf.l2j.gameserver.custom.battlepass;

import net.sf.l2j.gameserver.datatables.IconTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.templates.item.L2Item;
import net.sf.l2j.gameserver.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class BattlePassPages {
    private List<String> _playerBattlePassPages;
    private List<String> _playerBattlePassRewardPages;
    private L2PcInstance _player;

    public BattlePassPages (L2PcInstance player) {
        _playerBattlePassPages = new ArrayList<>();
        _playerBattlePassRewardPages = new ArrayList<>();
        _player = player;
    }

    public void fillPages(){
        _playerBattlePassPages.clear();
        StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;
        String owned = "(Owned)";


        for(BattlePass battlePass : BattlePassTable.getBattlePasses().values()){
            for(BattlePass bp : _player.getBattlePass().getBattlePasses()){
                if(bp.getId() == battlePass.getId()){
                    owned = "(Owned)";
                    break;
                }else{
                    owned = "";
                }
            }
            if(counter%2==0)
                StringUtil.append(sb, "<table><tr>");

            StringUtil.append(sb, "<td><table width=371 height=36 bgcolor=333333><tr><td width=371 align=center><font name=\"hs12\">" + battlePass.getName() + " " + owned + "</font></td></tr></table>");
            StringUtil.append(sb, "<table cellpadding=0 cellspacing=10 width=371 height=36 bgcolor=252525><tr><td width=360 align=center><font name=\"hs12\">Cost: </font>" +
                    "<font name=\"hs9\">" + battlePass.getPrice() + " Donation Tokens</font></td></tr></table>");
            StringUtil.append(sb, "<table cellpadding=0 cellspacing=10 width=371 height=36 bgcolor=252525><tr><td width=179 align=center>" +
                    "<button value=\"Preview\" action=\"bypass _bbsBattlePassPreview " + battlePass.getId() + "\" back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"31\"/>" +
                    "</td><td width=180 align=center><button value=\"Buy\" action=\"bypass _bbsBattlePassBuyPlayer " + battlePass.getId() + "\" back=\"l2ui_ct1.button.button_df_small_down\" " +
                    "fore=\"l2ui_ct1.button.button_df_small\" width=\"86\" height=\"31\"/></td></tr></table></td>");

            StringUtil.append(sb, "<td width=2></td><td width=2></td>");

            if(counter%2!=0)
                StringUtil.append(sb, "</tr></table><table height=16></table>");

            counter++;

            if(counter != 1 && counter%6 == 0){
                _playerBattlePassPages.add(sb.toString());
                sb.setLength(0);
            }
        }

        if(counter%2!=0)
            StringUtil.append(sb, "</tr></table><table height=16></table>");

        if(counter%6!=0){
            _playerBattlePassPages.add(sb.toString());
        }
    }

    public String fillNextPageButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        StringUtil.append(sb, "<table><tr>");

        for(int i=0; i<_playerBattlePassPages.size(); i++){
            StringUtil.append(sb, "<td width=10 align=center><button value=\"" + (i+1) + "\" action=\"bypass _bbsBattlePassPlayer " + i + "\"" +
                    " back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"31\" height=\"31\"/></td>");
        }

        StringUtil.append(sb, "</tr></table>");

        return sb.toString();
    }

    public void fillRewards(int battlePassId){
        _playerBattlePassRewardPages.clear();
        BattlePass thisBattlePass = null;
        int greenBar = 0;
        int redBar = 0;

        StringBuilder sb = StringUtil.startAppend(1000, "");
        int counter = 0;

        /**
         * finding the battlepass that we want its rewards to be displayed
         */
        for(BattlePass battlePass : _player.getBattlePass().getBattlePasses()){
            if(battlePassId == battlePass.getId()){
                thisBattlePass = battlePass;
                break;
            }
        }

        if(thisBattlePass.getRewards().isEmpty())
            return;

        for(Reward reward : thisBattlePass.getRewards().values()){
            L2Item item = ItemTable.getInstance().getTemplate(reward.getItemId());
            String icon = "icon.etc_charm_of_courage_i03";
            String colorBar = "l2ui_ct1.Gauge_DF_Food_Center";

            greenBar = Math.min(Math.max((int)(thisBattlePass.getPoints()*100) - 100*counter, 0) ,100);
            redBar = 100 - greenBar;

            if(greenBar == 100)
                colorBar = "l2ui_ct1.Gauge_DF_Large_MP_Center";

            if(IconTable.getInstance().getIcon(item.getItemId()) != "icon.NOIMAGE")
                icon = IconTable.getInstance().getIcon(item.getItemId());

            if(counter%2==0)
                StringUtil.append(sb, "<table width=700 cellpadding=0 cellspacing=0><tr><td width=30></td>");

            StringUtil.append(sb, "<td align=center><table><tr><td><table height=45 border=0 cellspacing=0 cellpadding=0 bgcolor=404040><tr><td FIXWIDTH=10></td>" +
                    "<td height=36 FIXWIDTH=10 align=right valign=center bgcolor=8B0000><img src=\"" + icon + "\" width=32 height=32></td>" +
                    "<td FIXWIDTH=5></td><td FIXWIDTH=200 align=left valign=center><font color=\"B59A75\" name=\"hs12\">" + item.getName() + "</font> &nbsp;<br1>" +
                    "<font color=\"B5B5B5\" name=\"CreditTextSmall\">Amount: " + reward.getAmount() + "</font></td><td width=" + greenBar + " valign=center align=right>" +
                    "<img src=\"" + colorBar + "\" width=" + greenBar + " height=8/></td><td width=" + redBar + " valign=center align=left>" +
                    "<img src=\"l2ui_ct1.Gauge_DF_Hp_bg_Center\" width=" + redBar + " height=8/></td><td FIXWIDTH=10></td></tr></table></td></tr></table></td>");

            if(counter%2==0)
                StringUtil.append(sb, "<td width=12></td>");


            if(counter%2!=0)
                StringUtil.append(sb, "</tr></table>&nbsp;<br1>");

            counter++;

            if(counter != 1 && counter%16 == 0){
                _playerBattlePassRewardPages.add(sb.toString());
                sb.setLength(0);
            }
        }

        if(counter%2!=0)
            StringUtil.append(sb, "</tr></table>&nbsp;<br1>");

        if(counter%16!=0){
            _playerBattlePassRewardPages.add(sb.toString());
        }
    }

    public String fillNextPageRewardButtons(){
        final StringBuilder sb = StringUtil.startAppend(1000, "");

        StringUtil.append(sb, "<table><tr>");

        for(int i=0; i<_playerBattlePassRewardPages.size(); i++){
            StringUtil.append(sb, "<td width=10 align=center><button value=\"" + (i+1) + "\" action=\"bypass _bbsBattlePassPreviewNextPage " + i + "\"" +
                    " back=\"l2ui_ct1.button.button_df_small_down\" fore=\"l2ui_ct1.button.button_df_small\" width=\"31\" height=\"31\"/></td>");
        }

        StringUtil.append(sb, "</tr></table>");

        return sb.toString();
    }

    public String getPage(int i){
        return _playerBattlePassPages.get(i);
    }
    public String getRewardPage(int i){return  _playerBattlePassRewardPages.get(i);}

    public void displayBattlePassPage(int pageNumber){

    }
}
