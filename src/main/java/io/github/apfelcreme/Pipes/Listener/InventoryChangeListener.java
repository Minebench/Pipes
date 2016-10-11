package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.LoopException;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    @EventHandler
    private void onInventoryItemMove(final InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.DISPENSER) {
            final Block dispenserBlock = event.getDestination().getLocation().getWorld().getBlockAt(
                    event.getDestination().getLocation().getBlockX(),
                    event.getDestination().getLocation().getBlockY(),
                    event.getDestination().getLocation().getBlockZ());
            if (dispenserBlock.getType() == Material.DISPENSER) {
                Pipes.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(Pipes.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Pipe pipe = Pipes.isPipe(dispenserBlock);
                            if (pipe != null) {
                                PipeInput pipeInput = pipe.getInput(dispenserBlock);
                                if (pipeInput != null) {
                                    transferItems(pipe, pipeInput);
                                }
                            }
                        } catch (LoopException e) {
                            Pipes.getInstance().getLogger().info(PipesConfig.getText("log.loop")
                                    .replace("{0}", e.getRelative().getLocation().toString()));
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
        if (dispenserBlock.getType() == Material.DISPENSER) {
            try {
                Pipe pipe = Pipes.isPipe(dispenserBlock);
                if (pipe != null) {
                    PipeInput pipeInput = pipe.getInput(dispenserBlock);
                    if (pipeInput != null) {
                        transferItems(pipe, pipeInput);
                    }
                }
            } catch (LoopException e) {
                Pipes.getInstance().getLogger().info(PipesConfig.getText("log.loop")
                        .replace("{0}", e.getRelative().getLocation().toString()));
            }
        }
    }

    /**
     * processes all the items in a dispenser into a connected pipe
     *
     * @param pipe a pipe
     * @throws LoopException when a loop was built and the items inside it are carried indefinetly
     */
    private void transferItems(Pipe pipe, PipeInput pipeInput) throws LoopException {

        System.out.println(pipeInput.toString() + " -> " + pipe.toString());

        //Store all items that should be moved to a queue
        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : pipeInput.getDispenser().getInventory()) {
            itemQueue.add(itemStack);
        }

        //Check all outputs and distribute the items
        while (!itemQueue.isEmpty()) {
            ItemStack item = itemQueue.remove();

            if (item != null) {

                for (PipeOutput output : pipe.getOutputs()) {
                    // check for a loop

                    // look if it can be sorted anywhere
                    List<String> sortMaterials = new ArrayList<>();
                    for (ItemStack i : output.getDropper().getInventory().getContents()) {
                        if (i != null) {
                            sortMaterials.add(i.getType() + ":" + i.getData().getData());
                        }
                    }
                    // sort!
                    if (sortMaterials.isEmpty() || sortMaterials.contains(item.getType() + ":" + item.getData().getData())) {
                        if (output.getInventoryHolder().getInventory().firstEmpty() != -1) {
//                                throw new LoopException(output.getDropper(), pipeInput.getDispenser().getBlock());

                            output.getInventoryHolder().getInventory().addItem(item);
                            pipeInput.getDispenser().getInventory().remove(item);

                            InventoryMoveItemEvent event = new InventoryMoveItemEvent(pipeInput.getDispenser().getInventory(), item, output.getInventoryHolder().getInventory(), true);
                            Pipes.getInstance().getServer().getPluginManager().callEvent(event);

                        }
                    }
                }
            }
        }

    }
}
