package io.github.apfelcreme.Pipes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.apfelcreme.Pipes.Listener.BlockListener;
import io.github.apfelcreme.Pipes.Listener.InventoryChangeListener;
import io.github.apfelcreme.Pipes.Listener.PlayerRightclickListener;
import io.github.apfelcreme.Pipes.Manager.ItemMoveScheduler;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
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
public class Pipes extends JavaPlugin {

    /**
     * the players who have registered a right click for /pipe info
     */
    private Cache<UUID, String> registeredRightClicks;

    /**
     * the plugin instance
     */
    private static Pipes instance = null;

    @Override
    public void onEnable() {
        instance = this;
        registeredRightClicks = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
        PipesConfig.load();
        ItemMoveScheduler.load();
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerRightclickListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginCommand("pipe").setExecutor(new PipeCommand());

        //create the custom recipes
        ShapelessRecipe dispenserRecipe = new ShapelessRecipe(PipesItem.PIPE_INPUT.toItemStack());
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("dispenserRecipe").entrySet()) {
            dispenserRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dispenserRecipe);

        ShapelessRecipe dropperRecipe = new ShapelessRecipe(PipesItem.PIPE_OUTPUT.toItemStack());
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("dropperRecipe").entrySet()) {
            dropperRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(dropperRecipe);

        ShapelessRecipe chunkLoaderRecipe = new ShapelessRecipe(PipesItem.CHUNK_LOADER.toItemStack());
        for (Map.Entry<Material, Integer> material : PipesConfig.getRecipeMaterials("chunkLoaderRecipe").entrySet()) {
            chunkLoaderRecipe.addIngredient(material.getValue(), material.getKey());
        }
        getServer().addRecipe(chunkLoaderRecipe);
    }

    @Override
    public void onDisable() {
        ItemMoveScheduler.exit();
    }

    /**
     * Register a right click action
     */
    public void registerRightClick(Player player, String action) {
        registeredRightClicks.put(player.getUniqueId(), action);
    }

    /**
     * Get a registered a right click action
     */
    public String getRegisterRightClick(Player player) {
        return registeredRightClicks.getIfPresent(player.getUniqueId());
    }

    /**
     * Unregister a right click action
     */
    public void unregisterRightClick(Player player) {
        registeredRightClicks.invalidate(player.getUniqueId());
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
