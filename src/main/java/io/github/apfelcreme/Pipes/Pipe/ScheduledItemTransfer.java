package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

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

    private SimpleLocation inputLocation;

    public ScheduledItemTransfer(SimpleLocation inputLocation) {
        this.inputLocation = inputLocation;
    }

    /**
     * executes the item transfer
     *
     * @return <tt>true</tt> if this transfer should be considered as completed and removed from the queue
     */
    public boolean execute() {
        Location location = inputLocation.getLocation();
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            // Chunk is not loaded, cannot transfer items
            return false;
        }

        Pipe pipe = PipeManager.getInstance().getPipe(inputLocation);
        if (pipe == null) {
            // No pipe at location? Remove the transfer
            return true;
        }

        PipeInput input = pipe.getInput(inputLocation);
        InventoryHolder inputHolder = input.getHolder();
        if (inputHolder == null) {
            // Could not find the input block, to not recheck this transfer we return true
            return true;
        }
        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : inputHolder.getInventory()) {
            if (itemStack != null) {
                itemQueue.add(itemStack);
            }
        }

        // createDetection the current transfer to all the running detections
        for (Detection detection : DetectionManager.getInstance().getDetections().values()) {
            detection.addLocation(new SimpleLocation(input.getLocation()));
        }

        boolean transferredAll = true;
        for (ItemStack item : itemQueue) {
            // first: try to place the item in a chest that uses filters. try furnaces first
            List<PipeOutput> outputs = pipe.getOutputs(item);
            if (!outputs.isEmpty()) {
                processItemTransfer(pipe, inputHolder, item, outputs);
                if (item.getAmount() != 0) {
                    transferredAll = false;
                }
            }
        }
        return transferredAll;
    }

    /**
     * handles the actual item transfer by trying to place them in one of the given outputs
     *
     * @param itemStack the item that shall be transferred
     * @param outputs   the list of outputs that are checked
     */
    private void processItemTransfer(Pipe pipe, InventoryHolder inputHolder, ItemStack itemStack, List<PipeOutput> outputs) {
        for (PipeOutput output : outputs) {
            InventoryHolder targetHolder = output.getTargetHolder();
            if (targetHolder != null && (output.getFilterItems().isEmpty() || PipesUtil.containsSimilar(output.getFilterItems(), itemStack))) {
                int originalAmount = itemStack.getAmount();
                switch (targetHolder.getInventory().getType()) {
                    case FURNACE:
                    /*
                    BEGIN FURNACE
                     */
                        // try to put coal etc in the correct place
                        Furnace furnace = (Furnace) targetHolder;
                        if (PipesUtil.isFuel(itemStack.getType())) {
                            // the transported item is either coal, or a coal block or a lava bucket
                            ItemStack fuel = furnace.getInventory().getFuel();
                            if (fuel != null && fuel.getType() != Material.AIR && fuel.isSimilar(itemStack)) {
                                // as you cannot mix two itemstacks with each other, check if the material inserted
                                // has the same type as the fuel that is already in the furnace
                                if (fuel.getMaxStackSize() == -1 || fuel.getAmount() < fuel.getMaxStackSize()) {
                                    // there is still room in the furnace
                                    int resultSize = itemStack.getAmount() - (fuel.getMaxStackSize() - fuel.getAmount());
                                    ItemStack result = itemStack.clone();
                                    if (resultSize <= 0) {
                                        inputHolder.getInventory().remove(itemStack);

                                        result.setAmount(fuel.getAmount() + itemStack.getAmount());
                                        furnace.getInventory().setFuel(result);

                                        itemStack.setAmount(0);
                                    } else {
                                        inputHolder.getInventory().remove(itemStack);
                                        itemStack.setAmount(resultSize);
                                        inputHolder.getInventory().addItem(itemStack);

                                        result.setAmount(itemStack.getMaxStackSize());
                                        furnace.getInventory().setFuel(result);

                                        itemStack.setAmount(itemStack.getMaxStackSize() - resultSize);
                                    }
                                } else {
                                    // the furnace is full, so find continue with the list of outputs
                                    // and try to fill one that isnt full
                                    continue;
                                }
                            } else if (fuel == null || fuel.getType() == Material.AIR) {
                                // there is no fuel currently in the fuel slot, so simply put it in
                                inputHolder.getInventory().remove(itemStack);
                                furnace.getInventory().setFuel(itemStack);
                            }
                        } else {
                            // the item is anything but a fuel (at least what we regard a fuel)
                            ItemStack smelting = furnace.getInventory().getSmelting();
                            if (smelting != null && smelting.isSimilar(itemStack)) {
                                // as you cannot mix two itemstacks with each other, check if the material inserted
                                // has the same type as the fuel that is already in the furnace
                                if (smelting.getMaxStackSize() == -1 || smelting.getAmount() < smelting.getMaxStackSize()) {
                                    // there is still room in the furnace
                                    int resultSize = itemStack.getAmount() - (smelting.getMaxStackSize() - smelting.getAmount());
                                    ItemStack result = itemStack.clone();
                                    if (resultSize <= 0) {
                                        inputHolder.getInventory().remove(itemStack);

                                        result.setAmount(smelting.getAmount() + itemStack.getAmount());
                                        furnace.getInventory().setSmelting(result);

                                        itemStack.setAmount(0);
                                    } else {
                                        inputHolder.getInventory().remove(itemStack);
                                        itemStack.setAmount(resultSize);
                                        inputHolder.getInventory().addItem(itemStack);

                                        result.setAmount(itemStack.getMaxStackSize());
                                        furnace.getInventory().setSmelting(result);

                                        itemStack.setAmount(itemStack.getMaxStackSize() - resultSize);
                                    }
                                }
                            } else if (smelting == null) {
                                inputHolder.getInventory().remove(itemStack);
                                furnace.getInventory().setSmelting(itemStack);
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
                        // for chests, dropper etc...
                        inputHolder.getInventory().remove(itemStack);
                        if (targetHolder.getInventory().firstEmpty() != -1) {
                            targetHolder.getInventory().addItem(itemStack);
                            itemStack.setAmount(0);
                        } else {
                            // CraftBukkit doesn't return leftovers when adding to partial stacks
                            // To work around this we add the items via an array with one item in
                            // each position so that we will get a leftover map in every case
                            ItemStack[] stacks = new ItemStack[itemStack.getAmount()];
                            for (int i = 0; i < itemStack.getAmount(); i++) {
                                ItemStack clone = new ItemStack(itemStack);
                                clone.setAmount(1);
                                stacks[i] = clone;
                            }
                            Map<Integer, ItemStack> rest = targetHolder.getInventory().addItem(stacks);
                            int newAmount = 0;
                            for (ItemStack item : rest.values()) {
                                newAmount += item.getAmount();
                            }
                            itemStack.setAmount(newAmount);
                            if (itemStack.getAmount() > 0) {
                                inputHolder.getInventory().addItem(itemStack);
                                itemStack.setAmount(newAmount); // Need to reset the amount as addItem might change the size
                            }
                        }
                        break;
                    /*
                    END DEFAULT
                     */
                }
                if (itemStack.getAmount() != originalAmount) {
                    PipeMoveItemEvent event = new PipeMoveItemEvent(pipe, inputHolder.getInventory(),
                            itemStack, targetHolder.getInventory(), true);
                    Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                    if (itemStack.getAmount() <= 0) {
                        return;
                    }
                }
            }

        }
    }

    public SimpleLocation getInputLocation() {
        return inputLocation;
    }
}




