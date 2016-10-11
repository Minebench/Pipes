package io.github.apfelcreme.Pipes.Pipe;

import org.bukkit.block.Block;

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
    private final List<Block> pipeBlocks;

    public Pipe(List<PipeInput> inputs, List<PipeOutput> outputs, List<Block> pipeBlocks) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.pipeBlocks = pipeBlocks;
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
     * returns the list of glass-blocks the pipe consists of
     *
     * @return the list of pipe blocks
     */
    public List<Block> getPipeBlocks() {
        return pipeBlocks;
    }

    /**
     * returns the pipe input object if there is one at the given blocks location
     * @param block a block
     * @return a pipeinput
     */
    public PipeInput getInput(Block block) {
        for (PipeInput pipeInput : inputs) {
            if (pipeInput.getDispenser().equals(block.getState())) {
                return pipeInput;
            }
        }
        return null;
    }

    /**
     * returns a string with some info in it
     *
     * @return a string with some info in it
     */
    public String getString() {
        return " In: " + inputs.size()
                + ", Out: " + (outputs.size())
                + ", Länge: " + (pipeBlocks.size());
    }
}
