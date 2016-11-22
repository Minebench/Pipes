package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Beacon;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;

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
        if ((event.getBlock().getType() == Material.DISPENSER)
                && PipeManager.isPipeInput((Dispenser) event.getBlock().getState())) {
            // a pipe input was destroyed
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), PipesUtil.getCustomDispenserItem());
        } else if ((event.getBlock().getType() == Material.DROPPER)
                && PipeManager.isPipeOutput((Dropper) event.getBlock().getState())) {
            // a pipe input was destroyed
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), PipesUtil.getCustomDropperItem());
        } else if ((event.getBlock().getType() == Material.BEACON)
                && PipeManager.isChunkLoader((Beacon) event.getBlock().getState())) {
            // a pipe input was destroyed
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), PipesUtil.getCustomChunkLoaderItem());
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getType() == Material.DISPENSER
                || event.getItemInHand().getType() == Material.DROPPER
                || event.getItemInHand().getType() == Material.BEACON) {
            if (event.getItemInHand() != null
                    && event.getItemInHand().getItemMeta() != null
                    && event.getItemInHand().getItemMeta().getLore() != null
                    && event.getItemInHand().getItemMeta().getLore().contains(
                    ChatColor.BLUE + "" + ChatColor.ITALIC + PipesUtil.hideString("Pipes", "Pipes"))) {
                if ((event.getItemInHand().getType() == Material.BEACON)
                        && !event.getPlayer().hasPermission("Pipes.placeChunkLoader")) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.noPermission"));
                    event.setCancelled(true);
                    return;
                }
                try {
                    Pipe pipe = PipeManager.isPipe(event.getBlock());
                    if (pipe != null) {
                        Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("info.pipe.pipeBuilt")
                                .replace("{0}", pipe.getString()));
                        pipe.highlight();
                    }
                } catch (ChunkNotLoadedException e) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.chunkNotLoaded"));
                }
            }
        }
    }

    @EventHandler
    public void onItemCraft(CraftItemEvent event) {
        if (event.getCurrentItem().equals(PipesUtil.getCustomChunkLoaderItem())) {
            if (!event.getWhoClicked().hasPermission("Pipes.placeChunkLoader")) {
                event.setCancelled(true);
                Pipes.sendMessage(event.getWhoClicked(), PipesConfig.getText("error.noPermission"));
            }
        }
    }
}
