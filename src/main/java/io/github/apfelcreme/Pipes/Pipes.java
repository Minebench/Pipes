package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Listener.BlockListener;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
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
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("dispenserRecipe").entrySet()) {
            dispenserRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dispenserRecipe);

        ShapelessRecipe dropperRecipe = new ShapelessRecipe(PipesUtil.getCustomDropperItem());
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("dropperRecipe").entrySet()) {
            dropperRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dropperRecipe);

        ShapelessRecipe chunkLoaderRecipe = new ShapelessRecipe(PipesUtil.getCustomChunkLoaderItem());
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("chunkLoaderRecipe").entrySet()) {
            chunkLoaderRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(chunkLoaderRecipe);
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

}
