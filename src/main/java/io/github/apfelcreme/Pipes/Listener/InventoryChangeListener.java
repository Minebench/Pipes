package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.Manager.ItemMoveScheduler;
import io.github.apfelcreme.Pipes.Pipe.ScheduledItemTransfer;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

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

    private final Pipes plugin;

    public InventoryChangeListener(Pipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onInventoryItemMove(final InventoryMoveItemEvent event) {
        if (!(event instanceof PipeMoveItemEvent) && !handleInventoryAction(event.getDestination(), true)) {
            event.setCancelled(true);
        }
    }

    /**
     * gets fired on every inventory close
     *
     * @param event the event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onInventoryClose(InventoryCloseEvent event) {
        handleInventoryAction(event.getInventory(), false);
    }

    /**
     * Handle an inventory action
     * @param inventory The inventory
     * @param scheduled Whether or not to add the scheduled task delayed by 2 ticks
     * @return <tt>Wether or not something went wrong</tt>
     */
    private boolean handleInventoryAction(Inventory inventory, boolean scheduled) {
        if (!(inventory.getHolder() instanceof BlockState)) {
            return true;
        }
        Block dispenserBlock = ((BlockState) inventory.getHolder()).getBlock();
        if (!PipesItem.PIPE_INPUT.check(dispenserBlock)) {
            return true;
        }
        SimpleLocation dispenserLocation = new SimpleLocation(dispenserBlock.getLocation());

        Pipe pipe = PipeManager.getInstance().getPipe(dispenserLocation);
        if (pipe == null) {
            return false;
        }
        PipeInput pipeInput = pipe.getInput(dispenserLocation);
        if (pipeInput != null) {
            final ScheduledItemTransfer transfer = new ScheduledItemTransfer(dispenserLocation);
            if (scheduled) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ItemMoveScheduler.getInstance().add(transfer);
                    }
                }.runTaskLater(plugin, 2);
            } else {
                ItemMoveScheduler.getInstance().add(transfer);
            }
        }
        return true;
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
            if (event.getSource().getHolder() instanceof BlockState
                    && PipesUtil.getPipesItem(((BlockState) event.getSource().getHolder()).getBlock()) != null) {
                event.setCancelled(true);
            }
        }
    }
}
