package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dropper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dispenser;

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

    @EventHandler
    public void onInventoryItemMove(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() == InventoryType.DISPENSER) {
            final Block dispenserBlock = event.getDestination().getLocation().getWorld().getBlockAt(
                    event.getDestination().getLocation().getBlockX(),
                    event.getDestination().getLocation().getBlockY(),
                    event.getDestination().getLocation().getBlockZ());
            if (dispenserBlock.getType() == Material.DISPENSER) {
                final Dispenser dispenser = (Dispenser) dispenserBlock.getState().getData();
                final Block relative = dispenserBlock.getRelative(dispenser.getFacing());
                Pipes.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(Pipes.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        Pipe pipe = Pipes.isPipe(dispenserBlock, relative.getData());
                        if (pipe != null) {
                            transferItems((org.bukkit.block.Dispenser) dispenserBlock.getState(), pipe);
                        }
                    }
                }, 2L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        final Block dispenserBlock = event.getInventory().getLocation().getWorld().getBlockAt(
                event.getInventory().getLocation().getBlockX(),
                event.getInventory().getLocation().getBlockY(),
                event.getInventory().getLocation().getBlockZ());
        if (dispenserBlock.getType() == Material.DISPENSER) {
            final Dispenser dispenser = (Dispenser) dispenserBlock.getState().getData();
            final Block relative = dispenserBlock.getRelative(dispenser.getFacing());
            Pipe pipe = Pipes.isPipe(dispenserBlock, relative.getData());
            if (pipe != null) {
                transferItems((org.bukkit.block.Dispenser) dispenserBlock.getState(), pipe);
            }
        }
    }

    /**
     * processes all the items in a dispenser into a connected pipe
     *
     * @param dispenser a dispenser item
     * @param pipe      a pipe
     */
    private void transferItems(org.bukkit.block.Dispenser dispenser, Pipe pipe) {
        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : dispenser.getInventory()) {
            itemQueue.add(itemStack);
        }

        while (!itemQueue.isEmpty()) {
            ItemStack item = itemQueue.remove();
            if (item != null) {
                boolean itemTransferred = false;
                for (Map.Entry<Dropper, Chest> entry : pipe.getSortedOutputs().entrySet()) {
                    // first: look if it can be sorted anywhere
                    Dropper sorter = entry.getKey();
                    Chest chest = entry.getValue();
                    List<String> sortMaterials = new ArrayList<>();
                    for (ItemStack i : sorter.getInventory().getContents()) {
                        if (i != null) {
                            sortMaterials.add(i.getType() + ":" + i.getData().getData());
                        }
                    }
                    if (sortMaterials.contains(item.getType() + ":" + item.getData().getData())) {
                        if (chest.getInventory().firstEmpty() != -1) {
                            chest.getInventory().addItem(item);
                            dispenser.getInventory().remove(item);
                            itemTransferred = true;
                            break;
                        }
                    }
                }
                if (!itemTransferred) {
                    // the item cannot be sorted, so its moved to one of the random chests
                    for (Chest chest : pipe.getRandomOutputs()) {
                        if (chest.getInventory().firstEmpty() != -1) {
                            chest.getInventory().addItem(item);
                            dispenser.getInventory().remove(item);
                            break;
                        }
                    }
                }
            }
        }
    }
}
