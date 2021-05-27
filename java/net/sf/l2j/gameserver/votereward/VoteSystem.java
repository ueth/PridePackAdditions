/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.votereward;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.Broadcast;
//import net.sf.l2j.util.Rnd;
/**
 * @author Anarchy
 *
 */
public abstract class VoteSystem implements Runnable
{
    private static List<VoteSystem> voteSystems = new ArrayList<>();
      
    protected int votesDiff;
    protected boolean allowReport;
    protected int boxes;
    protected Map<Integer, Integer> rewards;
    protected int checkMins;
    protected int lastVotes = 0;
    private Map<String, Integer> playerIps = new HashMap<>();
      
    public static void initialize()
    {
        System.out.println("Vote System: Loaded");
        if (Config.ALLOW_NETWORK_VOTE_REWARD)
            voteSystems.add(new Network(Config.NETWORK_VOTES_DIFFERENCE, Config.ALLOW_NETWORK_GAME_SERVER_REPORT, Config.NETWORK_DUALBOXES_ALLOWED, Config.NETWORK_REWARD, Config.NETWORK_REWARD_CHECK_TIME));
        if (Config.ALLOW_TOPZONE_VOTE_REWARD)
            voteSystems.add(new Topzone(Config.TOPZONE_VOTES_DIFFERENCE, Config.ALLOW_TOPZONE_GAME_SERVER_REPORT, Config.TOPZONE_DUALBOXES_ALLOWED, Config.TOPZONE_REWARD, Config.TOPZONE_REWARD_CHECK_TIME));
        if (Config.ALLOW_HOPZONE_VOTE_REWARD)
            voteSystems.add(new Hopzone(Config.HOPZONE_VOTES_DIFFERENCE, Config.ALLOW_HOPZONE_GAME_SERVER_REPORT, Config.HOPZONE_DUALBOXES_ALLOWED, Config.HOPZONE_REWARD, Config.HOPZONE_REWARD_CHECK_TIME));
    }
      
    public static VoteSystem getVoteSystem(String name)
    {
        for (VoteSystem vs : voteSystems)
            if (vs.getSiteName().equals(name))
                return vs;
              
        return null;
    }
      
    public VoteSystem(int votesDiff, boolean allowReport, int boxes, Map<Integer, Integer> rewards, int checkMins)
    {
        this.votesDiff = votesDiff;
        this.allowReport = allowReport;
        this.boxes = boxes;
        this.rewards = rewards;
        this.checkMins = checkMins;
              
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, checkMins*1000*60, checkMins*1000*60);
    }
      
    protected void reward()
    {
        int currentVotes = getVotes();
              
        if (currentVotes == -1)
        {
            System.out.println("There was a problem on getting server votes.");
            return;
        }
              
        if (lastVotes == 0)
        {
            lastVotes = currentVotes;
            announce(getSiteName()+": Current vote count is "+currentVotes+".");
            announce(getSiteName()+": "+((lastVotes+votesDiff)-currentVotes)+" vote(s) needed for reward.");
            if (allowReport)
            {
                System.out.println("Server votes on "+getSiteName()+": "+currentVotes);
                System.out.println("Votes needed for reward: "+((lastVotes+votesDiff)-currentVotes));
            }
            return;
        }
              
        if (currentVotes >= lastVotes+votesDiff)
        {
            Collection<L2PcInstance> pls = L2World.getInstance().getAllPlayers().values();
            if (allowReport)
            {
                System.out.println("Server votes on "+getSiteName()+": "+currentVotes);
                System.out.println("Votes needed for next reward: "+((currentVotes+votesDiff)-currentVotes));
            }
            announce(getSiteName()+": Everyone has been rewarded.");
            announce(getSiteName()+": Current vote count is "+currentVotes+".");
            announce(getSiteName()+": "+votesDiff+" vote(s) needed for next reward.");
            for (L2PcInstance p : pls)
            {
                if (p.getClient() == null || p.getClient().isDetached()) // offline shops protection
                    continue;
                              
                boolean canReward = false;
                String pIp = p.getClient().getConnection().getInetAddress().getHostAddress();
                if (playerIps.containsKey(pIp))
                {
                    int count = playerIps.get(pIp);
                    if (count < boxes)
                    {
                        playerIps.remove(pIp);
                        playerIps.put(pIp, count+1);
                        canReward = true;
                    }
                }
                else
                {
                    canReward = true;
                    playerIps.put(pIp, 1);
                }
                if (canReward)
                {               	
                	for (int i : rewards.keySet())                       
                	{
                        p.addItem("Vote reward.", i, rewards.get(i), p, true);                                               	                       	
                    }              	
                }
                else
                    p.sendMessage("Already "+boxes+" character(s) of your ip have been rewarded, so this character won't be rewarded.");
            }
            playerIps.clear();
                      
            lastVotes = currentVotes;
        }
        else
        {
            if (allowReport)
            {
                System.out.println("Server votes on "+getSiteName()+": "+currentVotes);
                System.out.println("Votes needed for next reward: "+((lastVotes+votesDiff)-currentVotes));
            }
            announce(getSiteName()+": Current vote count is "+currentVotes+".");
            announce(getSiteName()+": "+((lastVotes+votesDiff)-currentVotes)+" vote(s) needed for reward.");
        }
    }
      
    private static void announce(String msg)
    {
        CreatureSay cs = new CreatureSay(0, 18, "", msg);
        Broadcast.toAllOnlinePlayers(cs);
    }
      
    public abstract int getVotes();
    public abstract String getSiteName();
}