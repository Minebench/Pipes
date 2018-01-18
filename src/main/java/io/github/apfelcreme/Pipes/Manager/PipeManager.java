package io.github.apfelcreme.Pipes.Manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Exception.PipeTooLongException;
import io.github.apfelcreme.Pipes.Exception.TooManyOutputsException;
import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
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
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
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
     * a cache to stop endless pipe checks, this is for parts that can be attached to only one pipe (glass pipe blocks)
     */
    private final Map<SimpleLocation, Pipe> singleCache;

    /**
     * a cache to stop endless pipe checks, this is for parts that can be attached to multiple pipes (outputs and chunk loader)
     */
    private final Map<SimpleLocation, Set<Pipe>> multiCache;

    /**
     * A cache for pipe parts
     */
    private final Map<SimpleLocation, AbstractPipePart> pipePartCache;

    /**
     * constructor
     */
    private PipeManager() {
        pipeCache = CacheBuilder.newBuilder()
                .maximumSize(PipesConfig.getPipeCacheSize())
                .expireAfterWrite(PipesConfig.getPipeCacheDuration(), TimeUnit.SECONDS)
                .removalListener(new PipeRemovalListener())
                .build();
        singleCache = new HashMap<>();
        multiCache = new HashMap<>();
        pipePartCache = new HashMap<>();
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
     * returns the cache for blocks that can only belong to a single pipe (and aren't inputs)
     *
     * @return the single cache
     */
    public Map<SimpleLocation, Pipe> getSingleCache() {
        return singleCache;
    }

    /**
     * returns the cache for blocks that can belong to multiple pipes (outputs and chunk loaders)
     *
     * @return the multi cache
     */
    public Map<SimpleLocation, Set<Pipe>> getMultiCache() {
        return multiCache;
    }

    /**
     * returns the cache for pipe parts
     *
     * @return the pipe part cache
     */
    public Map<SimpleLocation, AbstractPipePart> getPipePartCache() {
        return pipePartCache;
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
     * Get the pipe by an input at a location. This will only lookup in the input cache and no other one.
     * If none is found it will try to calculate the pipe that starts at that position
     *
     * @param location the location the input is at
     * @return a Pipe or <tt>null</tt>
     */
    public Pipe getPipeByInput(SimpleLocation location) {
        Pipe pipe = pipeCache.getIfPresent(location);
        if (pipe == null) {
            Block block = location.getBlock();

            if (PipesUtil.getPipesItem(block) != PipesItem.PIPE_INPUT) {
                return null;
            }

            try {
                pipe = isPipe(block);
            } catch (ChunkNotLoadedException | TooManyOutputsException | PipeTooLongException ignored) {}

            if (pipe != null) {
                addPipe(pipe);
            }
        }
        return pipe;
    }

    /**
     * Get the pipe that is at that location, returns an empty set instead of throwing an exception
     *
     * @param location The location
     * @return the pipes; an empty set if none were found or an error occurred
     */
    public Set<Pipe> getPipesSafe(SimpleLocation location) {
        return getPipesSafe(location, false);
    }

    /**
     * Get the pipe that is at that location, returns an empty set instead of throwing an exception
     *
     * @param location The location
     * @param cacheOnly Only look in the cache, don't search for new ones
     * @return the pipes; an empty set if none were found or an error occurred
     */
    public Set<Pipe> getPipesSafe(SimpleLocation location, boolean cacheOnly) {
        if (cacheOnly) {
            Pipe pipe = pipeCache.getIfPresent(location);
            if (pipe == null) {
                pipe = singleCache.get(location);
            }
            if (pipe != null) {
                return Collections.singleton(pipe);
            }
            return multiCache.getOrDefault(location, Collections.emptySet());
        }
        try {
            return getPipes(location.getBlock(), false);
        } catch (ChunkNotLoadedException | PipeTooLongException | TooManyOutputsException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get the pipe, returns an empty set instead of throwing an exception
     *
     * @param block the block to get the pipe for
     * @return the pipes; an empty set if none were found or an error occurred
     */
    public Set<Pipe> getPipesSafe(Block block) {
        try {
            return getPipes(block);
        } catch (ChunkNotLoadedException | PipeTooLongException | TooManyOutputsException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get the pipe, returns an empty set instead of throwing an exception
     *
     * @param block the block to get the pipe for
     * @param cacheOnly Only look in the cache, don't search for new ones
     * @return the pipes; an empty set if none were found or an error occurred
     */
    public Set<Pipe> getPipesSafe(Block block, boolean cacheOnly) {
        try {
            return getPipes(block, cacheOnly);
        } catch (ChunkNotLoadedException | PipeTooLongException | TooManyOutputsException e) {
            return Collections.emptySet();
        }
    }

    /**
     * Get the pipe for a block
     *
     * @param block The block
     * @return the pipes; an empty set if none were found
     * @throws ChunkNotLoadedException When the pipe reaches into a chunk that is not loaded
     * @throws PipeTooLongException When the pipe is too long
     * @throws TooManyOutputsException when the pipe has too many outputs
     */
    public Set<Pipe> getPipes(Block block) throws ChunkNotLoadedException, PipeTooLongException, TooManyOutputsException {
        return getPipes(block, false);
    }

    /**
     * Get the pipe for a block
     *
     * @param block The block
     * @param cacheOnly Only look in the cache, don't search for new ones
     * @return the pipes; an empty set if none were found
     * @throws ChunkNotLoadedException When the pipe reaches into a chunk that is not loaded
     * @throws PipeTooLongException When the pipe is too long
     * @throws TooManyOutputsException when the pipe has too many outputs
     */
    public Set<Pipe> getPipes(Block block, boolean cacheOnly) throws ChunkNotLoadedException, PipeTooLongException, TooManyOutputsException {
        if (block == null) {
            return Collections.emptySet();
        }
        Set<Pipe> pipes = getPipesSafe(new SimpleLocation(block.getLocation()), true);
        if (pipes.isEmpty() && !cacheOnly) {
            Pipe pipe = isPipe(block);
            if (pipe != null) {
                addPipe(pipe);
                return Collections.singleton(pipe);
            }
        }
        return pipes;
    }

    public void removePipe(Pipe pipe) {
        if (pipe == null) {
            return;
        }

        for (Iterator<PipeInput> i = pipe.getInputs().iterator(); i.hasNext();) {
            PipeInput input = i.next();
            i.remove();
            pipeCache.invalidate(input.getLocation());
        }
    }

    /**
     * Add all the pipes locations to the cache
     * @param pipe The pipe
     */
    private void addPipe(Pipe pipe) {
        if (pipe == null) {
            return;
        }
        for (PipeInput input : pipe.getInputs()) {
            pipeCache.put(input.getLocation(), pipe);
            pipePartCache.put(input.getLocation(), input);
        }
        for (SimpleLocation location : pipe.getPipeBlocks()) {
            singleCache.put(location, pipe);
        }
        for (PipeOutput output : pipe.getOutputs()) {
            addToMultiCache(output.getLocation(), pipe);
            pipePartCache.put(output.getLocation(), output);
        }
        for (ChunkLoader chunkLoader : pipe.getChunkLoaders()) {
            addToMultiCache(chunkLoader.getLocation(), pipe);
            pipePartCache.put(chunkLoader.getLocation(), chunkLoader);
        }
    }

    /**
     * Add a part to a pipe while checking settings and caching the location
     *
     * @param pipe the pipe to add to
     * @param pipePart the part to add
     * @throws TooManyOutputsException when the pipe has too many outputs
     */
    public void addPart(Pipe pipe, AbstractPipePart pipePart) throws TooManyOutputsException {
        if (pipePart instanceof PipeInput) {
            pipe.getInputs().add((PipeInput) pipePart);
            for (PipeInput input : pipe.getInputs()) {
                pipeCache.put(input.getLocation(), pipe);
            }
        } else if (pipePart instanceof PipeOutput) {
            if (PipesConfig.getMaxPipeOutputs() > 0 && pipe.getOutputs().size() + 1 >= PipesConfig.getMaxPipeOutputs()) {
                removePipe(pipe);
                throw new TooManyOutputsException(pipePart.getLocation());
            }
            pipe.getOutputs().add((PipeOutput) pipePart);
            addToMultiCache(pipePart.getLocation(), pipe);
        } else if (pipePart instanceof ChunkLoader) {
            pipe.getChunkLoaders().add((ChunkLoader) pipePart);
            addToMultiCache(pipePart.getLocation(), pipe);
        }
        pipePartCache.put(pipePart.getLocation(), pipePart);
    }

    /**
     * Remove a part from a pipe
     *
     * @param pipe the pipe to remove from
     * @param pipePart the part to remove
     */
    public void removePart(Pipe pipe, AbstractPipePart pipePart) {
        if (pipePart instanceof PipeInput) {
            pipe.getInputs().remove(pipePart);
            pipeCache.invalidate(pipePart.getLocation());
        } else if (pipePart instanceof PipeOutput) {
            pipe.getOutputs().remove(pipePart);
            if (pipe.getOutputs().isEmpty()) {
                removePipe(pipe);
            } else {
                removeFromMultiCache(pipePart.getLocation(), pipe);
            }
        } else if (pipePart instanceof ChunkLoader) {
            pipe.getChunkLoaders().remove(pipePart);
            removeFromMultiCache(pipePart.getLocation(), pipe);
        }
        pipePartCache.remove(pipePart.getLocation(), pipePart);
    }

    private void addToMultiCache(SimpleLocation location, Pipe pipe) {
        multiCache.putIfAbsent(location, Collections.newSetFromMap(new WeakHashMap<>()));
        multiCache.get(location).add(pipe);
    }

    private void removeFromMultiCache(SimpleLocation location, Pipe pipe) {
        Collection<Pipe> pipes = multiCache.get(location);
        if (pipes != null) {
            if (pipes.size() == 1) {
                multiCache.remove(location);
            } else {
                pipes.remove(pipe);
            }
        }
    }

    /**
     * Add a block to a pipe while checking settings and caching the location
     *
     * @param pipe the pipe to add to
     * @param block the block to add
     * @throws PipeTooLongException When the pipe is too long
     */
    public void addBlock(Pipe pipe, Block block) throws PipeTooLongException {
        SimpleLocation location = new SimpleLocation(block.getLocation());
        if (PipesConfig.getMaxPipeLength() > 0 && pipe.getPipeBlocks().size() + 1 >= PipesConfig.getMaxPipeLength()) {
            removePipe(pipe);
            throw new PipeTooLongException(location);
        }
        pipe.getPipeBlocks().add(location);
        singleCache.put(location, pipe);
    }

    /**
     * Merge multiple pipes into one
     * @param pipes The pipes to merge
     * @return the merged Pipe or <tt>null</tt> if they couldn't be merged
     */
    public Pipe mergePipes(Set<Pipe> pipes) throws TooManyOutputsException, PipeTooLongException {
        DyeColor color = null;
        for (Pipe pipe : pipes) {
            if (color == null) {
                color = pipe.getColor();
            }
            if (!pipe.getColor().equals(color)) {
                return null;
            }
        }

        LinkedHashSet<PipeInput> inputs = new LinkedHashSet<>();
        LinkedHashSet<PipeOutput> outputs = new LinkedHashSet<>();
        LinkedHashSet<ChunkLoader> chunkLoaders = new LinkedHashSet<>();
        LinkedHashSet<SimpleLocation> blocks = new LinkedHashSet<>();

        pipes.forEach(pipe -> {
            removePipe(pipe);
            inputs.addAll(pipe.getInputs());
            outputs.addAll(pipe.getOutputs());
            chunkLoaders.addAll(pipe.getChunkLoaders());
            blocks.addAll(pipe.getPipeBlocks());
        });

        if (PipesConfig.getMaxPipeLength() > 0 &&blocks.size() >= PipesConfig.getMaxPipeLength()) {
            throw new PipeTooLongException(blocks.iterator().next());
        }

        if (PipesConfig.getMaxPipeOutputs() > 0 && outputs.size() + 1 >= PipesConfig.getMaxPipeOutputs()) {
            throw new TooManyOutputsException(outputs.iterator().next().getLocation());
        }

        Pipe pipe = new Pipe(inputs, outputs, chunkLoaders, blocks, color);

        addPipe(pipe);
        return pipe;
    }

    /**
     * checks if the block is part of a pipe.
     *
     * @param startingPoint a block
     * @return a pipe, if there is one
     */
    public Pipe isPipe(Block startingPoint) throws ChunkNotLoadedException, TooManyOutputsException, PipeTooLongException {

        Queue<SimpleLocation> queue = new LinkedList<>();
        List<Block> found = new ArrayList<>();

        LinkedHashSet<PipeInput> inputs = new LinkedHashSet<>();
        LinkedHashSet<PipeOutput> outputs = new LinkedHashSet<>();
        LinkedHashSet<ChunkLoader> chunkLoaders = new LinkedHashSet<>();
        LinkedHashSet<SimpleLocation> pipeBlocks = new LinkedHashSet<>();

        byte color = -1;

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
                    byte blockColor = block.getData();
                    if (color == -1) {
                        color = blockColor;
                    }
                    if (blockColor == color) {
                        if (PipesConfig.getMaxPipeLength() > 0 && pipeBlocks.size() >= PipesConfig.getMaxPipeLength()) {
                            throw new PipeTooLongException(location);
                        }
                        pipeBlocks.add(location);
                        found.add(block);
                        for (BlockFace face : PipesUtil.BLOCK_FACES) {
                            queue.add(location.getRelative(face));
                        }
                    }
                } else {
                    AbstractPipePart pipesPart = getPipePart(block);
                    if (pipesPart != null) {
                        switch (pipesPart.getType()) {
                            case PIPE_INPUT:
                                PipeInput pipeInput = (PipeInput) pipesPart;
                                Block relativeBlock = block.getRelative(pipeInput.getFacing());
                                if (relativeBlock.getType() == Material.STAINED_GLASS
                                        && (color == -1 || relativeBlock.getData() == color)) {
                                    inputs.add(pipeInput);
                                    found.add(block);
                                    queue.add(pipeInput.getTargetLocation());
                                }
                                break;
                            case PIPE_OUTPUT:
                                PipeOutput pipeOutput = (PipeOutput) pipesPart;
                                if (PipesConfig.getMaxPipeOutputs() > 0 && outputs.size() >= PipesConfig.getMaxPipeOutputs()) {
                                    throw new TooManyOutputsException(location);
                                }
                                outputs.add(pipeOutput);
                                found.add(block);
                                Block relativeToOutput = pipeOutput.getTargetLocation().getBlock();
                                if (relativeToOutput.getState(false) instanceof InventoryHolder) {
                                    found.add(relativeToOutput);
                                }
                                break;
                            case CHUNK_LOADER:
                                chunkLoaders.add((ChunkLoader) pipesPart);
                                found.add(block);
                                break;
                        }
                    }
                }
            }
        }
        if ((outputs.size() > 0) && (inputs.size() > 0) && pipeBlocks.size() > 0) {
            return new Pipe(inputs, outputs, chunkLoaders, pipeBlocks, DyeColor.getByWoolData(color));
        }
        return null;
    }

    /**
     * Get the pipes part. Will try to lookup the part in the cache first, if not found it will create a new one.
     * @param block the block to get the part for
     * @return the pipespart or
     */
    public AbstractPipePart getPipePart(Block block) {
        PipesItem type = PipesUtil.getPipesItem(block);
        if (type == null) {
            return null;
        }
        return pipePartCache.getOrDefault(
                new SimpleLocation(block.getLocation()),
                PipesUtil.convertToPipePart(block, type)
        );
    }

    /**
     * Get the pipes part. Will try to lookup the part in the cache first, if not found it will create a new one.
     * @param location the block to get the part for
     * @return the pipespart or
     */
    public AbstractPipePart getCachedPipePart(SimpleLocation location) {
        return pipePartCache.get(location);
    }

    /**
     * checks whether the given block state is a pipe input
     *
     * @param state a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(BlockState)}
     */
    @Deprecated
    public static boolean isPipeInput(BlockState state) {
        return PipesItem.PIPE_INPUT.check(state);
    }

    /**
     * checks whether the given block state is a pipe output
     *
     * @param holder a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(BlockState)}
     */
    @Deprecated
    public static boolean isPipeOutput(BlockState holder) {
        return PipesItem.PIPE_OUTPUT.check(holder);
    }

    /**
     * checks whether the given block state is a chunk loader
     *
     * @param holder a block state
     * @return true or false
     * @deprecated Use {@link PipesItem#check(BlockState)}
     */
    @Deprecated
    public static boolean isChunkLoader(BlockState holder) {
        return PipesItem.CHUNK_LOADER.check(holder);
    }

    /**
     * checks whether the given block location is a pipe input
     *
     * @param location a block location
     * @return true or false
     * @deprecated Use {@link PipesItem#check(BlockState)}
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
     * @deprecated Use {@link PipesItem#check(BlockState)}
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
     * @deprecated Use {@link PipesItem#check(BlockState)}
     */
    @Deprecated
    public static boolean isChunkLoader(SimpleLocation location) {
        return PipesItem.CHUNK_LOADER.check(location.getBlock());
    }

    private class PipeRemovalListener implements RemovalListener<SimpleLocation, Pipe> {
        @Override
        public void onRemoval(RemovalNotification<SimpleLocation, Pipe> notification) {
            Pipe pipe = notification.getValue();

            if (pipe == null) {
                return;
            }

            if (pipe.getInputs().isEmpty() || notification.getCause() != RemovalCause.EXPLICIT) {
                for (PipeInput input : pipe.getInputs()) {
                    pipeCache.invalidate(input.getLocation());
                    pipePartCache.remove(input.getLocation(), input);
                }
                for (SimpleLocation location : pipe.getPipeBlocks()) {
                    singleCache.remove(location);
                }
                for (PipeOutput output : pipe.getOutputs()) {
                    removeFromMultiCache(output.getLocation(), pipe);
                    if (multiCache.getOrDefault(output.getLocation(), Collections.emptySet()).isEmpty()) {
                        pipePartCache.remove(output.getLocation(), output);
                    }
                }
                for (ChunkLoader loader : pipe.getChunkLoaders()) {
                    removeFromMultiCache(loader.getLocation(), pipe);
                    if (multiCache.getOrDefault(loader.getLocation(), Collections.emptySet()).isEmpty()) {
                        pipePartCache.remove(loader.getLocation(), loader);
                    }
                }
            }
        }
    }
}
