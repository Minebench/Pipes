package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
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
                        input.getDispenser().getInventory().remove(item);

                        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                                input.getDispenser().getInventory(), item, output.getInventoryHolder().getInventory(), true);
                        Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                        itemTransferred = true;
                        break;
                    }
                }
            }
            // item could not be sorted! try to find a chest where no sorting is active
            if (!itemTransferred) {
                for (PipeOutput output : pipe.getOutputs()) {
                    List<String> sortMaterials = new ArrayList<>();
                    for (ItemStack i : output.getDropper().getInventory().getContents()) {
                        if (i != null) {
                            sortMaterials.add(i.getType() + ":" + i.getData().getData());
                        }
                    }
                    if (sortMaterials.isEmpty()) {
                        if (output.getInventoryHolder().getInventory().firstEmpty() != -1) {
                            //no sorting function
                            output.getInventoryHolder().getInventory().addItem(item);
                            input.getDispenser().getInventory().remove(item);

                            InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                                    input.getDispenser().getInventory(), item, output.getInventoryHolder().getInventory(), true);
                            Pipes.getInstance().getServer().getPluginManager().callEvent(event);
                            break;
                        }
                    }
                }
            }
        }
    }


}
