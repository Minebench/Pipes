package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.Manager.ItemMoveScheduler;
import io.github.apfelcreme.Pipes.Pipe.ScheduledItemTransfer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;

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
            if (!PipeManager.isPipeInput((Dispenser) dispenserBlock.getState())) {
                return;
            }
            final SimpleLocation dispenserLocation = new SimpleLocation(
                    dispenserBlock.getWorld().getName(),
                    dispenserBlock.getX(),
                    dispenserBlock.getY(),
                    dispenserBlock.getZ());

            // cache the pipe
            Pipe pipe = PipeManager.getInstance().getPipeCache().getIfPresent(dispenserLocation);
            if (pipe == null) {
                try {
                    pipe = PipeManager.isPipe(dispenserBlock);
                    PipeManager.getInstance().addPipeToCache(dispenserLocation, pipe);
                } catch (ChunkNotLoadedException e) {
                    pipe = null;
                    event.setCancelled(true);
                }
            }
            if (pipe != null && !event.isCancelled()) {
                final Pipe finalPipe = pipe;
                Pipes.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Pipes.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        PipeInput pipeInput = finalPipe.getInput(dispenserBlock);
                        if (pipeInput != null) {
                            ItemMoveScheduler.getInstance().add(new ScheduledItemTransfer(finalPipe, pipeInput));
                        }
                    }
//
                }, 2L);
            }
        }
    }

    /**
     * gets fired on every inventory close
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onInventoryClose(InventoryCloseEvent event) {
        final Block dispenserBlock = event.getInventory().getLocation().getWorld().getBlockAt(
                event.getInventory().getLocation().getBlockX(),
                event.getInventory().getLocation().getBlockY(),
                event.getInventory().getLocation().getBlockZ());
        if (dispenserBlock.getType() != Material.DISPENSER) {
            return;
        }
        if (!PipeManager.isPipeInput((Dispenser) dispenserBlock.getState())) {
            return;
        }

        SimpleLocation dispenserLocation = new SimpleLocation(
                dispenserBlock.getWorld().getName(),
                dispenserBlock.getX(),
                dispenserBlock.getY(),
                dispenserBlock.getZ());


        // cache the pipe
        Pipe pipe = PipeManager.getInstance().getPipeCache().getIfPresent(dispenserLocation);
        if (pipe == null) {
            try {
                pipe = PipeManager.isPipe(dispenserBlock);
                PipeManager.getInstance().addPipeToCache(dispenserLocation, pipe);
            } catch (ChunkNotLoadedException e) {
                pipe = null;
            }
        }
        if (pipe != null) {
            PipeInput pipeInput = pipe.getInput(dispenserBlock);
            if (pipeInput != null) {
                ItemMoveScheduler.getInstance().add(new ScheduledItemTransfer(pipe, pipeInput));
            }
        }
    }

    /**
     * cancels all events where hoppers are trying to move items out of a pipe output or a pipe input
     *
     * @param event the event
     */
    @EventHandler
    private void onHopperTransfer(InventoryMoveItemEvent event) {
        if (event.getInitiator().getType() == InventoryType.HOPPER &&
                (event.getSource().getType() == InventoryType.DISPENSER
                        || event.getSource().getType() == InventoryType.DROPPER)) {
            SimpleLocation location = new SimpleLocation(
                    event.getSource().getLocation().getWorld().getName(),
                    event.getSource().getLocation().getBlockX(),
                    event.getSource().getLocation().getBlockY(),
                    event.getSource().getLocation().getBlockZ());
            if (PipeManager.isPipeInput(location) || PipeManager.isPipeOutput(location)) {
                event.setCancelled(true);
            }
        }
    }
}
