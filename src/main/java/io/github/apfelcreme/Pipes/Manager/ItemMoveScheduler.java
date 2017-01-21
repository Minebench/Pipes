package io.github.apfelcreme.Pipes.Manager;

import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
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
public class ItemMoveScheduler {

    /**
     * the task id of the repeating task
     */
    private int taskId;

    /**
     * the queue that holds the items that are waiting to be transferred
     */
    private Set<SimpleLocation> scheduledItemTransfers;

    /**
     * the number of consecutive runs without any transfers (cancels at 4)
     */
    private int emptyRuns;

    /**
     * the scheduler instance
     */
    private static ItemMoveScheduler instance = null;

    private ItemMoveScheduler() {
        taskId = -1;
        scheduledItemTransfers = new LinkedHashSet<>();
        emptyRuns = 0;
    }

    /**
     * returns the scheduler instance
     *
     * @return the scheduler instance
     */
    public static ItemMoveScheduler getInstance() {
        if (instance == null) {
            instance = new ItemMoveScheduler();
        }
        return instance;
    }

    /**
     * starts a task
     */
    private void create() {
        taskId = Pipes.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(Pipes.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!scheduledItemTransfers.isEmpty()) {
                    Iterator<SimpleLocation> transfers = scheduledItemTransfers.iterator();
                    while (transfers.hasNext()) {
                        if (execute(transfers.next())) {
                            transfers.remove();
                        }
                    }
                } else {
                    emptyRuns++;
                    if (emptyRuns >= 3) {
                        kill();
                    }
                }
            }
        }, 20L, PipesConfig.getTransferCooldown());
    }

    /**
     * executes the item transfer
     *
     * @return <tt>true</tt> if this transfer should be considered as completed and removed from the queue
     */
    public boolean execute(SimpleLocation simpleLocation) {
        Location location = simpleLocation.getLocation();
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            // Chunk is not loaded, cannot transfer items
            return false;
        }

        Pipe pipe = PipeManager.getInstance().getPipe(simpleLocation);
        if (pipe == null) {
            // No pipe at location? Remove the transfer
            return true;
        }

        PipeInput input = pipe.getInput(simpleLocation);
        if (input == null) {
            // Could not find an input at that location, to not recheck this transfer we return true
            return true;
        }

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

        boolean transferredAll = true;

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
                                PipesUtil.addFuel(inputInventory, targetInventory, itemStack);
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
                                    ItemStack itemToSet = PipesUtil.moveToSingleSlot(inputInventory, smelting, itemStack);
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
                                if (!PipesUtil.addFuel(inputInventory, brewerInventory, itemStack)) {
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

                                    ItemStack result = new ItemStack(itemStack);
                                    result.setAmount(1);

                                    itemStack.setAmount(itemStack.getAmount() - 1);

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
                                    ItemStack itemToSet = PipesUtil.moveToSingleSlot(inputInventory, ingredient, itemStack);
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
                                PipesUtil.addItem(inputHolder.getInventory(), targetInventory, itemStack);
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
                        PipesUtil.addItem(inputHolder.getInventory(), targetInventory, itemStack);
                        break;
                    /*
                    END DEFAULT
                     */
                }
            }

            if (transferredAll && itemStack.getAmount() > 0) {
                transferredAll = false;
            }
        }

        return transferredAll;
    }


    /**
     * kills the task
     */
    private void kill() {
        Pipes.getInstance().getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
        emptyRuns = 0;
    }

    /**
     * is the task running at the moment?
     *
     * @return true or false
     */
    public boolean isActive() {
        return taskId != -1;
    }

    /**
     * schedules an item move
     *
     * @param scheduledItemTransfer the item transfer
     */
    public void add(SimpleLocation scheduledItemTransfer) {
        emptyRuns = 0;
        if (!scheduledItemTransfers.contains(scheduledItemTransfer)) {
            scheduledItemTransfers.add(scheduledItemTransfer);
        }
        if (!isActive() && !scheduledItemTransfers.isEmpty()) {
            create();
        }
    }

    public Set<SimpleLocation> getTransfers() {
        return scheduledItemTransfers;
    }

    public static void load() {
        YamlConfiguration oldTransfers = YamlConfiguration.loadConfiguration(new File(Pipes.getInstance().getDataFolder(), "transfers.yml"));
        for (Map locMap : oldTransfers.getMapList("transfers")) {
            try {
                SimpleLocation location = SimpleLocation.deserialize(locMap);
                getInstance().add(location);
            } catch (IllegalArgumentException e) {
                Pipes.getInstance().getLogger().log(Level.SEVERE, "Could not load transfer from transfers.yml: " + e.getMessage());
            }
        }
        Pipes.getInstance().getLogger().log(Level.INFO, "Loaded " + getInstance().getTransfers().size() + " scheduled transfers.");
    }

    public static void exit() {
        getInstance().kill();
        YamlConfiguration oldTransfers = new YamlConfiguration();
        List<Map<String, Object>> transferList = new ArrayList<>();
        for (SimpleLocation transfer : getInstance().getTransfers()) {
            transferList.add(transfer.serialize());
        }
        oldTransfers.set("transfers", transferList);
        try {
            oldTransfers.save(new File(Pipes.getInstance().getDataFolder(), "transfers.yml"));
            Pipes.getInstance().getLogger().log(Level.INFO, "Saved " + transferList.size() + " scheduled transfers.");
        } catch (IOException e) {
            Pipes.getInstance().getLogger().log(Level.SEVERE, "Could not write transfers to transfers.yml", e);
        }
    }

}
