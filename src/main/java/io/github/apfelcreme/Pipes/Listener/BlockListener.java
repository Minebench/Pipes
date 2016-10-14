package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.InputOutputLocationManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

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
public class BlockListener implements Listener {

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DISPENSER || event.getBlock().getType() == Material.DROPPER) {
            if (InputOutputLocationManager.isBlockListed(event.getBlock())) {
                InputOutputLocationManager.removeLocation(event.getBlock());
                ItemStack drop =  event.getBlock().getType() == Material.DISPENSER ?
                        Pipes.getCustomDispenserItem() : Pipes.getCustomDropperItem();
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
            }
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getType() == Material.DISPENSER || event.getItemInHand().getType() == Material.DROPPER) {
            if (event.getItemInHand() != null
                    && event.getItemInHand().getItemMeta() != null
                    && event.getItemInHand().getItemMeta().getLore() != null
                    && event.getItemInHand().getItemMeta().getLore().contains(Pipes.hideString("Pipes", ""))) {
                InputOutputLocationManager.saveLocation(event.getBlock());
                try {
                    Pipe pipe = Pipes.isPipe(event.getBlock());
                    if (pipe != null) {
                        Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("info.pipeBuilt")
                                .replace("{0}", pipe.getString()));
                        pipe.highlight();
                    }
                } catch (ChunkNotLoadedException e) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.chunkNotLoaded"));
                }
            }
        }
    }
}
