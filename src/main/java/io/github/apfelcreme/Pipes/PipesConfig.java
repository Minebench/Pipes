package io.github.apfelcreme.Pipes;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
public class PipesConfig {

    private static YamlConfiguration languageConfig;

    private static Pipes plugin;
    private static long transferCooldown;
    private static int maxPipeOutputs;
    private static int maxPipeLength;
    private static ItemStack guiFiller;
    private static Map<String, ItemStack> itemStacks;

    /**
     * loads the config
     */
    public static void load() {
        plugin = Pipes.getInstance();
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        plugin.saveDefaultConfig();
        plugin.saveResource("lang.de.yml", false);
        plugin.reloadConfig();
        transferCooldown = plugin.getConfig().getLong("transferCooldown");
        maxPipeOutputs = plugin.getConfig().getInt("maxPipeOutputs");
        maxPipeLength = plugin.getConfig().getInt("maxPipeLength");
        languageConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "lang.de.yml"));
        itemStacks = new HashMap<>();
    }

    /**
     * returns the time of the delay of pipe recalculation when e.g. a hopper transfers items into a dispenser.
     * with this it is only checked every 10 seconds if there is a pipe
     *
     * @return the delay of pipe recalculation in ms
     */
    public static long getPipeCacheDuration() {
        return plugin.getConfig().getLong("pipeCacheDuration", 10000L);
    }

    /**
     * returns the delay between item transfers
     *
     * @return the delay between item transfers
     */
    public static long getTransferCooldown() {
        return transferCooldown;
    }

    /**
     * returns the maximum number of outputs a pipe can have
     *
     * @return the maximum number of outputs a pipe can have
     */
    public static int getMaxPipeOutputs() {
        return maxPipeOutputs;
    }

    /**
     * returns the maximum length of a pipe
     *
     * @return the maximum length of a pipe
     */
    public static int getMaxPipeLength() {
        return maxPipeLength;
    }


    /**
     * returns the materials and their quantity for a custom recipe
     *
     * @return the materials and their quantity for a custom recipe
     */
    public static Map<Material, Integer> getRecipeMaterials(String path) {
        Map<Material, Integer> ret = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ret.put(Material.getMaterial(key), section.getInt(key));
            }
        }
        return ret;
    }

    /**
     * returns a texty string
     *
     * @param key the config path
     * @return the text
     */
    public static String getText(String key) {
        String ret = (String) languageConfig.get("texts." + key);
        if (ret != null && !ret.isEmpty()) {
            ret = ChatColor.translateAlternateColorCodes('&', ret);
            return ChatColor.translateAlternateColorCodes('§', ret);
        } else {
            return "Missing text node: " + key;
        }
    }

    public static ItemStack getItemStack(String key) {
        if (itemStacks.containsKey(key)) {
            return new ItemStack(itemStacks.get(key));
        }

        String error = "Missing item config entry for " + key;
        String input = plugin.getConfig().getString(key);
        if (input != null) {
            String[] parts = input.split(":");
            try {
                Material mat = Material.valueOf(parts[0].toUpperCase());
                byte data = 0;
                if (parts.length > 0) {
                    data = Byte.parseByte(parts[1]);
                }
                ItemStack item = new ItemStack(mat, 1, data);
                itemStacks.put(key, item);
                return item;
            } catch (NumberFormatException e) {
                error = parts[1] + " is not a valid Byte!";
            } catch (IllegalArgumentException e) {
                error = parts[0].toUpperCase() + " is not a valid Material name!";
            }
        }
        plugin.getLogger().log(Level.WARNING, error);
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + error);
        item.setItemMeta(meta);
        itemStacks.put(key, item);
        return item;
    }

    public static ItemStack getGuiFiller() {
        if (guiFiller == null) {
            guiFiller = getItemStack("gui.filler");
        }
        return new ItemStack(guiFiller);
    }
}
