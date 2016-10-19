package io.github.apfelcreme.Pipes;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (C) 2016 Lord36 aka Apfelcreme
 * <p>
 * This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 *
 * @author Lord36 aka Apfelcreme
 */
public class InputOutputLocationManager {

    /**
     * THIS IS CURRENTLY THE EASIEST (AND POSSIBLY THE ONLY) WAY TO PERSISTENTLY STORE DATA FOR BLOCKS
     * 1.11 MAY BRING SOME UPDATES ON THAT;
     **/

    /**
     * saves a block location to the plugin config
     *
     * @param block a block
     */
    public static void saveLocation(Block block) {
        List<String> locations = PipesConfig.getLocationConfig().getStringList(block.getType().name());
        String location = block.getWorld().getName() + "/" + block.getX() + "/" + block.getY() + "/" + block.getZ();
        if (locations == null) {
            locations = new ArrayList<>();
        }
        if (!locations.contains(location)) {
            locations.add(location);
        }
        PipesConfig.getLocationConfig().set(block.getType().name(), locations);
        PipesConfig.saveLocationConfig();
    }

    /**
     * removes a block location from the plugin config
     *
     * @param block a block
     */
    public static void removeLocation(Block block) {
        List<String> locations = PipesConfig.getLocationConfig().getStringList(block.getType().name());
        String location = block.getWorld().getName() + "/" + block.getX() + "/" + block.getY() + "/" + block.getZ();
        if (locations != null) {
            locations.remove(location);
            PipesConfig.getLocationConfig().set(block.getType().name(), locations);
            PipesConfig.saveLocationConfig();
        }
    }

    /**
     * checks whether a block is part of the saved locations
     *
     * @param block a block
     */
    public static boolean isBlockListed(Block block) {
        List<String> locations = PipesConfig.getLocationConfig().getStringList(block.getType().name());
        String location = block.getWorld().getName() + "/" + block.getX() + "/" + block.getY() + "/" + block.getZ();
        return locations != null && locations.contains(location);
    }
}
