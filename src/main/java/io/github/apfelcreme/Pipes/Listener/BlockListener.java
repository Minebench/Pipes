package io.github.apfelcreme.Pipes.Listener;

import com.destroystokyo.paper.MaterialTags;
import io.github.apfelcreme.Pipes.Event.PipeBlockBreakEvent;
import io.github.apfelcreme.Pipes.Event.PipeDispenseEvent;
import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Exception.LocationException;
import io.github.apfelcreme.Pipes.Exception.PipeTooLongException;
import io.github.apfelcreme.Pipes.Exception.TooManyOutputsException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
import io.github.apfelcreme.Pipes.Pipe.ChunkLoader;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
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
public class BlockListener implements Listener {

    private final Pipes plugin;

    public BlockListener(Pipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDispense(BlockDispenseEvent event) {
        if (!(event instanceof PipeDispenseEvent) && PipesUtil.getPipesItem(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMove(InventoryMoveItemEvent event) {
        if (event.getDestination().getType() != InventoryType.HOPPER // hoppers are allowed to remove items from the output
                && event.getSource().getType() != InventoryType.HOPPER) {
            InventoryHolder holder = event.getSource().getHolder(false);
            if (holder instanceof BlockState && PipesItem.PIPE_OUTPUT.check((BlockState) holder)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        AbstractPipePart pipePart = PipeManager.getInstance().getPipePart(event.getBlock());
        if (pipePart != null) {
            if (new PipeBlockBreakEvent(event.getBlock(), event.getPlayer(), pipePart).callEvent()) {
                Set<Pipe> pipes = PipeManager.getInstance().getPipesSafe(event.getBlock(), true);
                if (!pipes.isEmpty()) {
                    for (Pipe pipe : new ArrayList<>(pipes)) {
                        PipeManager.getInstance().removePart(pipe, pipePart);
                    }
                }
            } else {
                event.setCancelled(true);
            }
        } else if (MaterialTags.STAINED_GLASS.isTagged(event.getBlock())) {
            Set<Pipe> pipes = PipeManager.getInstance().getPipesSafe(event.getBlock(), true);
            if (!pipes.isEmpty()) {
                for (Pipe pipe : new ArrayList<>(pipes)) {
                    PipeManager.getInstance().removePipe(pipe);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreakDrop(BlockDropItemEvent event) {
        AbstractPipePart pipePart = PipeManager.getInstance().getPipePart(event.getBlockState());
        if (pipePart != null) {
            for (Item item : event.getItems()) {
                if (item.getItemStack().getType() == pipePart.getType().getMaterial()) {
                    item.setItemStack(pipePart.getType().toItemStack());
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        try {
            PipesItem pipesItem = PipesUtil.getPipesItem(event.getItemInHand());
            if (pipesItem != null) {
                if (pipesItem == PipesItem.CHUNK_LOADER && !event.getPlayer().hasPermission("Pipes.placeChunkLoader")) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "error.noPermission"));
                    event.setCancelled(true);
                    return;
                }
                AbstractPipePart pipePart = PipeManager.getInstance().createPipePart(pipesItem, event.getBlock());
                if (pipePart instanceof PipeInput) {
                    Block block = event.getBlock().getRelative(((PipeInput) pipePart).getFacing());
                    if (MaterialTags.STAINED_GLASS.isTagged(block)) {
                        Set<Pipe> pipes = PipeManager.getInstance().getPipesSafe(((PipeInput) pipePart).getTargetLocation(), true);
                        if (!pipes.isEmpty()) {
                            PipeManager.getInstance().addPart(pipes.iterator().next(), pipePart);
                        }
                    }
                } else if (pipePart instanceof PipeOutput) {
                    for (BlockFace face : PipesUtil.BLOCK_FACES) {
                        if (face != ((PipeOutput) pipePart).getFacing()) {
                            Block block = event.getBlock().getRelative(face);
                            if (MaterialTags.STAINED_GLASS.isTagged(block)) {
                                for (Pipe pipe : PipeManager.getInstance().getPipes(block, true)) {
                                    if (!pipe.getInputs().containsKey(((PipeOutput) pipePart).getTargetLocation())) {
                                        PipeManager.getInstance().addPart(pipe, pipePart);
                                    }
                                }
                            }
                        }
                    }
                } else if (pipePart instanceof ChunkLoader) {
                    for (BlockFace face : PipesUtil.BLOCK_FACES) {
                        for (Pipe pipe : PipeManager.getInstance().getPipesSafe(pipePart.getLocation().getRelative(face), true)) {
                            PipeManager.getInstance().addPart(pipe, pipePart);
                        }
                    }
                }

                for (Pipe pipe : PipeManager.getInstance().getPipes(event.getBlock())) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "info.pipe.pipeBuilt",
                            pipe.getString(event.getPlayer())));
                    pipe.highlight();
                }
            } else if (MaterialTags.STAINED_GLASS.isTagged(event.getBlock())) {
                Material placedType = event.getBlock().getType();

                Set<Pipe> found = new HashSet<>();
                for (BlockFace face : PipesUtil.BLOCK_FACES) {
                    Block block = event.getBlock().getRelative(face);
                    boolean isPotentialPipe = block.getType() == placedType;

                    if (!isPotentialPipe) {
                        AbstractPipePart potentialPart = PipeManager.getInstance().getPipePart(event.getBlock());
                        if (potentialPart != null) {
                            if (potentialPart instanceof PipeInput) {
                                isPotentialPipe = (((PipeInput) potentialPart).getFacing().getOppositeFace() == face);
                            } else if (potentialPart instanceof PipeOutput) {
                                isPotentialPipe = (((PipeOutput) potentialPart).getFacing().getOppositeFace() != face);
                            } else {
                                isPotentialPipe = true;
                            }
                        }
                    }

                    if (isPotentialPipe) {
                        Set<Pipe> pipes = PipeManager.getInstance().getPipesSafe(block, true);
                        if (!pipes.isEmpty()) {
                            Pipe pipe = pipes.iterator().next();
                            if (pipe.getType() == placedType) {
                                found.add(pipe);
                            }
                        }
                    }
                }

                if (found.size() == 1) {
                    PipeManager.getInstance().addBlock(found.iterator().next(), event.getBlock());
                } else if (found.size() > 1) {
                    PipeManager.getInstance().mergePipes(found);
                } else {
                    try {
                        for (Pipe pipe : PipeManager.getInstance().getPipes(event.getBlock())) {
                            Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "info.pipe.pipeBuilt",
                                    pipe.getString(event.getPlayer())));
                            pipe.highlight();
                        }
                    } catch (LocationException ignored) {}
                }
            }
        } catch (ChunkNotLoadedException e) {
            Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "error.chunkNotLoaded"));
        } catch (TooManyOutputsException e) {
            Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "error.tooManyOutputs",
                    String.valueOf(PipesConfig.getMaxPipeOutputs())));
        } catch (PipeTooLongException e) {
            Pipes.sendMessage(event.getPlayer(), PipesConfig.getText(event.getPlayer(), "error.pipeTooLong",
                    String.valueOf(PipesConfig.getMaxPipeLength())));
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePistonEvent(event.getDirection(), event.getBlocks());
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePistonEvent(event.getDirection(), event.getBlocks());
    }
    
    private void handlePistonEvent(BlockFace direction, List<Block> blocks) {
        if (!PipesConfig.isPistonCheckEnabled()) {
            return;
        }
        
        Set<Block> update = new LinkedHashSet<>();
        for (Block moved : blocks) {
            if (MaterialTags.STAINED_GLASS.isTagged(moved)) {
                update.add(moved);
                for (BlockFace face : PipesUtil.BLOCK_FACES) {
                    Block u = moved.getRelative(face);
                    if (!update.contains(u) && MaterialTags.STAINED_GLASS.isTagged(u) || PipeManager.getInstance().getPipePart(u) != null) {
                        update.add(u);
                    }
                    Block ud = u.getRelative(direction);
                    if (!update.contains(ud) && MaterialTags.STAINED_GLASS.isTagged(ud) || PipeManager.getInstance().getPipePart(ud) != null) {
                        update.add(ud);
                    }
                }
            }
        }
        for (Block u : update) {
            Set<Pipe> pipes = PipeManager.getInstance().getPipesSafe(u, true);
            if (!pipes.isEmpty()) {
                for (Pipe pipe : new ArrayList<>(pipes)) {
                    PipeManager.getInstance().removePipe(pipe);
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onItemCraft(CraftItemEvent event) {
        if (PipesUtil.getPipesItem(event.getCurrentItem()) == PipesItem.CHUNK_LOADER) {
            if (!event.getWhoClicked().hasPermission("Pipes.placeChunkLoader")) {
                event.setCancelled(true);
                Pipes.sendMessage(event.getWhoClicked(), PipesConfig.getText(event.getWhoClicked(), "error.noPermission"));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && PipesUtil.getPipesItem(event.getRecipe().getResult()) == PipesItem.CHUNK_LOADER) {
            if (event.getViewers().stream().anyMatch(p -> !p.hasPermission("Pipes.placeChunkLoader"))) {
                event.getInventory().setResult(null);
            }
        }
    }
}
