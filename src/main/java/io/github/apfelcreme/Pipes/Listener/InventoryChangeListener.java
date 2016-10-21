package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.InputOutputLocationManager;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.Scheduler.ItemMoveScheduler;
import io.github.apfelcreme.Pipes.Scheduler.ScheduledItemTransfer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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
public class InventoryChangeListener implements Listener {

    /**
     * a cache to stop endless pipe checks
     */
    private Map<SimpleLocation, Long> lastChecked = new HashMap<>();

    /**
     * a cache to stop endless pipe checks
     */
    private Map<SimpleLocation, Pipe> pipeCache = new HashMap<>();

    /**
     * the scheduler that transfers items
     */
    private ItemMoveScheduler itemMoveScheduler;

    public InventoryChangeListener() {
        itemMoveScheduler = new ItemMoveScheduler();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onInventoryItemMove(final InventoryMoveItemEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (event.getDestination().getType() == InventoryType.DISPENSER) {
            final Block dispenserBlock = event.getDestination().getLocation().getWorld().getBlockAt(
                    event.getDestination().getLocation().getBlockX(),
                    event.getDestination().getLocation().getBlockY(),
                    event.getDestination().getLocation().getBlockZ());
            if (dispenserBlock.getType() != Material.DISPENSER) {
                return;
            }
            if (!InputOutputLocationManager.isBlockListed(dispenserBlock)) {
                return;
            }
            Pipe pipe;
            final SimpleLocation dispenserLocation = new SimpleLocation(
                    dispenserBlock.getWorld().getName(),
                    dispenserBlock.getX(),
                    dispenserBlock.getY(),
                    dispenserBlock.getZ());

            // cache the pipe
            if (!lastChecked.containsKey(dispenserLocation)
                    || (new Date().getTime() > (lastChecked.get(dispenserLocation) + PipesConfig.getPipeCacheDuration()))) {
                try {
                    pipe = Pipes.isPipe(dispenserBlock);
                    if (pipe != null) {
                        lastChecked.put(dispenserLocation, new Date().getTime());
                        pipeCache.put(dispenserLocation, pipe);
                    }
                } catch (ChunkNotLoadedException e) {
                    pipe = null;
                    event.setCancelled(true);
                }
            } else {
                pipe = pipeCache.get(dispenserLocation);
            }
            if (pipe != null) {
                final Pipe finalPipe = pipe;
                Pipes.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Pipes.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        PipeInput pipeInput = finalPipe.getInput(dispenserBlock);
                        if (pipeInput != null) {
                            itemMoveScheduler.add(new ScheduledItemTransfer(finalPipe, pipeInput, event.getItem()));
                        }
                    }

                }, 2L);
            }


        }

    }

    /**
     * gets fired on every inventory close
     *
     * @param event the event
     */
    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        final Block dispenserBlock = event.getInventory().getLocation().getWorld().getBlockAt(
                event.getInventory().getLocation().getBlockX(),
                event.getInventory().getLocation().getBlockY(),
                event.getInventory().getLocation().getBlockZ());
        if (dispenserBlock.getType() != Material.DISPENSER) {
            return;
        }
        if (!((Dispenser) dispenserBlock.getState()).getInventory().getName().equals("Pipe Input")) {
            return;
        }

        Pipe pipe;
        SimpleLocation dispenserLocation = new SimpleLocation(
                dispenserBlock.getWorld().getName(),
                dispenserBlock.getX(),
                dispenserBlock.getY(),
                dispenserBlock.getZ());

        // cache the pipe
        if (!lastChecked.containsKey(dispenserLocation)
                || (new Date().getTime() > (lastChecked.get(dispenserLocation) + PipesConfig.getPipeCacheDuration()))) {
            try {
                pipe = Pipes.isPipe(dispenserBlock);
                if (pipe != null) {
                    lastChecked.put(dispenserLocation, new Date().getTime());
                    pipeCache.put(dispenserLocation, pipe);
                }
            } catch (ChunkNotLoadedException e) {
                pipe = null;
            }
        } else {
            pipe = pipeCache.get(dispenserLocation);
        }
        if (pipe != null) {
            PipeInput pipeInput = pipe.getInput(dispenserBlock);
            if (pipeInput != null) {
                for (ItemStack itemStack : pipeInput.getDispenser().getInventory().getContents()) {
                    if (itemStack != null) {
                        itemMoveScheduler.add(new ScheduledItemTransfer(pipe, pipeInput, itemStack));
                    }
                }
            }
        }

    }
}
