package io.github.apfelcreme.Pipes.Pipe;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.PipesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
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

    private final LinkedHashSet<PipeInput> inputs;
    private final LinkedHashSet<PipeOutput> outputs;
    private final LinkedHashSet<ChunkLoader> chunkLoaders;
    private final LinkedHashSet<SimpleLocation> pipeBlocks;
    private final Material type;

    public Pipe(LinkedHashSet<PipeInput> inputs, LinkedHashSet<PipeOutput> outputs,
                LinkedHashSet<ChunkLoader> chunkLoaders, LinkedHashSet<SimpleLocation> pipeBlocks, Material type) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.chunkLoaders = chunkLoaders;
        this.pipeBlocks = pipeBlocks;
        this.type = type;
    }

    /**
     * returns the set of inputs
     *
     * @return the set of inputs
     */
    public LinkedHashSet<PipeInput> getInputs() {
        return inputs;
    }

    /**
     * returns the set of outputs which are connected to a sorter
     *
     * @return the set of outputs
     */
    public LinkedHashSet<PipeOutput> getOutputs() {
        return outputs;
    }

    /**
     * returns the set of furnaces that are connected to a pipe that allows the
     * server to load chunks if parts of the pipe are located in unloaded chunks
     *
     * @return a set of chunk loaders
     */
    public LinkedHashSet<ChunkLoader> getChunkLoaders() {
        return chunkLoaders;
    }

    /**
     * returns the set of glass-blocks the pipe consists of
     *
     * @return the set of pipe blocks
     */
    public LinkedHashSet<SimpleLocation> getPipeBlocks() {
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
     * Get the type of this pipe
     *
     * @return The type of the blocks this pipe consists of
     */
    public Material getType() {
        return type;
    }

    /**
     * displays particles around a pipe
     * @param players The player to show the pipe to, none to show it to everyone
     */
    public void highlight(Player... players) {
        LinkedHashSet<SimpleLocation> locations = new LinkedHashSet<>(pipeBlocks);
        inputs.stream().map(PipeInput::getLocation).forEach(locations::add);
        outputs.stream().map(PipeOutput::getLocation).forEach(locations::add);
        outputs.stream().map(PipeOutput::getTargetLocation).forEach(locations::add);

        for (SimpleLocation simpleLocation : locations) {
            Location location = simpleLocation.getLocation();
            location.setX(location.getX() + 0.5);
            location.setY(location.getY() + 0.5);
            location.setZ(location.getZ() + 0.5);
            for (int i = 0; i < 3; i++) {
                if (players.length == 0) {
                    location.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, location, 1, 0, 0, 0, 0);
                } else {
                    for (Player p : players) {
                        p.spawnParticle(Particle.FIREWORKS_SPARK, location, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    /**
     * returns a string with some info in it
     *
     * @return a string with some info in it
     */
    public String getString() {
        return PipesConfig.getText("info.pipe.pipeData",
                String.valueOf(inputs.size()),
                String.valueOf(outputs.size()),
                String.valueOf(pipeBlocks.size()),
                String.valueOf(chunkLoaders.size()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pipe pipe = (Pipe) o;

        return inputs.equals(pipe.inputs)
                && outputs.equals(pipe.outputs)
                && chunkLoaders.equals(pipe.chunkLoaders)
                && pipeBlocks.equals(pipe.pipeBlocks);

    }

    @Override
    public int hashCode() {
        int result = inputs.hashCode();
        result = 31 * result + outputs.hashCode();
        result = 31 * result + chunkLoaders.hashCode();
        result = 31 * result + pipeBlocks.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    public void checkLoaded(SimpleLocation startLocation) throws ChunkNotLoadedException {
        World world = Bukkit.getWorld(startLocation.getWorldName());
        if (world == null) {
            return;
        }

        Multimap<Integer, Integer> chunks = MultimapBuilder.hashKeys().hashSetValues().build();
        LinkedHashSet<SimpleLocation> locations = new LinkedHashSet<>(pipeBlocks);
        inputs.stream().map(PipeInput::getLocation).forEach(locations::add);
        outputs.stream().map(PipeOutput::getLocation).forEach(locations::add);
        outputs.stream().map(PipeOutput::getTargetLocation).forEach(locations::add);
        for (SimpleLocation location : locations) {
            chunks.put(location.getX() >> 4, location.getZ() >> 4);
        }
        for (Map.Entry<Integer, Integer> chunk : chunks.entries()) {
            if (!world.isChunkLoaded(chunk.getKey(), chunk.getValue())) {
                throw new ChunkNotLoadedException(startLocation);
            }
        }
    }
}
