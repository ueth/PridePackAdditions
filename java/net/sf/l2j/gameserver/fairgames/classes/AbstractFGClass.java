package net.sf.l2j.gameserver.fairgames.classes;

import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFGClass {
    protected static final int MAX_SKILLS = 10;
    protected List<Integer> _skills = new ArrayList<>();
    protected int _skillCounter = 0;

    public void fillSkills(int[] SKILLS){
        for(int id : SKILLS)
            _skills.add(id);
    }

    public void learnSkill(L2PcInstance player, int id){
        if(_skillCounter >= MAX_SKILLS)
            return;

        int counter = 0;
        for(int i : _skills){
            if(i == id) {
                _skillCounter++;
                _skills.remove(counter);
                player.addSkill(SkillTable.getInstance().getInfo(id, SkillTable.getInstance().getMaxLevel(id)));
                break;
            }
            counter++;
        }

    }

    public String displaySkills(){
        //display skills in an html window
        return null;
    }

    public String displayBuffs(){
        return null;
    }

    public List<Integer> getSkills() {
        return _skills;
    }
    public abstract int[] getAllSkills();
}
