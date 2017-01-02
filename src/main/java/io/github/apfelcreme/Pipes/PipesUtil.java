package io.github.apfelcreme.Pipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.DirectionalContainer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
public class PipesUtil {

    private static ItemStack customDispenser;
    private static ItemStack customDropper;
    private static ItemStack customChunkLoader;

    /**
     * returns whether a string only contains numbers
     *
     * @param string the string to be checked
     * @return true or false
     */
    public static boolean isNumeric(String string) {
        return Pattern.matches("([0-9])*", string);
    }

    /**
     * returns the direction a dropper is facing
     *
     * @param dropper the dropper block
     * @return the BlockFace the dropper is facing to
     * @deprecated Use {@link DirectionalContainer#getFacing()}
     */
    @Deprecated
    public static BlockFace getDropperFace(Dropper dropper) {
        if (dropper.getData() instanceof DirectionalContainer) {
            return ((DirectionalContainer) dropper.getData()).getFacing();
        }
        return null;
    }

    /**
     * returns the direction a dispenser is facing
     *
     * @param dispenser the dispenser block
     * @return the BlockFace the dispenser is facing to
     * @deprecated Use {@link DirectionalContainer#getFacing()}
     */
    @Deprecated
    public static BlockFace getDispenserFace(Dispenser dispenser) {
        if (dispenser.getData() instanceof DirectionalContainer) {
            return ((DirectionalContainer) dispenser.getData()).getFacing();
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


    /**
     * Remove a specific material from an inventory
     *
     * @param inventory     the inventory
     * @param material      the material
     * @param count         the amount to remove
     * @param removeSpecial should we remove items that have meta/enchantments/are damaged?
     */
    public static void removeItems(Inventory inventory, Material material, int count, boolean removeSpecial) {
        for (int i = 0; i < inventory.getContents().length && count > 0; i++) {
            ItemStack itemStack = inventory.getContents()[i];
            if (itemStack != null && itemStack.getType() == material) {
                if (removeSpecial || (!itemStack.hasItemMeta() && itemStack.getDurability() == 0)) {
                    if (itemStack.getAmount() > count) {
                        itemStack.setAmount(itemStack.getAmount() - count);
                        inventory.setItem(i, itemStack);
                        count = 0;
                    } else {
                        count -= itemStack.getAmount();
                        inventory.clear(i);
                    }
                }
            }
        }
    }

    /**
     * checks if the given list of items contains an item stack similar to the given item stack
     *
     * @param items     a list of item stacks
     * @param itemStack an item stack
     * @return true if there is an item stack of the same type with the same data. Amount may vary
     */
    public static boolean containsSimilar(List<ItemStack> items, ItemStack itemStack) {
        for (ItemStack item : items) {
            if ((item.getType() == itemStack.getType()) && (item.getData().getData() == itemStack.getData().getData())) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if a type can be used for smelting in a furnace
     *
     * @param type a material type
     * @return true or false
     */
    public static boolean isFuel(Material type) {
        switch (type) {
            case COAL:
            case COAL_BLOCK:
            case LAVA_BUCKET:
                return true;
            default:
                return false;
        }
    }

    /**
     * returns an ItemStack of the custom dispenser item
     *
     * @return an ItemStack of the custom dispenser item
     */
    public static ItemStack getCustomDispenserItem() {
        if (customDispenser != null) {
            return customDispenser;
        }

        customDispenser = new ItemStack(Material.DISPENSER);
        ItemMeta meta = customDispenser.getItemMeta();
        List<String> lore = Arrays.asList(ChatColor.BLUE + "" + ChatColor.ITALIC + hideString("Pipes", "Pipes"),
                PipesConfig.getText("info.dispenserLore"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString("Pipes", "Pipe Input"));
        customDispenser.setItemMeta(meta);
        return customDispenser;
    }

    /**
     * returns an ItemStack of the custom dropper item
     *
     * @return an ItemStack of the custom dropper item
     */
    public static ItemStack getCustomDropperItem() {
        if (customDropper != null) {
            return customDropper;
        }

        customDropper = new ItemStack(Material.DROPPER);
        ItemMeta meta = customDropper.getItemMeta();
        List<String> lore = Arrays.asList(ChatColor.BLUE + "" + ChatColor.ITALIC + hideString("Pipes", "Pipes"),
                PipesConfig.getText("info.dropperLore"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString("Pipes", "Pipe Output"));
        customDropper.setItemMeta(meta);
        return customDropper;
    }

    /**
     * returns an ItemStack of the custom chunkLoader item
     *
     * @return an ItemStack of the custom chunkLoader item
     */
    public static ItemStack getCustomChunkLoaderItem() {
        if (customChunkLoader != null) {
            return customChunkLoader;
        }

        customChunkLoader = new ItemStack(Material.FURNACE);
        ItemMeta meta = customChunkLoader.getItemMeta();
        List<String> lore = Arrays.asList(ChatColor.BLUE + "" + ChatColor.ITALIC + hideString("Pipes", "Pipes"),
                PipesConfig.getText("info.chunkLoaderLore"));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString("Pipes", "Chunk Loader"));
        customChunkLoader.setItemMeta(meta);
        return customChunkLoader;
    }
}
