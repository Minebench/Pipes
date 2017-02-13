package io.github.apfelcreme.Pipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * Returns a hidden string in the itemstack which is hidden using the last lore line
     */
    public static String getHiddenString(String string) {
        // Only the color chars at the end of the string is it
        StringBuilder builder = new StringBuilder();
        char[] chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == ChatColor.COLOR_CHAR)
                continue;
            if (i + 1 < chars.length) {
                if (chars[i + 1] == ChatColor.COLOR_CHAR && i > 1 && chars[i - 1] == ChatColor.COLOR_CHAR)
                    builder.append(c);
                else if (builder.length() > 0)
                    builder = new StringBuilder();
            } else if (i > 0 && chars[i - 1] == ChatColor.COLOR_CHAR)
                builder.append(c);
        }
        if (builder.length() == 0)
            return null;
        return builder.toString();
    }

    public static PipesItem getPipesItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().isEmpty()) {
            return null;
        }

        List<String> lore = item.getItemMeta().getLore();
        if (!lore.get(lore.size() -1).contains(PipesItem.getIdentifier())) {
            return null;
        }

        String hidden = getHiddenString(lore.get(lore.size() - 1));
        if (hidden == null || hidden.isEmpty()) {
            return null;
        }

        try {
            return PipesItem.valueOf(hidden);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static PipesItem getPipesItem(Block block) {
        if (!(block.getState() instanceof InventoryHolder)) {
            return null;
        }

        if (block.getType() != PipesItem.PIPE_INPUT.getMaterial()
                && block.getType() != PipesItem.PIPE_OUTPUT.getMaterial()
                && block.getType() != PipesItem.CHUNK_LOADER.getMaterial()) {
            return null;
        }

        String hidden = getHiddenString(((InventoryHolder) block.getState()).getInventory().getTitle());
        if (hidden == null || hidden.isEmpty()) {
            return null;
        }

        try {
            return PipesItem.valueOf(hidden);
        } catch (IllegalArgumentException e) {
            if (PipesItem.getIdentifier().equals(hidden)) {
                switch (block.getType()) {
                    case DISPENSER:
                        return PipesItem.PIPE_INPUT;
                    case DROPPER:
                        return PipesItem.PIPE_OUTPUT;
                    case FURNACE:
                        return PipesItem.CHUNK_LOADER;
                }
            }
            return null;
        }
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
    public static boolean containsSimilar(Collection<ItemStack> items, ItemStack itemStack) {
        for (ItemStack item : items) {
            if (isSimilarFuzzy(item, itemStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks if the given list of items contains an item stack similar to the given item stack and returns the first one
     *
     * @param items     a list of item stacks
     * @param itemStack an item stack
     * @return The first itemstack matching this one or null if none was found
     */
    public static ItemStack getFirstSimilar(List<ItemStack> items, ItemStack itemStack) {
        for (ItemStack item : items) {
            if (isSimilarFuzzy(item, itemStack)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Check if two ItemStacks are similar ignoring amounts and meta
     * @param a
     * @param b
     * @return
     */
    public static boolean isSimilarFuzzy(ItemStack a, ItemStack b) {
        if (a == null || b == null)
            return false;
        if (a == b)
            return true;
        return a.getType() == b.getType() && a.getData().equals(b.getData());
    }

    /**
     * returns an ItemStack of the custom dispenser item
     *
     * @return an ItemStack of the custom dispenser item
     * @deprecated Use {@link PipesItem#PIPE_INPUT} and {@link PipesItem#toItemStack()}
     */
    @Deprecated
    public static ItemStack getCustomDispenserItem() {
        return PipesItem.PIPE_INPUT.toItemStack();
    }

    /**
     * returns an ItemStack of the custom dropper item
     *
     * @return an ItemStack of the custom dropper item
     * @deprecated Use {@link PipesItem#PIPE_OUTPUT} and {@link PipesItem#toItemStack()}
     */
    @Deprecated
    public static ItemStack getCustomDropperItem() {
        return PipesItem.PIPE_OUTPUT.toItemStack();
    }

    /**
     * returns an ItemStack of the custom chunkLoader item
     *
     * @return an ItemStack of the custom chunkLoader item
     * @deprecated Use {@link PipesItem#CHUNK_LOADER} and {@link PipesItem#toItemStack()}
     */
    @Deprecated
    public static ItemStack getCustomChunkLoaderItem() {
        return PipesItem.CHUNK_LOADER.toItemStack();
    }

    /**
     * Set the fuel of an inventory (currently either of type BREWING or FURNACE)
     * @param inventory The inventory that accepts fuel
     * @param itemStack The item that should be placed as fuel
     */
    public static void setFuel(Inventory inventory, ItemStack itemStack) {
        switch (inventory.getType()) {
            case BREWING:
                ((BrewerInventory) inventory).setFuel(itemStack);
                break;
            case FURNACE:
                ((FurnaceInventory) inventory).setFuel(itemStack);
                break;
            default:
                throw new IllegalArgumentException("Inventories of the type " + inventory.getType() + " do not have fuel!");
        }
    }

    /**
     * Get the fuel of an inventory (currently either of type BREWING or FURNACE)
     * @param inventory The inventory that accepts fuel
     */
    public static ItemStack getFuel(Inventory inventory) {
        switch (inventory.getType()) {
            case BREWING:
                return ((BrewerInventory) inventory).getFuel();
            case FURNACE:
                return ((FurnaceInventory) inventory).getFuel();
            default:
                throw new IllegalArgumentException("Inventories of the type " + inventory.getType() + " do not have fuel!");
        }
    }

    /**
     * Add an item to an inventory. This more complex method is necessary as CraftBukkit doesn't properly
     * return leftovers with partial item stacks
     * @param source The inventory that we move it from
     * @param target Where to move the item to
     * @param itemStack The item stack
     */
    public static void addItem(Inventory source, Inventory target, ItemStack itemStack) {
        source.removeItem(itemStack);
        if (target.firstEmpty() != -1) {
            target.addItem(itemStack);
            itemStack.setAmount(0);
        } else {
            // CraftBukkit doesn't return leftovers when adding to partial stacks
            // To work around this we add the items via an array with one item in
            // each position so that we will get a leftover map in every case
            ItemStack[] stacks = new ItemStack[itemStack.getAmount()];
            for (int i = 0; i < itemStack.getAmount(); i++) {
                ItemStack clone = new ItemStack(itemStack);
                clone.setAmount(1);
                stacks[i] = clone;
            }
            Map<Integer, ItemStack> rest = target.addItem(stacks);
            int newAmount = 0;
            for (ItemStack item : rest.values()) {
                newAmount += item.getAmount();
            }
            itemStack.setAmount(newAmount);
            if (itemStack.getAmount() > 0) {
                source.addItem(itemStack);
                itemStack.setAmount(newAmount); // Need to reset the amount as addItem might change the size
            }
        }
    }

    /**
     * Add fuel to an inventory that supports fuel
     * @param source The inventory that we move it from
     * @param target Where to move the item to
     * @param itemStack The item stack
     * @return Whether or not the fuel was successfully set
     */
    public static boolean addFuel(Inventory source, Inventory target, ItemStack itemStack) {
        ItemStack fuel = getFuel(target);
        if (fuel != null && fuel.isSimilar(itemStack)) {
            ItemStack itemToSet = moveToSingleSlot(source, fuel, itemStack);
            if (itemToSet == null) {
                return false;
            }

            setFuel(target, itemToSet);
        } else if (fuel == null) {
            // there is no fuel currently in the fuel slot, so simply put it in
            source.removeItem(itemStack);
            setFuel(target, itemStack);
            itemStack.setAmount(0);
        }
        return true;
    }

    /**
     * Calculate the result itemstack that should be moved to a single slot containing some item
     * @param source The inventory the added item is coming from
     * @param current The current item in the target inventory
     * @param added The item to be added to the target
     * @return The item stack that should be added to the target inventory
     */
    public static ItemStack moveToSingleSlot(Inventory source, ItemStack current, ItemStack added) {
        if (current == null || current.getAmount() == 0) {
            current = new ItemStack(added);
            added.setAmount(0);
            return current;
        } else if (current.getAmount() < current.getMaxStackSize()) {
            // as you cannot mix two itemstacks with each other, check if the material inserted
            // has the same type as the fuel that is already in current slot

            // there is still room in the slot
            int remaining = current.getMaxStackSize() - current.getAmount(); // amount of room in the slot
            int restSize = added.getAmount() - remaining; // amount of overflowing items

            if (restSize > 0) {
                ItemStack remove = new ItemStack(added);
                remove.setAmount(remaining);
                source.removeItem(remove);
                added.setAmount(restSize);

                current.setAmount(current.getMaxStackSize());
            } else {
                source.removeItem(new ItemStack(added));

                current.setAmount(current.getAmount() + added.getAmount());

                added.setAmount(0);
            }

            return current;
        }

        // the inventory is full, so find continue with the list of outputs
        // and try to fill one that isnt full
        return null;
    }
}
