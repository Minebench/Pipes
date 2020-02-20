package io.github.apfelcreme.Pipes;

import de.minebench.blockinfostorage.BlockInfoStorage;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
import io.github.apfelcreme.Pipes.Pipe.ChunkLoader;
import io.github.apfelcreme.Pipes.Pipe.PipeInput;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

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
public class PipesUtil {

    public static final  Random RANDOM = new Random();
    public static final BlockFace[] BLOCK_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

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
        while (string.length() > 1 && string.charAt(string.length() - 2) == ChatColor.COLOR_CHAR) {
            string = string.substring(0, string.length() - 2);
        }
        // Add hidden string
        char[] chars = new char[hidden.length() * 2];
        for (int i = 0; i < hidden.length(); i++) {
            chars[i * 2] = ChatColor.COLOR_CHAR;
            chars[i * 2 + 1] = hidden.charAt(i);
        }
        return string + new String(chars);
    }

    /**
     * Returns a hidden string in the itemstack which is hidden using the last lore line
     * @param string The string to search in for a hidden string
     * @return The hidden string or <tt>null</tt> if there is none or the input is null
     */
    public static String getHiddenString(String string) {
        if (string == null) {
            return null;
        }
        // Only the color chars at the end of the string is it
        StringBuilder builder = new StringBuilder();
        for (int i = string.length() - 1; i > 0 && string.charAt(i - 1) == ChatColor.COLOR_CHAR; i -= 2) {
            builder.append(string.charAt(i));
        }
        if (builder.length() == 0)
            return null;
        return builder.reverse().toString();
    }

    /**
     * Get the {@link PipesItem} from an ItemStack
     * @param item  the ItemStack
     * @return the PipesItem or null if none found
     */
    public static PipesItem getPipesItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        String type = null;
        if (item.getItemMeta().getPersistentDataContainer().has(AbstractPipePart.TYPE_KEY, PersistentDataType.STRING)) {
            type = item.getItemMeta().getPersistentDataContainer().get(AbstractPipePart.TYPE_KEY, PersistentDataType.STRING);
        } else if (item.getItemMeta().hasLore() && !item.getItemMeta().getLore().isEmpty()) {
            List<String> lore = item.getItemMeta().getLore();
            if (!lore.get(lore.size() - 1).contains(PipesItem.getIdentifier())) {
                return null;
            }

            type = getHiddenString(lore.get(lore.size() - 1));
        }

        if (type == null || type.isEmpty()) {
            return null;
        }

        try {
            return PipesItem.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Get the {@link PipesItem} from a Block
     * @param block  the Block
     * @return the PipesItem or null if none found
     */
    public static PipesItem getPipesItem(Block block) {
        BlockState state = block.getState(false);
        // Paper's non-snapshot BlockState's are broken in some cases
        if (state instanceof Container && ((Container) state).getPersistentDataContainer() == null) {
            state = block.getState(true);
        }

        if (!(state instanceof Container)) {
            return null;
        }

        if (block.getType() != PipesItem.PIPE_INPUT.getMaterial()
                && block.getType() != PipesItem.PIPE_OUTPUT.getMaterial()
                && block.getType() != PipesItem.CHUNK_LOADER.getMaterial()) {
            return null;
        }

        Object stored = null;
        if (((Container) state).getPersistentDataContainer().has(AbstractPipePart.TYPE_KEY, PersistentDataType.STRING)) {
            stored = ((Container) state).getPersistentDataContainer().get(AbstractPipePart.TYPE_KEY, PersistentDataType.STRING);
        } else if (Pipes.hasBlockInfoStorage()) {
            stored = BlockInfoStorage.get().getBlockInfoValue(block, AbstractPipePart.TYPE_KEY);
        }
        String type = null;
        if (stored instanceof String) {
            type = (String) stored;
        }

        if (type == null) {
            type = getHiddenString(((Container) state).getCustomName());
            if (type == null) {
                return null;
            }
            type = type.split(",")[0];
            if (type.isEmpty()) {
                return null;
            }
        }

        try {
            return PipesItem.valueOf(type);
        } catch (IllegalArgumentException e) {
            // Legacy
            if (PipesItem.getIdentifier().equals(type)) {
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
     * This is a helper method to convert a block to a PipesPart.
     * Use {@link PipeManager#getPipePart} for cached ones.
     *
     * @param block The block to convert
     * @return the pipe part or <tt>null</tt> if the block isn't one
     * @deprecated Either use {@link #convertToPipePart(Block, PipesItem)} or {@link PipeManager#getPipePart}
     */
    @Deprecated
    public static AbstractPipePart getPipesPart(Block block) {
        return PipeManager.getInstance().getPipePart(block);
    }

    /**
     * This is a helper method to convert a block to a PipesPart.
     *
     * @param block The block to convert
     * @param type The type of the part
     * @return the pipe part or <tt>null</tt> if the block isn't one
     */
    public static AbstractPipePart convertToPipePart(Block block, PipesItem type) {
        switch (type) {
            case PIPE_INPUT:
                return new PipeInput(block);
            case PIPE_OUTPUT:
                return new PipeOutput(block);
            case CHUNK_LOADER:
                return new ChunkLoader(block);
        }
        return null;
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
     * @param a First item stack
     * @param b Second item stack
     * @return Whether or not they are similar
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
     * @return The fuel ItemStack
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
     * Add an item to an inventory. This more complex method is necessary as not every implementation sets the leftover amount
     * @param target Where to move the item to
     * @param itemStack The item stack
     */
    public static void addItem(Inventory target, ItemStack itemStack) {
        if (target.firstEmpty() != -1) {
            target.addItem(itemStack);
            itemStack.setAmount(0);
        } else {
            Map<Integer, ItemStack> rest = target.addItem(itemStack);
            // Recalculate the leftover amount as changing the input stack's size depends on the implementation
            int newAmount = 0;
            for (ItemStack item : rest.values()) {
                newAmount += item.getAmount();
            }
            itemStack.setAmount(newAmount);
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

    /**
     * Check whether or not a certain potion item can accepts an ingredient
     * @param itemStack     The potion to check
     * @param ingredient    The ingredient
     * @throws IllegalArgumentException When the input item is not a potion
     * @return              <tt>true</tt> if the potion can be brewed with the ingredient; <tt>false</tt> if not
     */
    public static boolean potionAcceptsIngredient(ItemStack itemStack, ItemStack ingredient) throws IllegalArgumentException {
        if (itemStack.getType() != Material.POTION && itemStack.getType() != Material.SPLASH_POTION && itemStack.getType() != Material.LINGERING_POTION) {
            throw new IllegalArgumentException("Input itemstack is not a potion but " + itemStack.getType());
        }
        if (ingredient == null || ingredient.getAmount() <= 0) {
            return false;
        }
        if (ingredient.getType() == Material.GUNPOWDER && itemStack.getType() == Material.POTION) {
            return true;
        }
        if (ingredient.getType() == Material.DRAGON_BREATH && itemStack.getType() == Material.SPLASH_POTION) {
            return true;
        }

        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        PotionType type = meta.getBasePotionData().getType();

        if (type == PotionType.MUNDANE || type == PotionType.THICK) {
            return false;
        }
        switch (ingredient.getType()) {
            case REDSTONE:
            case GLOWSTONE_DUST:
                return meta.getBasePotionData().isUpgraded() || meta.getBasePotionData().isExtended();
            case FERMENTED_SPIDER_EYE:
                // List of potions that can be corrupted
                switch (type) {
                    case WATER:
                    case POISON:
                    case INSTANT_HEAL:
                    case SPEED:
                    case JUMP:
                    case NIGHT_VISION:
                    case SLOW_FALLING:
                    case TURTLE_MASTER:
                        return false;
                    default:
                        return true;
                }
            case COD:
            case SALMON:
            case TROPICAL_FISH:
            case GOLDEN_CARROT:
            case MAGMA_CREAM:
            case RABBIT_FOOT:
            case SUGAR:
            case GLISTERING_MELON_SLICE:
            case SPIDER_EYE:
            case GHAST_TEAR:
            case BLAZE_POWDER:
            case TURTLE_HELMET:
            case PHANTOM_MEMBRANE:
                return type == PotionType.AWKWARD;
        }
        return false;
    }

    /**
     * Get the amount of times that a character appears in a certain string (array)
     * @param c         The character
     * @param strings   The string(s) to search ing
     * @return          The amount of times the character appears in the string(s)
     */
    public static int countChar(char c, String... strings) {
        int amount = 0;
        for (String string : strings) {
            for (char stringChar : string.toCharArray()) {
                if (stringChar == c) {
                    amount++;
                }
            }
        }
        return amount;
    }
}
