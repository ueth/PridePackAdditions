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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
//import java.net.URLConnection;
import java.util.Map;

import net.sf.l2j.Config;

/**
 * @author Anarchy
 *
 */
public class Topzone extends VoteSystem
{
    public Topzone(int votesDiff, boolean allowReport, int boxes, Map<Integer, Integer> rewards, int checkMins)
    {
        super(votesDiff, allowReport, boxes, rewards, checkMins);
    }
      
    @Override
    public void run()
    {
        reward();
    }

    @Override
    public int getVotes()
	{
		int votes = -1;
		try
		{
			final URL obj = new URL(Config.TOPZONE_SERVER_LINK);
			final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			con.addRequestProperty("User-Agent", "L2TopZone");
			con.setConnectTimeout(5000);
			
			final int responseCode = con.getResponseCode();
			if (responseCode == 200)
			{
				try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream())))
				{
					String inputLine;
					while ((inputLine = in.readLine()) != null)
					{
						votes = Integer.valueOf(inputLine);
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("TOPZONE is offline. We will check reward as it will be online again.");
		}
		
		return votes;
	}

    @Override
    public String getSiteName()
    {
        return "Topzone";
    }
}