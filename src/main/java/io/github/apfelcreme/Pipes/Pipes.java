package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Listener.BlockListener;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
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

    private static Pipes instance = null;

    @Override
    public void onEnable() {
        instance = this;
        registeredRightClicks = new HashMap<>();
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerRightclickListener(), this);
        getServer().getPluginManager().registerEvents(new BlockListener(), this);
        getServer().getPluginCommand("pipe").setExecutor(new PipeCommand());
        PipesConfig.load();

        //create the custom recipes
        ShapelessRecipe dispenserRecipe = new ShapelessRecipe(getCustomDispenserItem())
                .addIngredient(1, Material.IRON_BLOCK)
                .addIngredient(1, Material.DISPENSER);
        getServer().addRecipe(dispenserRecipe);

        ShapelessRecipe dropperRecipe = new ShapelessRecipe(getCustomDropperItem())
                .addIngredient(1, Material.IRON_BLOCK)
                .addIngredient(1, Material.DROPPER);
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
     * returns an ItemStack of the custom dispenser item
     *
     * @return an ItemStack of the custom dispenser item
     */
    public static ItemStack getCustomDispenserItem() {
        ItemStack customDispenser = new ItemStack(Material.DISPENSER);
        ItemMeta meta = customDispenser.getItemMeta();
        List<String> lore = Arrays.asList(hideString("Pipes", ""), ChatColor.BLUE + "" + ChatColor.ITALIC + "Pipes");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName("Pipe Input");
        customDispenser.setItemMeta(meta);
        return customDispenser;
    }

    /**
     * returns an ItemStack of the custom dropper item
     *
     * @return an ItemStack of the custom dropper item
     */
    public static ItemStack getCustomDropperItem() {
        ItemStack customDropper = new ItemStack(Material.DROPPER);
        ItemMeta meta = customDropper.getItemMeta();
        List<String> lore = Arrays.asList(hideString("Pipes", ""), ChatColor.BLUE + "" + ChatColor.ITALIC + "Pipes");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName("Pipe Output");
        customDropper.setItemMeta(meta);
        return customDropper;
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

        queue.add(new SimpleLocation(startingPoint.getX(), startingPoint.getY(), startingPoint.getZ()));

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
                        if (block.getRelative(getDropperFace(dropper)).getState() instanceof InventoryHolder) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                outputs.add(new PipeOutput(dropper,
                                        (InventoryHolder) block.getRelative(getDropperFace(dropper)).getState()));
                                found.add(block);
                                found.add(block.getRelative(getDropperFace(dropper)));
                            }
                        }
                    } else if (block.getState() instanceof Dispenser) {
                        Dispenser dispenser = (Dispenser) block.getState();
                        if (block.getRelative(getDispenserFace(dispenser)).getType() == Material.STAINED_GLASS) {
                            if (InputOutputLocationManager.isBlockListed(block)) {
                                inputs.add(new PipeInput(dispenser));
                                found.add(block);
                                queue.add(simpleLocation.getRelative(getDispenserFace(dispenser)));
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

    /**
     * Hide a string inside another string with chat color characters
     *
     * @param hidden The string to hide
     * @param string The string to hide in
     * @return The string with the hidden string appended
     */
    public static String hideString(String hidden, String string) {
        for (int i = string.length() - 1; i >= 0; i--) {
            if (string.length() - i > 2)
                break;
            if (string.charAt(i) == ChatColor.COLOR_CHAR)
                string = string.substring(0, i);
        }
        // Add hidden string
        for (int i = 0; i < hidden.length(); i++) {
            string += ChatColor.COLOR_CHAR + hidden.substring(i, i + 1);
        }
        return string;
    }
}
