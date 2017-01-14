package io.github.apfelcreme.Pipes.Listener;

import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.listeners.BlockBreakLogging;
import io.github.apfelcreme.Pipes.Event.PipeBlockBreakEvent;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

    private final Pipes plugin;
    private BlockBreakLogging blockBreakLogging;

    public BlockListener(Pipes plugin) {
        this.plugin = plugin;
        if(plugin.getServer().getPluginManager().isPluginEnabled("LogBlock")) {
            blockBreakLogging = new BlockBreakLogging(LogBlock.getInstance());
        }
    }

    @EventHandler
    private void onItemDispense(BlockDispenseEvent event) {
        if (PipesUtil.getPipesItem(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        PipesItem pipesItem = PipesUtil.getPipesItem(event.getBlock());
        if (pipesItem != null) {
            event.setCancelled(true);
            PipeBlockBreakEvent blockBreakEvent = new PipeBlockBreakEvent(event.getBlock(), event.getPlayer(), pipesItem);
            plugin.getServer().getPluginManager().callEvent(blockBreakEvent);
            if (!blockBreakEvent.isCancelled()) {
                if(blockBreakLogging != null) {
                    blockBreakLogging.onBlockBreak(new BlockBreakEvent(blockBreakEvent.getBlock(), event.getPlayer()));
                }
                event.getBlock().setType(Material.AIR);
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), pipesItem.toItemStack());
            }
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        if (PipesUtil.getPipesItem(event.getBlock()) != null) {
            if (event.getBlock().getType() == PipesItem.CHUNK_LOADER.getMaterial()
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

    @EventHandler
    public void onItemCraft(CraftItemEvent event) {
        if (PipesUtil.getPipesItem(event.getCurrentItem()) == PipesItem.CHUNK_LOADER) {
            if (!event.getWhoClicked().hasPermission("Pipes.placeChunkLoader")) {
                event.setCancelled(true);
                Pipes.sendMessage(event.getWhoClicked(), PipesConfig.getText("error.noPermission"));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL
                || event.getResult() == Event.Result.DENY
                || event.getSlotType() != InventoryType.SlotType.RESULT
                || !event.getCurrentItem().hasItemMeta()
                || !event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }

        if (event.getInventory().getContents().length > 0) {
            ItemStack item = event.getInventory().getContents()[0];
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().equals(event.getCurrentItem().getItemMeta().getDisplayName())) {
                return;
            }
        }

        PipesItem pipesItem = PipesUtil.getPipesItem(event.getCurrentItem());
        if (pipesItem == null) {
            return;
        }

        ItemMeta resultMeta = event.getCurrentItem().getItemMeta();
        if (PipesUtil.getHiddenString(resultMeta.getDisplayName()) != null) {
            return;
        }
        resultMeta.setDisplayName(PipesUtil.hideString(pipesItem.toString(), resultMeta.getDisplayName()));
        event.getCurrentItem().setItemMeta(resultMeta);
    }
}
