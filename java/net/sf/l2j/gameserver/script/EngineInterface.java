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
package net.sf.l2j.gameserver.script;

import net.sf.l2j.gameserver.Announcements;
import net.sf.l2j.gameserver.GameTimeController;
import net.sf.l2j.gameserver.RecipeController;
import net.sf.l2j.gameserver.datatables.CharNameTable;
import net.sf.l2j.gameserver.datatables.CharTemplateTable;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.LevelUpData;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.datatables.SkillTreeTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.datatables.TeleportLocationTable;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.L2World;

/**
 * @author Luis Arias
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface EngineInterface
{
    //*  keep the references of Singletons to prevent garbage collection
    CharNameTable charNametable = CharNameTable.getInstance();

    IdFactory idFactory = IdFactory.getInstance();
    ItemTable itemTable = ItemTable.getInstance();

    SkillTable skillTable = SkillTable.getInstance();

    RecipeController recipeController = RecipeController.getInstance();

    SkillTreeTable skillTreeTable = SkillTreeTable.getInstance();
    CharTemplateTable charTemplates = CharTemplateTable.getInstance();
    ClanTable clanTable = ClanTable.getInstance();

    NpcTable npcTable = NpcTable.getInstance();

    TeleportLocationTable teleTable = TeleportLocationTable.getInstance();
    LevelUpData levelUpData = LevelUpData.getInstance();
    L2World world = L2World.getInstance();
    SpawnTable spawnTable = SpawnTable.getInstance();
    GameTimeController gameTimeController = GameTimeController.getInstance();
    Announcements announcements = Announcements.getInstance();
    MapRegionTable mapRegions = MapRegionTable.getInstance();



    //public ArrayList getAllPlayers();
    //public Player getPlayer(String characterName);
    void addQuestDrop(int npcID, int itemID, int min, int max, int chance, String questID, String[] states);
    void addEventDrop(int[] items, int[] count, double chance, DateRange range);
    void onPlayerLogin(String[] message, DateRange range);

}
