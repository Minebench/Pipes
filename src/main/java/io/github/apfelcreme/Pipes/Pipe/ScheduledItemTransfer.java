package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Location;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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

        Inventory inputInventory = inputHolder.getInventory();

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

        // loop through all items and check if they should be handled by this output
        for (ItemStack itemStack : itemQueue) {

            // loop through all outputs
            for (PipeOutput output : pipe.getOutputs(itemStack)) {
                // we don't need to move empty/already moved itemstacks
                if (itemStack.getAmount() <= 0) {
                    continue;
                }

                // check if the pipe output is powered, if so don't try to put items in it
                if (output.isPowered()) {
                    continue;
                }

                InventoryHolder targetHolder = output.getTargetHolder();
                if (targetHolder == null) {
                    continue;
                }

                Inventory targetInventory = targetHolder.getInventory();
                // call move event before doing any moving to check if it was cancelled
                PipeMoveItemEvent pipeMoveEvent = new PipeMoveItemEvent(pipe, inputHolder.getInventory(),
                        itemStack, targetInventory);
                Pipes.getInstance().getServer().getPluginManager().callEvent(pipeMoveEvent);
                if (pipeMoveEvent.isCancelled()) {
                    continue;
                }

                switch (targetInventory.getType()) {
                    /*
                    BEGIN FURNACE
                     */
                    case FURNACE:
                        // try to put coal etc in the correct place
                        switch (itemStack.getType()) {
                            case COAL:
                            case COAL_BLOCK:
                            case LAVA_BUCKET:
                                // the transported item is either coal, or a coal block or a lava bucket
                                addFuel(inputInventory, targetInventory, itemStack);
                                break;
                            default:
                                // the item is anything but a fuel (at least what we regard a fuel)
                                FurnaceInventory furnaceInventory = (FurnaceInventory) targetInventory;
                                ItemStack smelting = furnaceInventory.getSmelting();
                                if (smelting == null) {
                                    inputInventory.removeItem(new ItemStack(itemStack));
                                    furnaceInventory.setSmelting(itemStack);
                                    itemStack.setAmount(0);
                                } else if (smelting.isSimilar(itemStack)) {
                                    ItemStack itemToSet = moveToSingleSlot(inputInventory, smelting, itemStack);
                                    if (itemToSet != null) {
                                        furnaceInventory.setSmelting(itemToSet);
                                    }
                                }
                                break;
                        }
                        break;
                    /*
                    END FURNACE
                     */
                    /*
                    BEGIN BREWING STAND
                     */
                    case BREWING:
                        BrewerInventory brewerInventory = (BrewerInventory) targetInventory;
                        switch (itemStack.getType()) {
                            case BLAZE_POWDER:
                                // the transported item is fuel
                                if (!addFuel(inputInventory, brewerInventory, itemStack)) {
                                    continue;
                                }
                                break;
                            case POTION:
                            case SPLASH_POTION:
                            case LINGERING_POTION:
                                int firstEmpty = brewerInventory.firstEmpty();
                                while (firstEmpty != -1 && firstEmpty < 3 && itemStack.getAmount() > 0) {
                                    ItemStack remove = new ItemStack(itemStack);
                                    remove.setAmount(1);
                                    inputInventory.removeItem(remove);
                                    itemStack.setAmount(itemStack.getAmount() - 1);

                                    ItemStack result = new ItemStack(itemStack);
                                    result.setAmount(1);
                                    brewerInventory.setItem(firstEmpty, result);
                                    if (itemStack.getAmount() > 0) {
                                        firstEmpty = brewerInventory.firstEmpty();
                                    }
                                }
                                break;
                            default:
                                ItemStack ingredient = brewerInventory.getIngredient();
                                if (ingredient == null) {
                                    inputInventory.removeItem(new ItemStack(itemStack));
                                    brewerInventory.setIngredient(itemStack);
                                    itemStack.setAmount(0);
                                } else if (ingredient.isSimilar(itemStack)) {
                                    ItemStack itemToSet = moveToSingleSlot(inputInventory, ingredient, itemStack);
                                    if (itemToSet != null) {
                                        brewerInventory.setIngredient(itemToSet);
                                    }
                                }
                                break;
                        }
                        break;
                    /*
                    END BREWING STAND
                     */
                    /*
                    BEGIN BEACON
                     */
                    case BEACON:
                        switch (itemStack.getType()) {
                            case DIAMOND:
                            case EMERALD:
                            case GOLD_INGOT:
                            case IRON_INGOT:
                                addItem(inputHolder.getInventory(), targetInventory, itemStack);
                                break;
                        }
                        break;
                    /*
                    END BEACON
                     */
                    /*
                    BEGIN DEFAULT
                     */
                    default:
                        // for chests, dropper etc...
                        addItem(inputHolder.getInventory(), targetInventory, itemStack);
                        break;
                    /*
                    END DEFAULT
                     */
                }
            }
        }

        for (ItemStack item : itemQueue) {
            if (item.getAmount() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an item to an inventory. This more complex method is necessary as CraftBukkit doesn't properly
     * return leftovers with partial item stacks
     * @param source The inventory that we move it from
     * @param target Where to move the item to
     * @param itemStack The item stack
     */
    private void addItem(Inventory source, Inventory target, ItemStack itemStack) {
        source.removeItem(itemStack);
        if (target.firstEmpty() != -1) {
            target.addItem(itemStack);
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
            Map<Integer, ItemStack> rest = target.addItem(stacks);
            int newAmount = 0;
            for (ItemStack item : rest.values()) {
                newAmount += item.getAmount();
            }
            itemStack.setAmount(newAmount);
            if (itemStack.getAmount() > 0) {
                source.addItem(itemStack);
                itemStack.setAmount(newAmount); // Need to reset the amount as addItem might change the size
            }
        }
    }

    /**
     * Add fuel to an inventory that supports fuel
     * @param source The inventory that we move it from
     * @param target Where to move the item to
     * @param itemStack The item stack
     * @return Whether or not the fuel was successfully set
     */
    private boolean addFuel(Inventory source, Inventory target, ItemStack itemStack) {
        ItemStack fuel = getFuel(target);
        if (fuel != null && fuel.isSimilar(itemStack)) {
            ItemStack itemToSet = moveToSingleSlot(source, fuel, itemStack);
            if (itemToSet == null) {
                return false;
            }

            setFuel(target, itemToSet);
        } else if (fuel == null) {
            // there is no fuel currently in the fuel slot, so simply put it in
            source.removeItem(itemStack);
            setFuel(target, itemStack);
        }
        return true;
    }

    private ItemStack moveToSingleSlot(Inventory source, ItemStack current, ItemStack added) {
        // as you cannot mix two itemstacks with each other, check if the material inserted
        // has the same type as the fuel that is already in current slot
        if (current.getAmount() < current.getMaxStackSize()) {
            // there is still room in the slot
            int remaining = current.getMaxStackSize() - current.getAmount(); // amount of room in the slot
            int restSize = added.getAmount() - remaining; // amount of overflowing items

            if (restSize > 0) {
                ItemStack remove = new ItemStack(added);
                remove.setAmount(remaining);
                source.removeItem(remove);
                added.setAmount(restSize);

                current.setAmount(current.getMaxStackSize());
            } else {
                source.removeItem(new ItemStack(added));

                current.setAmount(current.getAmount() + added.getAmount());

                added.setAmount(0);
            }

            return current;
        }

        // the inventory is full, so find continue with the list of outputs
        // and try to fill one that isnt full
        return null;
    }

    private ItemStack getFuel(Inventory inventory) {
        switch (inventory.getType()) {
            case BREWING:
                return ((BrewerInventory) inventory).getFuel();
            case FURNACE:
                return ((FurnaceInventory) inventory).getFuel();
            default:
                throw new IllegalArgumentException("Inventories of the type " + inventory.getType() + " do not have fuel!");
        }
    }

    private void setFuel(Inventory inventory, ItemStack itemStack) {
        switch (inventory.getType()) {
            case BREWING:
                ((BrewerInventory) inventory).setFuel(itemStack);
                break;
            case FURNACE:
                ((FurnaceInventory) inventory).setFuel(itemStack);
                break;
            default:
                throw new IllegalArgumentException("Inventories of the type " + inventory.getType() + " do not have fuel!");
        }
    }

    public SimpleLocation getInputLocation() {
        return inputLocation;
    }
}




