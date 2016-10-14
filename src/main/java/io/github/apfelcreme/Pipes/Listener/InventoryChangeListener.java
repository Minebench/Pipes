package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.InputOutputLocationManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
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
            SimpleLocation dispenserLocation = new SimpleLocation(dispenserBlock.getX(), dispenserBlock.getY(), dispenserBlock.getZ());

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
                            transferItems(finalPipe, pipeInput);
                        }
                    }

                }, 2L);
            }


        }

    }

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
        SimpleLocation dispenserLocation = new SimpleLocation(dispenserBlock.getX(), dispenserBlock.getY(), dispenserBlock.getZ());

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
                transferItems(pipe, pipeInput);
            }
        }
    }

    /**
     * processes all the items in a dispenser into a connected pipe
     *
     * @param pipe      the pipe
     * @param pipeInput the input the item was injected in
     */
    private void transferItems(Pipe pipe, PipeInput pipeInput) {
        //Store all items that should be moved to a queue
        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : pipeInput.getDispenser().getInventory()) {
            if (itemStack != null) {
                itemQueue.add(itemStack);
            }
        }

        //Check all outputs and distribute the items
        // add the current transfer to all the running detections
        for (Detection detection : Pipes.getInstance().getRunningDetections().values()) {
            detection.addLocation(
                    new SimpleLocation(
                            pipeInput.getDispenser().getWorld().getName(),
                            pipeInput.getDispenser().getX(),
                            pipeInput.getDispenser().getY(),
                            pipeInput.getDispenser().getZ()));
        }

        while (!itemQueue.isEmpty()) {
            ItemStack item = itemQueue.remove();
            boolean itemTransferred = false;
            for (PipeOutput output : pipe.getOutputs()) {

                // look if it can be sorted anywhere
                List<String> sortMaterials = new ArrayList<>();
                for (ItemStack i : output.getDropper().getInventory().getContents()) {
                    if (i != null) {
                        sortMaterials.add(i.getType() + ":" + i.getData().getData());
                    }
                }
                // sort!
                if (sortMaterials.contains(item.getType() + ":" + item.getData().getData())) {
                    if (output.getInventoryHolder().getInventory().firstEmpty() != -1) {
                        output.getInventoryHolder().getInventory().addItem(item);
                        pipeInput.getDispenser().getInventory().remove(item);

                        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                                pipeInput.getDispenser().getInventory(), item, output.getInventoryHolder().getInventory(), true);
                        Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                        itemTransferred = true;
                        break;
                    }
                }
            }
            //item could not be sorted! try to find a chest where no sorting is active
            if (!itemTransferred) {
                for (PipeOutput output : pipe.getOutputs()) {
                    List<String> sortMaterials = new ArrayList<>();
                    for (ItemStack i : output.getDropper().getInventory().getContents()) {
                        if (i != null) {
                            sortMaterials.add(i.getType() + ":" + i.getData().getData());
                        }
                    }
                    if (sortMaterials.isEmpty()) {
                        //no sorting function
                        output.getInventoryHolder().getInventory().addItem(item);
                        pipeInput.getDispenser().getInventory().remove(item);

                        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                                pipeInput.getDispenser().getInventory(), item, output.getInventoryHolder().getInventory(), true);
                        Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                    }
                }
            }
        }

    }
}
