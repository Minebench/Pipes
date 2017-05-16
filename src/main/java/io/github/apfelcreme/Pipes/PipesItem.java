package io.github.apfelcreme.Pipes;

/**
 * Copyright (C) 2016 Max lLe aka Phoenix616
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
 * @author Max lee aka Phoenix616
 */

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public enum PipesItem {
    PIPE_INPUT("Pipe Input", Material.DISPENSER, "dispenserLore"),
    PIPE_OUTPUT("Pipe Output", Material.DROPPER, "dropperLore"),
    CHUNK_LOADER("Chunk Loader", Material.FURNACE, "chunkLoaderLore");

    private static final String IDENTIFIER = "Pipes";

    private final String name;
    private final Material material;
    private final String loreKey;
    private ItemStack item;

    PipesItem(String name, Material material, String loreKey) {
        this.name = name;
        this.material = material;
        this.loreKey = loreKey;
    }

    public static String getIdentifier() {
        return IDENTIFIER;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public String getLoreKey() {
        return loreKey;
    }

    public ItemStack toItemStack() {
        if (item != null) {
            return item;
        }
        ItemStack item = new ItemStack(this.material);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = Arrays.asList(PipesConfig.getText("info." + this.loreKey),
                ChatColor.BLUE + "" + ChatColor.ITALIC + PipesUtil.hideString(toString(), IDENTIFIER));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString(toString(), name));
        item.setItemMeta(meta);
        this.item = item;
        return item;
    }

    public boolean check(Block block) {
        if (block == null || block.getType() != this.material || !(block.getState() instanceof InventoryHolder)) {
            return false;
        }

        String hidden = PipesUtil.getHiddenString(((InventoryHolder) block.getState()).getInventory().getTitle());

        return hidden != null && toString().equals(hidden.split(",")[0]) || IDENTIFIER.equals(hidden);
    }

    public boolean check(ItemStack item) {
        if (item == null || item.getType() != this.material || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }

        List<String> lore = item.getItemMeta().getLore();
        String hidden = PipesUtil.getHiddenString(lore.get(lore.size() - 1));

        return hidden != null && toString().equals(hidden) || IDENTIFIER.equals(hidden);
    }

    /**
     * Get the enum name as a lowercase string with underscores replaced with dashes
     * @return  The enum name as a config key
     */
    public String toConfigKey() {
        return toString().toLowerCase().replace('_', '-');
    }
}
