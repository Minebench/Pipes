package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Listener.BlockPlaceListener;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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

    private Map<Player, BukkitTask> registeredRightClicks;

    @Override
    public void onEnable() {
        registeredRightClicks = new HashMap<>();
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(), this);
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
     * @param color         the color of the pipe
     * @return a pipe, if there is one
     */
    public static Pipe isPipe(Block startingPoint, byte color) {
        // as i do not really trust recursive functions with a basically unlimited world, i use much safer queues ;)
        Queue<Block> queue = new LinkedList<>();
        List<Block> found = new ArrayList<>();

        List<Dispenser> inputs = new ArrayList<>();
        List<Chest> randomOutputs = new ArrayList<>();
        Map<Dropper, Chest> sortedOutputs = new HashMap<>();

        // add a starting point
        queue.add(startingPoint);

        while (!queue.isEmpty()) {
            Block block = queue.remove();
            if (!found.contains(block)) {
                if (block.getState() instanceof Chest && !sortedOutputs.values().contains(block.getState())) {
                    randomOutputs.add((Chest) block.getState());
                } else if (block.getState() instanceof Dispenser) {
                    inputs.add((Dispenser) block.getState());
                } else if (block.getState() instanceof Dropper) {
                    Dropper dropper = (Dropper) block.getState();
                    if (block.getRelative(getDropperFace(dropper)).getState() instanceof Chest) {
                        sortedOutputs.put(dropper, (Chest) block.getRelative(getDropperFace(dropper)).getState());
                    }
                }
                if ((block.getType() == Material.DISPENSER)
                        || (block.getType() == Material.DROPPER)
                        || (block.getType() == Material.CHEST)
                        || ((block.getType() == Material.STAINED_GLASS) && (block.getData() == color))) {
                    found.add(block);

                    queue.add(block.getRelative(BlockFace.NORTH));
                    queue.add(block.getRelative(BlockFace.EAST));
                    queue.add(block.getRelative(BlockFace.SOUTH));
                    queue.add(block.getRelative(BlockFace.WEST));
                    queue.add(block.getRelative(BlockFace.UP));
                    queue.add(block.getRelative(BlockFace.DOWN));
                }
            }
        }
        if (((randomOutputs.size() > 0) || (sortedOutputs.size() > 0))
                && (inputs.size() > 0)) {
            return new Pipe(inputs, randomOutputs, sortedOutputs, found);
        }
        return null;
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
}
