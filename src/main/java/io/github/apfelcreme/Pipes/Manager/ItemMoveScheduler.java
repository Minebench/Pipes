package io.github.apfelcreme.Pipes.Manager;

import io.github.apfelcreme.Pipes.Event.PipeDispenseEvent;
import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Dropper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        taskId = Pipes.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(Pipes.getInstance(), () -> {
            if (!scheduledItemTransfers.isEmpty()) {
                scheduledItemTransfers.removeIf(this::execute);
            } else {
                emptyRuns++;
                if (emptyRuns >= 3) {
                    kill();
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
        if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
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

        Container inputHolder = input.getHolder();
        if (inputHolder == null) {
            // Could not find the input block, to not recheck this transfer we return true
            return true;
        }

        Inventory inputInventory = inputHolder.getInventory();
        List<ItemStack> itemQueue = new ArrayList<>();
        for (ItemStack itemStack : inputInventory) {
            if (itemStack != null) {
                itemQueue.add(itemStack);
            }
        }

        // add the current transfer to all the running detections
        for (Detection detection : DetectionManager.getInstance().getDetections().values()) {
            detection.addLocation(new SimpleLocation(input.getLocation()));
        }

        boolean transferredAll = true;
        boolean spread = (boolean) input.getOption(PipeInput.Option.SPREAD);

        // loop through all items and try to move them
        for (ItemStack itemStack : itemQueue) {
            transferredAll &= moveItem(inputInventory, pipe, itemStack, spread);
        }

        // Set snapshot contents to real contents so that we can call an update (for redstone)
        inputHolder.getSnapshotInventory().setContents(inputInventory.getContents());
        inputHolder.update();

        return transferredAll;
    }

    private boolean moveItem(Inventory inputInventory, Pipe pipe, ItemStack itemStack, boolean spread) {
        Map<PipeOutput, PipeOutput.AcceptResult> outputs = new LinkedHashMap<>();
        if (spread) {
            for (PipeOutput output : pipe.getOutputs()) {
                PipeOutput.AcceptResult acceptResult = output.accepts(itemStack);
                if (acceptResult.getType() == PipeOutput.ResultType.ACCEPT) {
                    outputs.put(output, acceptResult);
                }
            }
        } else {
            for (PipeOutput output : pipe.getOutputs()) {
                outputs.put(output, null);
            }
        }

        // Calculate amount that should be spread over the outputs (when in spread mode)
        int spreadAmount = itemStack.getAmount() / outputs.size();
        if (spread && spreadAmount == 0) { // not enough items to spread over all outputs, return
            return false;
        }

        // loop through all outputs
        for (Map.Entry<PipeOutput, PipeOutput.AcceptResult> entry : outputs.entrySet()) {
            // we don't need to move empty/already moved itemstacks
            if (itemStack.getAmount() <= 0) {
                return true;
            }

            PipeOutput output = entry.getKey();
            InventoryHolder targetHolder = output.getTargetHolder();
            Inventory targetInventory = null;
            if (targetHolder != null) {
                targetInventory = targetHolder.getInventory();
            }

            ItemStack transferring = itemStack;
            PipeOutput.AcceptResult acceptResult = spread ? entry.getValue() : output.accepts(transferring);

            // Set the spread amount
            if (spread && spreadAmount < transferring.getAmount()) {
                if (transferring == itemStack) {
                    transferring = new ItemStack(transferring);
                }
                transferring.setAmount(spreadAmount);
            }

            // Check the target amount option
            if (targetInventory != null
                    && acceptResult.getType() == PipeOutput.ResultType.ACCEPT
                    && acceptResult.isInFilter()
                    && (boolean) output.getOption(PipeOutput.Option.WHITELIST)
                    && (boolean) output.getOption(PipeOutput.Option.TARGET_AMOUNT)) {
                int amountInTarget = 0;
                for (ItemStack item : targetInventory) {
                    if (output.matchesFilter(acceptResult.getFilterItem(), item)) {
                        amountInTarget += item.getAmount();
                        if (amountInTarget > acceptResult.getFilterItem().getAmount()) {
                            acceptResult = new PipeOutput.AcceptResult(PipeOutput.ResultType.DENY_AMOUNT, acceptResult.getFilterItem());
                            break;
                        }
                    }
                }
                if (amountInTarget + transferring.getAmount() > acceptResult.getFilterItem().getAmount()) {
                    if (transferring == itemStack) {
                        transferring = new ItemStack(transferring);
                    }
                    transferring.setAmount(acceptResult.getFilterItem().getAmount() - amountInTarget);
                }
            }

            // Calculate the amount not transferred
            int leftOverAmount = transferring == itemStack ? 0 : itemStack.getAmount() - transferring.getAmount();

            boolean overflowIsAllowed = (boolean) output.getOption(PipeOutput.Option.OVERFLOW);
            if (acceptResult.getType() != PipeOutput.ResultType.ACCEPT) {
                if (!overflowIsAllowed && !spread && acceptResult.isInFilter()) {
                    return false;
                }
                continue;
            }

            if ((boolean) output.getOption(PipeOutput.Option.DROP)) {
                Location dropLocation = output.getTargetLocation().getLocation().add(0.5, 0.5, 0.5);

                double speed = PipesUtil.RANDOM.nextDouble() * 0.1d + 0.2d;
                Vector motion = new Vector(
                        output.getFacing().getModX() * speed + PipesUtil.RANDOM.nextGaussian() * 0.0075 * 6,
                        0.2                                  + PipesUtil.RANDOM.nextGaussian() * 0.0075 * 6,
                        output.getFacing().getModZ() * speed + PipesUtil.RANDOM.nextGaussian() * 0.0075 * 6
                );

                PipeDispenseEvent pipeDispenseEvent = new PipeDispenseEvent(pipe, output, transferring, motion);
                Pipes.getInstance().getServer().getPluginManager().callEvent(pipeDispenseEvent);
                if (pipeDispenseEvent.isCancelled()) {
                    continue;
                }

                ItemStack dropping = new ItemStack(transferring);
                transferring.setAmount(0);
                Item droppedItem = dropLocation.getWorld().dropItem(dropLocation, dropping);
                droppedItem.setVelocity(pipeDispenseEvent.getVelocity());

                dropLocation.getWorld().playEffect(dropLocation, Effect.CLICK2, null);
                dropLocation.getWorld().playEffect(dropLocation, Effect.SMOKE, output.getFacing().getOppositeFace());

            } else if (targetInventory != null) {
                // call move event before doing any moving to check if it was cancelled
                PipeMoveItemEvent pipeMoveEvent = new PipeMoveItemEvent(pipe, output, inputInventory, transferring, targetInventory);
                Pipes.getInstance().getServer().getPluginManager().callEvent(pipeMoveEvent);
                if (pipeMoveEvent.isCancelled()) {
                    continue;
                }

                boolean smartInsert = (boolean) output.getOption(PipeOutput.Option.SMART_INSERT);

                switch (targetInventory.getType()) {
                    /*
                    BEGIN FURNACE
                     */
                    case FURNACE:
                        // try to put coal etc in the correct place
                        if (transferring.getType().isFuel()) {
                            if (smartInsert || (output.getFacing() != BlockFace.DOWN && output.getFacing() != BlockFace.UP)) {
                                PipesUtil.addFuel(inputInventory, targetInventory, transferring);
                            }
                        } else {
                            if (smartInsert || output.getFacing() == BlockFace.DOWN) {
                                FurnaceInventory furnaceInventory = (FurnaceInventory) targetInventory;
                                ItemStack smelting = furnaceInventory.getSmelting();
                                if (smelting == null) {
                                    inputInventory.removeItem(new ItemStack(transferring));
                                    furnaceInventory.setSmelting(transferring);
                                    transferring.setAmount(0);
                                } else if (smelting.isSimilar(transferring)) {
                                    ItemStack itemToSet = PipesUtil.moveToSingleSlot(inputInventory, smelting, transferring);
                                    if (itemToSet != null) {
                                        furnaceInventory.setSmelting(itemToSet);
                                    }
                                }
                            }
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
                        switch (transferring.getType()) {
                            case BLAZE_POWDER:
                                // the transported item is fuel
                                if (smartInsert || (output.getFacing() != BlockFace.DOWN && output.getFacing() != BlockFace.UP)) {
                                    if (!PipesUtil.addFuel(inputInventory, brewerInventory, transferring)) {
                                        continue;
                                    }
                                }
                                break;
                            case POTION:
                            case SPLASH_POTION:
                            case LINGERING_POTION:
                                if (smartInsert || (output.getFacing() != BlockFace.DOWN && output.getFacing() != BlockFace.UP)) {
                                    ItemStack ingredient = brewerInventory.getIngredient();
                                    if (!PipesUtil.potionAcceptsIngredient(transferring, ingredient)) {
                                        break;
                                    }
                                    int firstEmpty = brewerInventory.firstEmpty();
                                    while (firstEmpty != -1 && firstEmpty < 3 && transferring.getAmount() > 0) {
                                        ItemStack remove = new ItemStack(transferring);
                                        remove.setAmount(1);
                                        inputInventory.removeItem(remove);

                                        ItemStack result = new ItemStack(transferring);
                                        result.setAmount(1);

                                        transferring.setAmount(transferring.getAmount() - 1);

                                        brewerInventory.setItem(firstEmpty, result);
                                        if (transferring.getAmount() > 0) {
                                            firstEmpty = brewerInventory.firstEmpty();
                                        }
                                    }
                                }
                                break;
                            default:
                                if (smartInsert || output.getFacing() == BlockFace.DOWN) {
                                    ItemStack ingredient = brewerInventory.getIngredient();
                                    if (ingredient == null) {
                                        inputInventory.removeItem(new ItemStack(transferring));
                                        brewerInventory.setIngredient(transferring);
                                        transferring.setAmount(0);
                                    } else if (ingredient.isSimilar(transferring)) {
                                        ItemStack itemToSet = PipesUtil.moveToSingleSlot(inputInventory, ingredient, transferring);
                                        if (itemToSet != null) {
                                            brewerInventory.setIngredient(itemToSet);
                                        }
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
                        switch (transferring.getType()) {
                            case DIAMOND:
                            case EMERALD:
                            case GOLD_INGOT:
                            case IRON_INGOT:
                                PipesUtil.addItem(targetInventory, transferring);
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
                        PipesUtil.addItem(targetInventory, transferring);
                        break;
                    /*
                    END DEFAULT
                     */
                }
            }

            if (itemStack != transferring) {
                // Check if the item stack that we transferred is the one that was given to us.
                // If not merge their amounts (this split can happen due to the amount filtering and spreading)
                itemStack.setAmount(leftOverAmount + transferring.getAmount());
            }

            if (!spread && itemStack.getAmount() > 0 && acceptResult.isInFilter() && !overflowIsAllowed) {
                return false;
            }
        }

        return itemStack.getAmount() <= 0;
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
