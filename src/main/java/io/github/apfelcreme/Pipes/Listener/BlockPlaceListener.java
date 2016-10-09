package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.material.Dispenser;
import org.bukkit.material.MaterialData;

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
public class BlockPlaceListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!event.getBlockPlaced().getType().equals(Material.DISPENSER)) {
            return;
        }

        Block block = event.getBlockPlaced();
        MaterialData blockData = block.getState().getData();
        Dispenser dispenser = (Dispenser) blockData;
        Block relative = block.getRelative(dispenser.getFacing());
        Pipe pipe = Pipes.isPipe(block, relative.getData());
        if (pipe != null) {
            Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("info.pipeCreated")
                    .replace("{0}", pipe.getString()));
        }

    }

}
