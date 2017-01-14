package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.Event.PipeMoveItemEvent;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesUtil;
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
    public boolean execute() {

        Queue<ItemStack> itemQueue = new LinkedList<>();
        for (ItemStack itemStack : input.getHolder().getInventory()) {
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
            boolean success;
            success = processItemTransfer(item, pipe.getOutputs(InventoryType.FURNACE, true));
            if (!success) success = processItemTransfer(item, pipe.getOutputs(null, true));

            // item could not be placed in an item filtering chest
            if (!success) success = processItemTransfer(item, pipe.getOutputs(InventoryType.FURNACE, false));
            if (!success) success = processItemTransfer(item, pipe.getOutputs(null, false));
            if (!success && transferredAll) transferredAll = false;
        }
        return transferredAll;
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
            if (PipesUtil.containsSimilar(output.getFilterItems(), itemStack) || output.getFilterItems().isEmpty()) {
                boolean success = false;
                switch (output.getTargetHolder().getInventory().getType()) {
                    case FURNACE:
                    /*
                    BEGIN FURNACE
                     */
                        // try to put coal etc in the correct place
                        Furnace furnace = (Furnace) output.getTargetHolder();
                        if (PipesUtil.isFuel(itemStack.getType())) {
                            // the transported item is either coal, or a coal block or a lava bucket
                            ItemStack fuel = furnace.getInventory().getFuel();
                            if (fuel != null && fuel.isSimilar(itemStack)) {
                                // as you cannot mix two itemstacks with each other, check if the material inserted
                                // has the same type as the fuel that is already in the furnace
                                if ((itemStack.getAmount() + fuel.getAmount()) <= 64) {
                                    // the combined amount of both stacks is <= 64, so simply merge them
                                    input.getHolder().getInventory().remove(itemStack);
                                    furnace.getInventory().setFuel(new ItemStack(itemStack.getType(),
                                            fuel.getAmount() + itemStack.getAmount(), itemStack.getDurability()));
                                    success = true;
                                } else {
                                    // the furnace is full, so find continue with the list of outputs
                                    // and try to fill one that isnt full
                                    continue;
                                }
                            } else if (fuel == null) {
                                // there is no fuel currently in the fuel slot, so simply put it in
                                input.getHolder().getInventory().remove(itemStack);
                                furnace.getInventory().setFuel(itemStack);
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
                                    input.getHolder().getInventory().remove(itemStack);
                                    furnace.getInventory().setSmelting(new ItemStack(itemStack.getType(),
                                            smelting.getAmount() + itemStack.getAmount(), itemStack.getDurability()));
                                    success = true;
                                } else {
                                    // the furnace is full, so find continue with the list of outputs
                                    // and try to fill one that isnt full
                                    continue;
                                }
                            } else if (smelting == null) {
                                input.getHolder().getInventory().remove(itemStack);
                                furnace.getInventory().setSmelting(itemStack);
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
                        if (output.getTargetHolder().getInventory().firstEmpty() != -1) {
                            input.getHolder().getInventory().remove(itemStack);
                            output.getTargetHolder().getInventory().addItem(itemStack);
                            success = true;
                        }
                        break;
                    /*
                    END DEFAULT
                     */
                }
                if (success) {
                    PipeMoveItemEvent event = new PipeMoveItemEvent(pipe, input.getHolder()
                            .getInventory(), itemStack, output.getTargetHolder().getInventory(), true);
                    Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                    return true;
                }
            }

        }
        return false;
    }

}




