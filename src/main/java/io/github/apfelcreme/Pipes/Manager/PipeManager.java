package io.github.apfelcreme.Pipes.Manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Pipe.ChunkLoader;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
    private final LoadingCache<SimpleLocation, Pipe> pipeCache;

    /**
     * constructor
     */
    private PipeManager() {
        pipeCache = CacheBuilder.newBuilder().expireAfterWrite(PipesConfig.getPipeCacheDuration(), TimeUnit.MILLISECONDS).build(new CacheLoader<SimpleLocation, Pipe>() {
            @Override
            public Pipe load(SimpleLocation location) throws Exception {
                Pipe pipe = isPipe(location.getBlock());
                if (pipe == null) {
                    throw new ExecutionException(new Exception("No pipe found!"));
                }
                return pipe;
            }
        });
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

    public Pipe getPipe(SimpleLocation location) {
        try {
            return pipeCache.get(location);
        } catch (ExecutionException e) {
            Pipes.getInstance().getLogger().log(Level.WARNING, "Error while trying to get a pipe from the cache.", e);
            return null;
        }
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
                    DyeColor blockColor = DyeColor.getByWoolData(block.getState().getRawData());
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
     * checks whether the given block state is a pipe input
     *
     * @param state a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeInput(BlockState state) {
        return PipesItem.PIPE_INPUT.check(state.getBlock());
    }

    /**
     * checks whether the given block state is a pipe output
     *
     * @param holder a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isPipeOutput(BlockState holder) {
        return PipesItem.PIPE_OUTPUT.check(holder.getBlock());
    }

    /**
     * checks whether the given block state is a chunk loader
     *
     * @param holder a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(Block)}
     */
    @Deprecated
    public static boolean isChunkLoader(BlockState holder) {
        return PipesItem.CHUNK_LOADER.check(holder.getBlock());
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
        return PipesItem.PIPE_INPUT.check(location.getBlock());
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
        return PipesItem.PIPE_OUTPUT.check(location.getBlock());
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
        return PipesItem.CHUNK_LOADER.check(location.getBlock());
    }
}
