package io.github.apfelcreme.Pipes.Manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Pipe.ChunkLoader;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Colorable;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

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
    private final Cache<SimpleLocation, Pipe> pipeCache;

    /**
     * constructor
     */
    private PipeManager() {
        pipeCache = CacheBuilder.newBuilder().expireAfterWrite(PipesConfig.getPipeCacheDuration(), TimeUnit.MILLISECONDS).build();
    }

    /**
     * returns the pipe cache
     *
     * @return the pipe cache
     */
    public Cache<SimpleLocation, Pipe> getPipeCache() {
        return pipeCache;
    }

    /**
     * adds a pipe to the pipe cache
     *
     * @param dispenserLocation the location
     * @param pipe              the pipe object
     */
    public void addPipeToCache(SimpleLocation dispenserLocation, Pipe pipe) {
        if (pipe != null) {
            pipeCache.put(dispenserLocation, pipe);
        }
    }

    /**
     * removes a pipe from the pipe cache
     *
     * @param dispenserLocation the location
     */
    public void removeFromPipeCache(SimpleLocation dispenserLocation) {
        pipeCache.invalidate(dispenserLocation);
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
        List<ChunkLoader> chunkLoaders = new ArrayList<>();
        List<SimpleLocation> pipeBlocks = new ArrayList<>();

        DyeColor color = null;

        World world = startingPoint.getWorld();

        queue.add(new SimpleLocation(
                startingPoint.getWorld().getName(),
                startingPoint.getX(),
                startingPoint.getY(),
                startingPoint.getZ()));

        while (!queue.isEmpty()) {
            SimpleLocation location = queue.remove();
            if (!world.isChunkLoaded(location.getX() >> 4, location.getZ() >> 4)
                    && (chunkLoaders.size() == 0)) {
                throw new ChunkNotLoadedException(location);
            }
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!found.contains(block)) {
                if (block.getType() == Material.STAINED_GLASS) {
                    DyeColor blockColor = ((Colorable) block.getState().getData()).getColor();
                    if (color == null) {
                        color = blockColor;
                    }
                    if (color == blockColor) {
                        pipeBlocks.add(location);
                        found.add(block);
                        queue.add(location.getRelative(BlockFace.NORTH));
                        queue.add(location.getRelative(BlockFace.EAST));
                        queue.add(location.getRelative(BlockFace.SOUTH));
                        queue.add(location.getRelative(BlockFace.WEST));
                        queue.add(location.getRelative(BlockFace.UP));
                        queue.add(location.getRelative(BlockFace.DOWN));
                    }
                } else {
                    PipesItem pipesItem = PipesUtil.getPipesItem(block);
                    if (pipesItem != null) {
                        switch (pipesItem) {
                            case PIPE_INPUT:
                                Block relativeToInput = block.getRelative(((Directional) block.getState().getData()).getFacing());
                                if (relativeToInput.getType() == Material.STAINED_GLASS) {
                                    inputs.add(new PipeInput(location));
                                    found.add(block);
                                    queue.add(new SimpleLocation(relativeToInput.getLocation()));
                                }
                                break;
                            case PIPE_OUTPUT:
                                Block relativeToOutput = block.getRelative(((Directional) block.getState().getData()).getFacing());
                                if (relativeToOutput.getState() instanceof InventoryHolder) {
                                    outputs.add(new PipeOutput(location, new SimpleLocation(relativeToOutput.getLocation())));
                                    found.add(block);
                                    found.add(relativeToOutput);
                                }
                                break;
                            case CHUNK_LOADER:
                                chunkLoaders.add(new ChunkLoader(location));
                                found.add(block);
                                break;
                        }
                    }
                }
            }
        }
        if ((outputs.size() > 0) && (inputs.size() > 0) && pipeBlocks.size() > 0) {
            return new Pipe(inputs, outputs, chunkLoaders, pipeBlocks, color);
        }
        return null;
    }

    /**
     * checks whether the given dispenser block is a pipe input
     *
     * @param dispenser a dispenser block
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeInput(Dispenser dispenser) {
        return dispenser.getInventory().getName().equals(PipesUtil.getCustomDispenserItem().getItemMeta().getDisplayName());
    }

    /**
     * checks whether the given dropper block is a pipe output
     *
     * @param dropper a dropper block
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeOutput(Dropper dropper) {
        return dropper.getInventory().getName().equals(PipesUtil.getCustomDropperItem().getItemMeta().getDisplayName());
    }

    /**
     * checks whether the given furnace block is a chunk loader
     *
     * @param furnace a furnace block
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isChunkLoader(Furnace furnace) {
        return furnace.getInventory().getName().equals(PipesUtil.getCustomChunkLoaderItem().getItemMeta().getDisplayName());
    }

    /**
     * checks whether the given block location is a pipe input
     *
     * @param location a block location
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeInput(SimpleLocation location) {
//        System.out.println(location.getBlock().getState() instanceof Dispenser);
//        if (location.getBlock().getState() instanceof Dispenser)
//            System.out.println(isPipeOutput((Dropper) location.getBlock().getState()));
        return location.getBlock().getState() instanceof Dispenser
                && isPipeInput((Dispenser) location.getBlock().getState());
    }

    /**
     * checks whether the given block location is a pipe output
     *
     * @param location a block location
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeOutput(SimpleLocation location) {
//        System.out.println(location.getBlock().getState() instanceof Dropper);
//        if (location.getBlock().getState() instanceof Dropper)
//          System.out.println(isPipeOutput((Dropper) location.getBlock().getState()));
        return location.getBlock().getState() instanceof Dropper
                && isPipeOutput((Dropper) location.getBlock().getState());
    }

    /**
     * checks whether the given block location is a chunk loader
     *
     * @param location a block location
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isChunkLoader(SimpleLocation location) {
        return location.getBlock().getState() instanceof Furnace
                && isChunkLoader((Furnace) location.getBlock().getState());
    }
}
