package io.github.apfelcreme.Pipes.Manager;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.InputOutputLocationManager;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

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
public class PipeManager {

    /**
     * the DetectionManager instance
     */
    private static PipeManager instance = null;

    /**
     * a cache to stop endless pipe checks
     */
    private Map<SimpleLocation, Pipe> pipeCache = new HashMap<>();

    /**
     * constructor
     */
    private PipeManager() {
        pipeCache = new HashMap<>();
    }

    /**
     * returns the pipe cache
     * @return the pipe cache
     */
    public Map<SimpleLocation, Pipe> getPipeCache() {
        return pipeCache;
    }

    /**
     * adds a pipe to the pipe cache
     * @param dispenserLocation the location
     * @param pipe the pipe object
     */
    public void addPipeToCache(SimpleLocation dispenserLocation, Pipe pipe) {
        if (pipe != null) {
            pipeCache.put(dispenserLocation, pipe);
        }
    }
    /**
     * removes a pipe from the pipe cache
     * @param dispenserLocation the location
     */
    public void removeFromPipeCache(SimpleLocation dispenserLocation) {
        pipeCache.remove(dispenserLocation);
    }

    /**
     * returns the PipeManager instance
     *
     * @return the PipeManager instance
     */
    public static PipeManager getInstance() {
        if (instance == null) {
            instance = new PipeManager();
        }
        return instance;
    }

    /**
     * checks if the block is part of a pipe.
     *
     * @param startingPoint a block
     * @return a pipe, if there is one
     */
    public static Pipe isPipe(Block startingPoint) throws ChunkNotLoadedException {

        Queue<SimpleLocation> queue = new LinkedList<>();
        List<Block> found = new ArrayList<>();

        List<PipeInput> inputs = new ArrayList<>();
        List<PipeOutput> outputs = new ArrayList<>();
        List<SimpleLocation> pipeBlocks = new ArrayList<>();

        Byte color = null;

        World world = startingPoint.getWorld();

        queue.add(new SimpleLocation(
                startingPoint.getWorld().getName(),
                startingPoint.getX(),
                startingPoint.getY(),
                startingPoint.getZ()));

        while (!queue.isEmpty()) {
            SimpleLocation location = queue.remove();
            if (!world.isChunkLoaded(location.getX() >> 4, location.getZ() >> 4)) {
                throw new ChunkNotLoadedException(location);
            }
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!found.contains(block)) {
                if (block.getType() == Material.STAINED_GLASS) {
                    if (color == null) {
                        color = block.getData();
                    }
                    pipeBlocks.add(location);
                    found.add(block);
                    queue.add(location.getRelative(BlockFace.NORTH));
                    queue.add(location.getRelative(BlockFace.EAST));
                    queue.add(location.getRelative(BlockFace.SOUTH));
                    queue.add(location.getRelative(BlockFace.WEST));
                    queue.add(location.getRelative(BlockFace.UP));
                    queue.add(location.getRelative(BlockFace.DOWN));
                } else if (block.getState() instanceof InventoryHolder) {
                    if (block.getType() == Material.DROPPER) {
                        Dropper dropper = (Dropper) block.getState();
                        if (block.getRelative(PipesUtil.getDropperFace(dropper)).getState() instanceof InventoryHolder) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                outputs.add(new PipeOutput(location,
                                        location.getRelative(PipesUtil.getDropperFace(dropper))));
                                found.add(block);
                                found.add(block.getRelative(PipesUtil.getDropperFace(dropper)));
                            }
                        }
                    } else if (block.getState() instanceof Dispenser) {
                        Dispenser dispenser = (Dispenser) block.getState();
                        if (block.getRelative(PipesUtil.getDispenserFace(dispenser)).getType() == Material.STAINED_GLASS) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                inputs.add(new PipeInput(location));
                                found.add(block);
                                queue.add(location.getRelative(PipesUtil.getDispenserFace(dispenser)));
                            }
                        }
                    }
                }
            }
        }
        if ((outputs.size() > 0) && (inputs.size() > 0) && pipeBlocks.size() > 0) {
            return new Pipe(inputs, outputs, pipeBlocks);
        }
        return null;
    }
}
