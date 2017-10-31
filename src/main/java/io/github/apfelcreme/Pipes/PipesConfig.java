package io.github.apfelcreme.Pipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
     * returns the time that the cache stores all the locations of pipes
     *
     * @return the delay of pipe recalculation in s
     */
    public static long getPipeCacheDuration() {
        return plugin.getConfig().getLong("pipeCacheDuration");
    }

    /**
     * returns the amount of locations to store in the cache
     *
     * @return the amount of locations to store
     */
    public static long getPipeCacheSize() {
        return plugin.getConfig().getLong("pipeCacheSize");
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
     * @param replacements
     * @return the text
     */
    public static String getText(String key, String... replacements) {
        String ret = (String) languageConfig.get("texts." + key);
        if (ret != null && !ret.isEmpty()) {
            for (int i = 0; i < replacements.length; i++) {
                ret = ret.replace("{" + i + "}", replacements[i]);
            }
            ret = ChatColor.translateAlternateColorCodes('&', ret);
            return ChatColor.translateAlternateColorCodes('§', ret);
        } else {
            return "Missing text node: " + key;
        }
    }

    public static ItemStack getGuiItemStack(String key) {
        while (!itemStacks.containsKey("gui." + key) && !plugin.getConfig().contains("gui." + key)) {
            int index = key.indexOf('.');
            if (index == -1 || index + 1 >= key.length()) {
                break;
            }
            key = key.substring(index + 1);
        }
        return getItemStack("gui." + key);
    }

    public static ItemStack getItemStack(String key) {
        if (itemStacks.containsKey(key)) {
            return new ItemStack(itemStacks.get(key));
        }

        ItemStack serializedItem = plugin.getConfig().getItemStack(key);
        if (serializedItem != null) {
            itemStacks.put(key, serializedItem);
            return serializedItem;
        }

        String error = "Missing item config entry for " + key;
        String input = plugin.getConfig().getString(key);
        if (input != null) {
            String[] parts = input.split(":");
            try {
                Material mat = Material.valueOf(parts[0].toUpperCase());
                byte data = 0;
                if (parts.length > 1) {
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
}
