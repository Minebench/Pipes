package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
public class Pipe {

    private final List<PipeInput> inputs;
    private final List<PipeOutput> outputs;
    private final List<ChunkLoader> chunkLoaders;
    private final List<SimpleLocation> pipeBlocks;
    private final DyeColor color;

    public Pipe(List<PipeInput> inputs, List<PipeOutput> outputs,
                List<ChunkLoader> chunkLoaders, List<SimpleLocation> pipeBlocks, DyeColor color) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.chunkLoaders = chunkLoaders;
        this.pipeBlocks = pipeBlocks;
        this.color = color;
    }

    /**
     * returns the list of inputs
     *
     * @return the list of inputs
     */
    public List<PipeInput> getInputs() {
        return inputs;
    }

    /**
     * returns the list of outputs which are connected to a sorter
     *
     * @return the list of outputs
     */
    public List<PipeOutput> getOutputs() {
        return outputs;
    }

    /**
     * returns a list of outputs with the given inventory types, with or
     * without a sorting function
     *
     * @param inventoryType the type inventory holder you wish to get, null for all
     * @param filtering     only get outputs with a filtering function
     * @return a list of outputs matching the given parameters
     */
    public List<PipeOutput> getOutputs(InventoryType inventoryType, boolean filtering) {
        List<PipeOutput> sorterOutputs = new ArrayList<>();
        for (PipeOutput output : outputs) {
            InventoryHolder targetHolder = output.getTargetHolder();
            if (inventoryType == null || targetHolder != null && targetHolder.getInventory().getType() == inventoryType) {
                if ((filtering && !output.getFilterItems().isEmpty()) // there are items as filters + user wants sorters
                        || (!filtering && output.getFilterItems().isEmpty())) { // there are no items + user doesn't want sorters
                    InventoryHolder holder = output.getOutputHolder();
                    if (holder != null && !((BlockState) holder).getBlock().isBlockPowered()) {
                        sorterOutputs.add(output);
                    }
                }
            }
        }
        return sorterOutputs;
    }

    /**
     * Get the outputs that match this item
     *
     * @param item
     * @return The filtered outputs that match this item or every unfiltered ones
     */
    public List<PipeOutput> getOutputs(ItemStack item) {
        List<PipeOutput> filteredOutputs = new ArrayList<>();
        List<PipeOutput> unfilteredOutputs = new ArrayList<>();
        for (PipeOutput output : outputs) {
            List<ItemStack> filterItems = output.getFilterItems();
            if (filterItems.isEmpty()) {
                unfilteredOutputs.add(output);
            } else if (PipesUtil.containsSimilar(filterItems, item)) {
                filteredOutputs.add(output);
            }
        }
        return filteredOutputs.isEmpty() ? unfilteredOutputs : filteredOutputs;
    }

    /**
     * returns the list of furnaces that are connected to a pipe that allows the
     * server to load chunks if parts of the pipe are located in unloaded chunks
     *
     * @return a list of chunk loaders
     */
    public List<ChunkLoader> getChunkLoaders() {
        return chunkLoaders;
    }

    /**
     * returns the list of glass-blocks the pipe consists of
     *
     * @return the list of pipe blocks
     */
    public List<SimpleLocation> getPipeBlocks() {
        return pipeBlocks;
    }

    /**
     * returns the pipe input object if there is one at the given location
     *
     * @param location a SimpleLocation
     * @return a pipeinput
     */
    public PipeInput getInput(SimpleLocation location) {
        for (PipeInput pipeInput : inputs) {
            if (pipeInput.getLocation().equals(location)) {
                return pipeInput;
            }
        }
        return null;
    }

    /**
     * Get the color of this pipe
     *
     * @return The color of the stained glass blocks this pipe consists of
     */
    public DyeColor getColor() {
        return color;
    }

    /**
     * displays particles around a pipe
     */
    public void highlight() {
        List<SimpleLocation> locations = new ArrayList<>();
        for (SimpleLocation simpleLocation : pipeBlocks) {
            locations.add(simpleLocation);
        }
        for (PipeInput input : inputs) {
            locations.add(input.getLocation());
        }
        for (PipeOutput output : outputs) {
            locations.add(output.getLocation());
            locations.add(output.getTargetLocation());
        }
        for (SimpleLocation simpleLocation : locations) {
            Location location = simpleLocation.getLocation();
            location.setX(location.getX() + 0.5);
            location.setY(location.getY() + 0.5);
            location.setZ(location.getZ() + 0.5);
            for (int i = 0; i < 3; i++) {
                location.getWorld().spigot().playEffect(location, Effect.FIREWORKS_SPARK, 0, 0,
                        0.1f, 0.1f, 0.1f, 0, 1, 50);
            }
        }
    }

    /**
     * returns a string with some info in it
     *
     * @return a string with some info in it
     */
    public String getString() {
        return PipesConfig.getText("info.pipe.pipeData")
                .replace("{0}", String.valueOf(inputs.size()))
                .replace("{1}", String.valueOf(outputs.size()))
                .replace("{2}", String.valueOf(pipeBlocks.size()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pipe pipe = (Pipe) o;

        if (!inputs.equals(pipe.inputs)) return false;
        if (!outputs.equals(pipe.outputs)) return false;
        if (!chunkLoaders.equals(pipe.chunkLoaders)) return false;
        return pipeBlocks.equals(pipe.pipeBlocks);

    }

    @Override
    public int hashCode() {
        int result = inputs.hashCode();
        result = 31 * result + outputs.hashCode();
        result = 31 * result + chunkLoaders.hashCode();
        result = 31 * result + pipeBlocks.hashCode();
        result = 31 * result + color.hashCode();
        return result;
    }
}
