package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;

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
     * displays particles around a pipe
     */
    public void highlight() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : pipeBlocks) {
            blocks.add(block);
        }
        for (PipeInput input : inputs) {
            blocks.add(input.getDispenser().getBlock());
        }
        for (PipeOutput output : outputs) {
            blocks.add(output.getDropper().getBlock());
            blocks.add(output.getDropper().getBlock().getRelative(PipesUtil.getDropperFace(output.getDropper())));
        }
        for (Block block : blocks) {
            Location location = block.getLocation();
            location.setX(location.getX() + 0.5);
            location.setY(location.getY() + 0.5);
            location.setZ(location.getZ() + 0.5);
            for (int i = 0; i < 3; i++) {
                block.getWorld().spigot().playEffect(location, Effect.FIREWORKS_SPARK, 0, 0,
                        0.1f, 0.1f, 0.1f, 0, 1, 50);
            }
            location = null;
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
        return pipeBlocks.equals(pipe.pipeBlocks);

    }

    @Override
    public int hashCode() {
        int result = inputs.hashCode();
        result = 31 * result + outputs.hashCode();
        result = 31 * result + pipeBlocks.hashCode();
        return result;
    }
}
