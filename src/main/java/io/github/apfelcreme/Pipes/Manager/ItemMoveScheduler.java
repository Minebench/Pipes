package io.github.apfelcreme.Pipes.Manager;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import io.github.apfelcreme.Pipes.Event.PipeDispenseEvent;
import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Exception.PipeTooLongException;
import io.github.apfelcreme.Pipes.Exception.TooManyOutputsException;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.Levelled;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/*
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

    private final Timing TIMINGS_MOVE_FILTER;
    private final Timing TIMINGS_MOVE_TRANSFER;
    private final Timing TIMINGS_MOVE_FILTER_AMOUNT;

    /**
     * the task id of the repeating task
     */
    private int taskId;

    /**
     * the queue that holds the items that are waiting to be transferred
     */
    private Set<SimpleLocation> scheduledItemTransfers;

    /**
     * item transfers that need to be added after the moves were run
     */
    private Set<SimpleLocation> addItemTransfers;

    /**
     * whether or not the scheduler is currently transferring
     */
    private boolean isTransferring;

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
        addItemTransfers = new LinkedHashSet<>();
        emptyRuns = 0;

        Timing timingsMove = Timings.of(Pipes.getInstance(), "move");
        TIMINGS_MOVE_FILTER = Timings.of(Pipes.getInstance(), "## filter", timingsMove);
        TIMINGS_MOVE_TRANSFER = Timings.of(Pipes.getInstance(), "## transfer", timingsMove);
        TIMINGS_MOVE_FILTER_AMOUNT = Timings.of(Pipes.getInstance(), "## filter_amount", timingsMove);
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
                isTransferring = true;
                scheduledItemTransfers.removeIf(this::execute);
                isTransferring = false;
                addQueued();
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
     * @param simpleLocation the location of the PipeInput
     * @return <code>true</code> if this transfer should be considered as completed and removed from the queue
     */
    public boolean execute(SimpleLocation simpleLocation) {
        Location location = simpleLocation.getLocation();
        if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            // Chunk is not loaded, cannot transfer items
            return false;
        }

        Pipe pipe;
        try {
            pipe = PipeManager.getInstance().getPipeByInput(simpleLocation);
        } catch (ChunkNotLoadedException | TooManyOutputsException | PipeTooLongException e) {
            // Is input of pipe but pipe is not valid, schedule it for next transfer
            return false;
        }
        if (pipe == null) {
            // No pipe at location? Remove the transfer
            return true;
        }

        if (pipe.getLastTransfer() != Bukkit.getCurrentTick()) {
            // Reset transfer count if no transfer occurred this tick
            pipe.setTransfers(0);
        } else if (PipesConfig.getTransferCount() > 0 && pipe.getTransfers() >= PipesConfig.getTransferCount()) {
            // Pipe already transferred more than the max transfer based on hard cap? Handle next tick
            return false;
        } else if (PipesConfig.getInputToOutputRatio() > 0 && pipe.getTransfers() >= pipe.getOutputs().size() * PipesConfig.getInputToOutputRatio()) {
            // Pipe already transferred more than the max transfer based on the input/output ratio? Handle next tick
            return false;
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

        boolean transferedAnything = false;
        boolean transferredAll = true;
        boolean spread = input.getOption(PipeInput.Options.SPREAD);
        boolean overflow = input.getOption(PipeInput.Options.OVERFLOW);

        // loop through all items and try to move them
        for (ItemStack itemStack : itemQueue) {
            transferedAnything |= moveItem(input, inputInventory, pipe, itemStack, spread, overflow);
            transferredAll &= transferedAnything;
        }

        if (!transferredAll && input.getOption(PipeInput.Options.MERGE)) {
            List<ItemStack> inputContents = Arrays.stream(inputInventory.getContents()).filter(Objects::nonNull).collect(Collectors.toList());
            if (inputContents.size() > 1) {
                inputInventory.clear();
                for (ItemStack item : inputContents) {
                    inputInventory.addItem(item);
                }
            }
        }
        inputHolder.update();

        if (transferedAnything) {
            // Update transfers
            pipe.setTransfers(pipe.getTransfers() + 1);
            pipe.setLastTransfer(Bukkit.getCurrentTick());
        }

        return transferredAll;
    }

    private boolean moveItem(PipeInput input, Inventory inputInventory, Pipe pipe, ItemStack itemStack, boolean spread, boolean overflow) {
        Map<PipeOutput, PipeOutput.AcceptResult> outputs = new LinkedHashMap<>();
        int filterCount = 0;
        try (Timing t = TIMINGS_MOVE_FILTER.startTiming()) {
            for (PipeOutput output : pipe.getOutputs().values()) {
                PipeOutput.AcceptResult acceptResult = output.accepts(input, itemStack);
                if (!spread || acceptResult.getType() == PipeOutput.ResultType.ACCEPT) {
                    outputs.put(output, acceptResult);
                    if (acceptResult.isInFilter()) {
                        filterCount++;
                    }
                }
            }

            if (outputs.isEmpty()) {
                return false;
            }

            outputs = outputs.entrySet().stream()
                    .sorted((e1, e2) -> e1.getValue().isInFilter() ? e2.getValue().isInFilter() ? 0 : -1 : e2.getValue().isInFilter() ? 1 : 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        }

        // Calculate amount that should be spread over the outputs (when in spread mode)
        int spreadOver = filterCount > 0 ? filterCount : outputs.size();
        int spreadAmount = itemStack.getAmount() / spreadOver;
        if (spread && spreadAmount == 0) { // not enough items to spread over all outputs, return
            return false;
        }

        try (Timing t = TIMINGS_MOVE_TRANSFER.startTiming()) {
            // loop through all outputs
            for (Map.Entry<PipeOutput, PipeOutput.AcceptResult> entry : outputs.entrySet()) {
                // we don't need to move empty/already moved itemstacks
                if (itemStack.getAmount() <= 0) {
                    return true;
                }

                PipeOutput output = entry.getKey();
                // Don't allow looping back into input
                if (output.getTargetLocation().equals(input.getTargetLocation())) {
                    continue;
                }
                Block targetBlock = output.getTargetLocation().getBlock();
                InventoryHolder targetHolder = output.getTargetHolder();
                Inventory targetInventory = targetHolder != null ? targetHolder.getInventory() : null;

                ItemStack transferring = itemStack;
                PipeOutput.AcceptResult acceptResult = entry.getValue();

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
                        && output.getOption(PipeOutput.Options.WHITELIST)
                        && output.getOption(PipeOutput.Options.TARGET_AMOUNT)) {
                    int amountInTarget = 0;
                    try (Timing t2 = TIMINGS_MOVE_FILTER_AMOUNT.startTiming()) {
                        for (ItemStack item : targetInventory) {
                            if (output.matchesFilter(acceptResult.getFilterItem(), item)) {
                                amountInTarget += item.getAmount();
                                if (amountInTarget > acceptResult.getFilterItem().getAmount()) {
                                    acceptResult = new PipeOutput.AcceptResult(PipeOutput.ResultType.DENY_AMOUNT, acceptResult.getFilterItem());
                                    break;
                                }
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

                PipeOutput.Options.Overflow outputOverflow = output.getOption(PipeOutput.Options.OVERFLOW);
                if (acceptResult.getType() != PipeOutput.ResultType.ACCEPT) {
                    if (!spread && acceptResult.isInFilter() &&
                            (outputOverflow == PipeOutput.Options.Overflow.FALSE || (!overflow && outputOverflow == PipeOutput.Options.Overflow.INPUT))) {
                        return false;
                    }
                    continue;
                }

                if (output.getOption(PipeOutput.Options.DROP)) {
                    Location dropLocation = output.getTargetLocation().getLocation().add(0.5, 0.5, 0.5);

                    double speed = PipesUtil.RANDOM.nextDouble() * 0.1d + 0.2d;
                    Vector motion = new Vector(
                            output.getFacing().getModX() * speed + PipesUtil.RANDOM.nextGaussian() * 0.0075 * 6,
                            0.2 + PipesUtil.RANDOM.nextGaussian() * 0.0075 * 6,
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
                    dropLocation.getWorld().playEffect(dropLocation, Effect.SMOKE, output.getFacing() != BlockFace.DOWN ? output.getFacing() : BlockFace.SELF);

                } else if (targetInventory != null) {
                    // call move event before doing any moving to check if it was cancelled
                    PipeMoveItemEvent pipeMoveEvent = new PipeMoveItemEvent(pipe, output, inputInventory, transferring, targetInventory);
                    Pipes.getInstance().getServer().getPluginManager().callEvent(pipeMoveEvent);
                    if (pipeMoveEvent.isCancelled()) {
                        continue;
                    }

                    boolean smartInsert = output.getOption(PipeOutput.Options.SMART_INSERT);

                    switch (targetInventory.getType()) {
                    /*
                    BEGIN FURNACE
                     */
                        case FURNACE:
                        case SMOKER:
                        case BLAST_FURNACE:
                            // try to put coal etc in the correct place
                            if (transferring.getType().isFuel() && (smartInsert || (output.getFacing() != BlockFace.DOWN && output.getFacing() != BlockFace.UP))) {
                                PipesUtil.addFuel(inputInventory, targetInventory, transferring);
                            } else if (smartInsert || output.getFacing() == BlockFace.DOWN) {
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
                                case BLAZE_POWDER:
                                    // the transported item is fuel
                                    // only insert if pointing from the side, smart insert will treat it as an ingredient
                                    if (!smartInsert && output.getFacing() != BlockFace.DOWN && output.getFacing() != BlockFace.UP) {
                                        if (!PipesUtil.addFuel(inputInventory, brewerInventory, transferring)) {
                                            continue;
                                        }
                                        break;
                                    }
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
                    BEGIN SHULKER_BOX
                     */
                        case SHULKER_BOX:
                            if (Tag.SHULKER_BOXES.isTagged(transferring.getType())) {
                                // Don't allow shulker boxes inside shulker boxes
                                break;
                            }
                    /*
                    END SHULKER_BOX
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
                } else if (targetBlock.getType() == Material.COMPOSTER
                        && (output.getFacing() == BlockFace.DOWN || output.getOption(PipeOutput.Options.SMART_INSERT))) {
                    double itemChance = PipesUtil.getCompostableChance(itemStack.getType());
                    if (itemChance > 0) {
                        Levelled composter = (Levelled) targetBlock.getBlockData();
                        int layersToFill = composter.getMaximumLevel() - composter.getLevel();
                        if (layersToFill > 0) {
                            int layers = (int) Math.round(itemChance * transferring.getAmount());
                            if (layers > layersToFill) {
                                composter.setLevel(composter.getMaximumLevel());
                                transferring.setAmount(transferring.getAmount() - (int) Math.round(layersToFill / itemChance));
                            } else {
                                composter.setLevel(composter.getLevel() + layers);
                                transferring.setAmount(0);
                            }
                            targetBlock.setBlockData(composter);
                        }
                    }
                }

                if (itemStack != transferring) {
                    // Check if the item stack that we transferred is the one that was given to us.
                    // If not merge their amounts (this split can happen due to the amount filtering and spreading)
                    itemStack.setAmount(leftOverAmount + transferring.getAmount());
                }

                if (!spread && itemStack.getAmount() > 0 && acceptResult.isInFilter() &&
                        (outputOverflow == PipeOutput.Options.Overflow.FALSE || (!overflow && outputOverflow == PipeOutput.Options.Overflow.INPUT))) {
                    return false;
                }
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
        if (!isTransferring) {
            scheduledItemTransfers.add(scheduledItemTransfer);
        } else if (!scheduledItemTransfers.contains(scheduledItemTransfer)) {
            addItemTransfers.add(scheduledItemTransfer);
        }
        if (!isActive() && (!scheduledItemTransfers.isEmpty() || !addItemTransfers.isEmpty())) {
            create();
        }
    }

    private void addQueued() {
        scheduledItemTransfers.addAll(addItemTransfers);
        addItemTransfers.clear();
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
