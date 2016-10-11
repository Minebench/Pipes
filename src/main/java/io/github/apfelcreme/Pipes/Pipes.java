package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Exception.LoopException;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

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
public class Pipes extends JavaPlugin {

    /**
     * the players who have registered a right click for /pipe info
     */
    private Map<Player, BukkitTask> registeredRightClicks;

    @Override
    public void onEnable() {
        registeredRightClicks = new HashMap<>();
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRightclickListener(), this);
        getServer().getPluginCommand("pipe").setExecutor(new PipeCommand());
        PipesConfig.load();

    }

    /**
     * returns the registered rightclick map for /pipe info
     *
     * @return a map of registered rightclicks
     */
    public Map<Player, BukkitTask> getRegisteredRightClicks() {
        return registeredRightClicks;
    }

    /**
     * sends a message to a player
     *
     * @param player  the player the message shall be sent to
     * @param message the message
     */
    public static void sendMessage(CommandSender player, String message) {
        player.sendMessage(PipesConfig.getText("prefix") + message);
    }

    /**
     * returns the plugin instance
     *
     * @return the plugin instance
     */
    public static Pipes getInstance() {
        return (Pipes) Bukkit.getServer().getPluginManager()
                .getPlugin("Pipes");
    }

    /**
     * checks if the block is part of a pipe.
     *
     * @param startingPoint a block
     * @return a pipe, if there is one
     */
    public static Pipe isPipe(Block startingPoint) {

        Queue<Block> queue = new LinkedList<>();
        List<Block> found = new ArrayList<>();

        List<PipeInput> inputs = new ArrayList<>();
        List<PipeOutput> outputs = new ArrayList<>();
        List<Block> pipeBlocks = new ArrayList<>();

        Byte color = null;

        queue.add(startingPoint);

        while (!queue.isEmpty()) {
            Block block = queue.remove();
            if (!found.contains(block)) {
                if (block.getType() == Material.STAINED_GLASS) {
                    if (color == null) {
                        color = block.getData();
                    }
                    pipeBlocks.add(block);
                    found.add(block);
                    queue.add(block.getRelative(BlockFace.NORTH));
                    queue.add(block.getRelative(BlockFace.EAST));
                    queue.add(block.getRelative(BlockFace.SOUTH));
                    queue.add(block.getRelative(BlockFace.WEST));
                    queue.add(block.getRelative(BlockFace.UP));
                    queue.add(block.getRelative(BlockFace.DOWN));
                } else if (block.getState() instanceof InventoryHolder) {
                    if (block.getType() == Material.DROPPER) {
                        if (block.getRelative(getDropperFace((Dropper) block.getState())).getState() instanceof InventoryHolder) {
                            outputs.add(new PipeOutput((Dropper) block.getState(),
                                    (InventoryHolder) block.getRelative(getDropperFace((Dropper) block.getState())).getState()));
                            found.add(block);
                            found.add(block.getRelative(getDropperFace((Dropper) block.getState())));
                        }
                    } else if (block.getState() instanceof Dispenser) {
                        if (block.getRelative(getDispenserFace((Dispenser) block.getState())).getType() == Material.STAINED_GLASS) {
                            inputs.add(new PipeInput((Dispenser) block.getState()));
                            found.add(block);
                            queue.add(block.getRelative(getDispenserFace((Dispenser)block.getState())));
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


    /**
     * displays particles around a pipe
     *
     * @param pipe the pipe
     */
    public void highlightPipe(Pipe pipe) {
        List<Block> blocks = new ArrayList<>();
        for (Block block : pipe.getPipeBlocks()) {
            blocks.add(block);
        }
        for (PipeInput input : pipe.getInputs()) {
            blocks.add(input.getDispenser().getBlock());
        }
        for (PipeOutput output : pipe.getOutputs()) {
            blocks.add(output.getDropper().getBlock());
            blocks.add(output.getDropper().getBlock().getRelative(getDropperFace(output.getDropper())));
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
     * returns the direction a dropper is facing, as there is no way to get that information from the current API
     *
     * @param dropper the dropper block
     * @return the BlockFace the dropper is facing to
     */
    public static BlockFace getDropperFace(Dropper dropper) {
        byte data = dropper.getData().getData();
        if (data == 0) {
            return BlockFace.DOWN;
        } else if (data == 1) {
            return BlockFace.UP;
        } else if (data == 2) {
            return BlockFace.NORTH;
        } else if (data == 3) {
            return BlockFace.SOUTH;
        } else if (data == 4) {
            return BlockFace.WEST;
        } else if (data == 5) {
            return BlockFace.EAST;
        }
        return null;
    }

    /**
     * returns the direction a dispenser is facing
     *
     * @param dispenser the dispenser block
     * @return the BlockFace the dispenser is facing to
     */
    public static BlockFace getDispenserFace(Dispenser dispenser) {
        byte data = dispenser.getData().getData();
        if (data == 0) {
            return BlockFace.DOWN;
        } else if (data == 1) {
            return BlockFace.UP;
        } else if (data == 2) {
            return BlockFace.NORTH;
        } else if (data == 3) {
            return BlockFace.SOUTH;
        } else if (data == 4) {
            return BlockFace.WEST;
        } else if (data == 5) {
            return BlockFace.EAST;
        }
        return null;
    }
}
