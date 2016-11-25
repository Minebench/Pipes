package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

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
public class ScheduledItemTransfer {

    private Pipe pipe;
    private PipeInput input;

    public ScheduledItemTransfer(Pipe pipe, PipeInput input) {
        this.pipe = pipe;
        this.input = input;
    }

    /**
     * executes the item transfer
     */
    public void execute() {

        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : input.getDispenser().getInventory()) {
            if (itemStack != null) {
                itemQueue.add(itemStack);
            }
        }

        // createDetection the current transfer to all the running detections
        for (Detection detection : DetectionManager.getInstance().getDetections().values()) {
            detection.addLocation(
                    new SimpleLocation(
                            input.getDispenser().getWorld().getName(),
                            input.getDispenser().getX(),
                            input.getDispenser().getY(),
                            input.getDispenser().getZ()));
        }

        for (ItemStack item : itemQueue) {
            // first: try to place the item in a chest that uses filters. try furnaces first
            boolean success;
            success = processItemTransfer(item, pipe.getOutputs(InventoryType.FURNACE, true));
            if (!success) success = processItemTransfer(item, pipe.getOutputs(null, true));

            // item could not be placed in an item filtering chest
            if (!success) success = processItemTransfer(item, pipe.getOutputs(InventoryType.FURNACE, false));
            if (!success) processItemTransfer(item, pipe.getOutputs(null, false));
        }
    }

    /**
     * handles the actual item transfer by trying to place them in one of the given outputs
     *
     * @param itemStack the item that shall be transferred
     * @param outputs   the list of outputs that are checked
     * @return true if the item was transferred, false if not
     */
    private boolean processItemTransfer(ItemStack itemStack, List<PipeOutput> outputs) {
        for (PipeOutput output : outputs) {
            if (containsSimilar(output.getFilterItems(), itemStack) || output.getFilterItems().isEmpty()) {
                boolean success = false;
                switch (output.getInventoryHolder().getInventory().getType()) {
                    case FURNACE:
                    /*
                    BEGIN FURNACE
                     */
                        // try to put coal etc in the correct place
                        Furnace furnace = (Furnace) output.getInventoryHolder();
                        if (isFuel(itemStack.getType())) {
                            // the transported item is either coal, or a coal block or a lava bucket
                            ItemStack fuel = furnace.getInventory().getFuel();
                            if (fuel != null && fuel.isSimilar(itemStack)) {
                                // as you cannot mix two itemstacks with each other, check if the material inserted
                                // has the same type as the fuel that is already in the furnace
                                if ((itemStack.getAmount() + fuel.getAmount()) <= 64) {
                                    // the combined amount of both stacks is <= 64, so simply merge them
                                    furnace.getInventory().setFuel(new ItemStack(itemStack.getType(),
                                            fuel.getAmount() + itemStack.getAmount(), itemStack.getDurability()));
                                    input.getDispenser().getInventory().remove(itemStack);
                                    success = true;
                                } else {
                                    // the combined amount is greater than 64, so calculate the leftover stack and
                                    // place it in the dispenser
                                    int taken = 64 - fuel.getAmount();
                                    furnace.getInventory().setFuel(new ItemStack(itemStack.getType(), 64, itemStack.getDurability()));
                                    PipesUtil.removeItems(input.getDispenser().getInventory(), itemStack.getType(), taken, true);
                                    success = true;
                                }
                            } else if (fuel == null) {
                                // there is no fuel currently in the fuel slot, so simply put it in
                                furnace.getInventory().setFuel(itemStack);
                                input.getDispenser().getInventory().remove(itemStack);
                                success = true;
                            }
                        } else {
                            // the item is anything but a fuel (at least what we regard a fuel)
                            ItemStack smelting = furnace.getInventory().getSmelting();
                            if (smelting != null && smelting.isSimilar(itemStack)) {
                                // as you cannot mix two itemstacks with each other, check if the material inserted
                                // has the same type as the fuel that is already in the furnace
                                if ((itemStack.getAmount() + smelting.getAmount()) <= 64) {
                                    // the combined amount of both stacks is <= 64, so simply merge them
                                    furnace.getInventory().setSmelting(new ItemStack(itemStack.getType(),
                                            smelting.getAmount() + itemStack.getAmount(), itemStack.getDurability()));
                                    input.getDispenser().getInventory().remove(itemStack);
                                    success = true;
                                } else {
                                    // the combined amount is greater than 64, so calculate the leftover stack and
                                    // place it in the dispenser
                                    int taken = 64 - smelting.getAmount();
                                    furnace.getInventory().setSmelting(new ItemStack(itemStack.getType(), 64, itemStack.getDurability()));
                                    PipesUtil.removeItems(input.getDispenser().getInventory(), itemStack.getType(), taken, true);
                                    success = true;
                                }
                            } else if (smelting == null) {
                                furnace.getInventory().setSmelting(itemStack);
                                input.getDispenser().getInventory().remove(itemStack);
                                success = true;
                            }
                        }
                        break;
                    /*
                    END FURNACE
                     */
                    default:
                    /*
                    BEGIN DEFAULT
                     */
                        // for chests, ender-chests, dropper etc...
                        if (output.getInventoryHolder().getInventory().firstEmpty() != -1) {
                            output.getInventoryHolder().getInventory().addItem(itemStack);
                            input.getDispenser().getInventory().remove(itemStack);
                            success = true;
                        }
                        break;
                    /*
                    END DEFAULT
                     */
                }
                if (success) {
                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(input.getDispenser()
                            .getInventory(), itemStack, output.getInventoryHolder().getInventory(), true);
                    Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * checks if the given list of items contains an itemstack similar to the given item stack
     *
     * @param items     a list of item stacks
     * @param itemStack an item stack
     * @return true if there is an item stack of the same type with the same data. Amount may vary
     */
    private boolean containsSimilar(List<ItemStack> items, ItemStack itemStack) {
        for (ItemStack item : items) {
            if (item.isSimilar(itemStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if a type can be used for smelting in a furnace
     *
     * @param type a material type
     * @return true or false
     */
    private boolean isFuel(Material type) {
        switch (type) {
            case COAL:
            case COAL_BLOCK:
            case LAVA_BUCKET:
                return true;
            default:
                return false;
        }
    }

}




