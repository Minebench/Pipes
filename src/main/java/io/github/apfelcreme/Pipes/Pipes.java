package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Listener.BlockListener;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
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

    /**
     * the plugin instance
     */
    private static Pipes instance = null;

    @Override
    public void onEnable() {
        instance = this;
        registeredRightClicks = new HashMap<>();
        PipesConfig.load();
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRightclickListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginCommand("pipe").setExecutor(new PipeCommand());

        //create the custom recipes
        ShapelessRecipe dispenserRecipe = new ShapelessRecipe(PipesUtil.getCustomDispenserItem());
        for (Map.Entry<Material, Integer> material : PipesConfig.getDispenserMaterials().entrySet()) {
            dispenserRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dispenserRecipe);

        ShapelessRecipe dropperRecipe = new ShapelessRecipe(PipesUtil.getCustomDropperItem());
        for (Map.Entry<Material, Integer> material : PipesConfig.getDropperMaterials().entrySet()) {
            dropperRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dropperRecipe);
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
        List<Block> pipeBlocks = new ArrayList<>();

        Byte color = null;

        World world = startingPoint.getWorld();

        queue.add(new SimpleLocation(
                startingPoint.getWorld().getName(),
                startingPoint.getX(),
                startingPoint.getY(),
                startingPoint.getZ()));

        while (!queue.isEmpty()) {
            SimpleLocation simpleLocation = queue.remove();
            if (!world.isChunkLoaded(simpleLocation.getX() >> 4, simpleLocation.getZ() >> 4)) {
                throw new ChunkNotLoadedException(simpleLocation);
            }
            Block block = world.getBlockAt(simpleLocation.getX(), simpleLocation.getY(), simpleLocation.getZ());
            if (!found.contains(block)) {
                if (block.getType() == Material.STAINED_GLASS) {
                    if (color == null) {
                        color = block.getData();
                    }
                    pipeBlocks.add(block);
                    found.add(block);
                    queue.add(simpleLocation.getRelative(BlockFace.NORTH));
                    queue.add(simpleLocation.getRelative(BlockFace.EAST));
                    queue.add(simpleLocation.getRelative(BlockFace.SOUTH));
                    queue.add(simpleLocation.getRelative(BlockFace.WEST));
                    queue.add(simpleLocation.getRelative(BlockFace.UP));
                    queue.add(simpleLocation.getRelative(BlockFace.DOWN));
                } else if (block.getState() instanceof InventoryHolder) {
                    if (block.getType() == Material.DROPPER) {
                        Dropper dropper = (Dropper) block.getState();
                        if (block.getRelative(PipesUtil.getDropperFace(dropper)).getState() instanceof InventoryHolder) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                outputs.add(new PipeOutput(dropper,
                                        (InventoryHolder) block.getRelative(PipesUtil.getDropperFace(dropper)).getState()));
                                found.add(block);
                                found.add(block.getRelative(PipesUtil.getDropperFace(dropper)));
                            }
                        }
                    } else if (block.getState() instanceof Dispenser) {
                        Dispenser dispenser = (Dispenser) block.getState();
                        if (block.getRelative(PipesUtil.getDispenserFace(dispenser)).getType() == Material.STAINED_GLASS) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                inputs.add(new PipeInput(dispenser));
                                found.add(block);
                                queue.add(simpleLocation.getRelative(PipesUtil.getDispenserFace(dispenser)));
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
