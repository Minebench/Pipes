package io.github.apfelcreme.Pipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
public class PipesConfig {

    private static YamlConfiguration languageConfig;
    private static YamlConfiguration locationConfig;

    private static Pipes plugin;

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
        plugin.saveResource("locations.yml", false);

        plugin.reloadConfig();
        languageConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/lang.de.yml"));
        locationConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/locations.yml"));
    }

    /**
     * returns the list of registered dispensers
     *
     * @return the list of registered dispensers
     */
    public static Configuration getLocationConfig() {
        return locationConfig;
    }

    /**
     * saves the location config
     */
    public static void saveLocationConfig() {
        try {
            locationConfig.save(new File(plugin.getDataFolder() + "/locations.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * returns the time of the delay of pipe recalculation when e.g. a hopper transfers items into a dispenser.
     * with this it is only checked every 10 seconds if there is a pipe
     *
     * @return the delay of pipe recalculation
     */
    public static Long getPipeCacheDuration() {
        return plugin.getConfig().getLong("pipeCacheDuration", 10000L);
    }

    /**
     * returns the delay between item transfers
     *
     * @return the delay between item transfers
     */
    public static Long getTransferCooldown() {
        return plugin.getConfig().getLong("transferCooldown", 1000L);
    }

    /**
     * returns the materials and their quantity for the custom dispenser recipe
     *
     * @return the materials and their quantity for the custom dispenser recipe
     */
    public static Map<Material, Integer> getDispenserMaterials() {
        Map<Material, Integer> ret = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("dispenserRecipe");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ret.put(Material.getMaterial(key), section.getInt(key));
            }
        }
        return ret;
    }

    /**
     * returns the materials and their quantity for the custom dropper recipe
     *
     * @return the materials and their quantity for the custom dropper recipe
     */
    public static Map<Material, Integer> getDropperMaterials() {
        Map<Material, Integer> ret = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("dropperRecipe");
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

}
