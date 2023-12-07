package io.github.apfelcreme.Pipes;

import de.themoep.utils.lang.LanguageConfig;
import de.themoep.utils.lang.bukkit.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

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
public class PipesConfig {

    private static LanguageManager languageManager;

    private static Pipes plugin;
    private static long transferCooldown;
    private static int transferCount;
    private static double inputToOutputRatio;
    private static int maxPipeOutputs;
    private static int maxPipeLength;
    private static boolean pistonUpdateCheck;
    private static int custommodelDataOffset;
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
        transferCount = plugin.getConfig().getInt("transferCount");
        inputToOutputRatio = plugin.getConfig().getDouble("inputToOutputRatio");
        maxPipeOutputs = plugin.getConfig().getInt("maxPipeOutputs");
        maxPipeLength = plugin.getConfig().getInt("maxPipeLength");
        pistonUpdateCheck = plugin.getConfig().getBoolean("pistonUpdateCheck");
        custommodelDataOffset = plugin.getConfig().getInt("custommodelDataOffset");
        languageManager = new LanguageManager(plugin, getDefaultLocale());
        languageManager.loadConfigs();
        itemStacks = new HashMap<>();
    }

    /**
     * returns the configured default locale
     *
     * @return the default locale
     */
    public static String getDefaultLocale() {
        return plugin.getConfig().getString("defaultLocale");
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
     * returns the max amount of item stacks transfered per pipe transfer
     *
     * @return the max amount of item stacks transfered per pipe transfer
     */
    public static double getTransferCount() {
        return transferCount;
    }

    /**
     * returns the ratio of inputs to outputs for a pipe's transfer, 0 for unlimited
     *
     * @return the ratio of inputs to outputs for a pipe's transfer, 0 for unlimited
     */
    public static double getInputToOutputRatio() {
        return inputToOutputRatio;
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
     * returns whether or not piston checking is enabled
     *
     * @return whether or not piston checking is enabled
     */
    public static boolean isPistonCheckEnabled() {
        return pistonUpdateCheck;
    }

    /**
     * returns the offset for the custom model data of the items
     *
     * @return the custom model data offset
     */
    public static int getCustomModelDataOffset() {
        return custommodelDataOffset;
    }

    /**
     * returns the materials and their quantity for a custom recipe
     *
     * @param path  The config path
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
     * @param sender       the sender to get the locale for
     * @param key          the config path
     * @param replacements the replacements as an array of alternating placeholder and value
     * @return the text
     */
    public static String getText(CommandSender sender, String key, String... replacements) {
        return getText(languageManager.getConfig(sender), key, replacements);
    }

    /**
     * returns a texty string
     *
     * @param locale       the locale to use
     * @param key          the config path
     * @param replacements the replacements as an array of alternating placeholder and value
     * @return the text
     */
    public static String getText(String locale, String key, String... replacements) {
        return getText(languageManager.getConfig(locale), key, replacements);
    }

    /**
     * returns a texty string
     *
     * @param config       the config to use
     * @param key          the config path
     * @param replacements the replacements as an array of alternating placeholder and value
     * @return the text
     */
    private static String getText(LanguageConfig<?> config, String key, String... replacements) {
        String ret = config.get("texts." + key);
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

    /**
     * Get the ItemStack from the GUI config
     * @param key   The key, all parts get checked.
     * @return The ItemStack or placeholder. Never null.
     */
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

    /**
     * Get the ItemStack from the GUI config
     * @param key   The literal key
     * @return The ItemStack or placeholder. Never null.
     */
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
                int data;
                if (parts.length > 1) {
                    data = Integer.parseInt(parts[1]);
                } else {
                    data = 0;
                }
                ItemStack item = new ItemStack(mat);
                item.editMeta(meta -> meta.setCustomModelData(data));
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
