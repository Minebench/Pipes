package io.github.apfelcreme.Pipes;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;

import java.util.List;
import java.util.Map;

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

    private final List<Dispenser> inputs;
    private final List<Chest> randomOutputs;
    private final Map<Dropper, Chest> sortedOutputs;
    private final List<Block> pipeBlocks;

    public Pipe(List<Dispenser> inputs, List<Chest> randomOutputs, Map<Dropper, Chest> sortedOutputs, List<Block> pipeBlocks) {
        this.inputs = inputs;
        this.randomOutputs = randomOutputs;
        this.sortedOutputs = sortedOutputs;
        this.pipeBlocks = pipeBlocks;
    }

    /**
     * returns the list of dispensers that act as inputs
     *
     * @return the list of inputs
     */
    public List<Dispenser> getInputs() {
        return inputs;
    }

    /**
     * returns the map of chests which are connected to a sorter
     *
     * @return the output chests
     */
    public Map<Dropper, Chest> getSortedOutputs() {
        return sortedOutputs;
    }

    /**
     * returns the list of chests which are not connected to a sorter
     *
     * @return the output chests
     */
    public List<Chest> getRandomOutputs() {
        return randomOutputs;
    }

    /**
     * returns the list of blocks the pipe consists of
     *
     * @return the list of pipe blocks
     */
    public List<Block> getPipeBlocks() {
        return pipeBlocks;
    }

    /**
     * returns a string with some info in it
     *
     * @return a string with some info in it
     */
    public String getString() {
        return " In: " + inputs.size()
                + ", Out: " + (randomOutputs.size() + sortedOutputs.keySet().size())
                + ", Sortierer: " + sortedOutputs.values().size()
                + ", Länge: " + (pipeBlocks.size() - inputs.size() - randomOutputs.size() - sortedOutputs.keySet().size() - sortedOutputs.values().size());
    }
}
