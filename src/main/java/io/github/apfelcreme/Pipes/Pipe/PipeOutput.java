package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class PipeOutput extends AbstractPipePart {

    public final static String[] GUI_SETUP = {
            "ss iii zz",
            "ss iii zz",
            "ss iii zz",
            "ss  c  zz"
    };

    private final BlockFace facing;

    public PipeOutput(Block block) {
        super(PipesItem.PIPE_OUTPUT, block);
        this.facing = ((Directional) block.getState(false).getData()).getFacing();
    }

    /**
     * returns the InventoryHolder
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getTargetHolder() {
        Block block = getTargetLocation().getBlock();
        BlockState state = block != null ? block.getState(false) : null;
        if (state != null && state instanceof InventoryHolder) {
            return (InventoryHolder) state;
        }
        return null;
    }

    public SimpleLocation getTargetLocation() {
        return getLocation().getRelative(getFacing());
    }

    public BlockFace getFacing() {
        return facing;
    }

    /**
     * returns the list of items in the dropper
     *
     * @return the list of items in the dropper
     */
    public List<ItemStack> getFilterItems() {
        List<ItemStack> sorterItems = new ArrayList<>();
        InventoryHolder holder = getHolder();
        if (holder != null) {
            for (ItemStack itemStack : holder.getInventory().getContents()) {
                if (itemStack != null) {
                    sorterItems.add(itemStack);
                }
            }
        }
        return sorterItems;
    }

    /**
     * Check whether or not this output can accept that item stack
     *
     * @param input     The input that tries to move the item
     * @param itemStack The item to check
     * @return  A result that represents why or why not the item is accepted by this output
     */
    public AcceptResult accepts(PipeInput input, ItemStack itemStack) {
        Block block = getLocation().getBlock();
        BlockState state = block != null ? block.getState(false) : null;
        if (state == null || !(state instanceof InventoryHolder)) {
            return new AcceptResult(ResultType.DENY_INVALID, null);
        }
        Options.Overflow outputOverflow = getOption(Options.OVERFLOW);
        if ((outputOverflow == Options.Overflow.TRUE || outputOverflow == Options.Overflow.INPUT && input.getOption(PipeInput.Options.OVERFLOW))
                && block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, null);
        }

        ItemStack filter = null;
        boolean isEmpty = true;
        boolean isWhitelist = getOption(Options.WHITELIST);
        for (ItemStack filterItem : ((InventoryHolder) state).getInventory().getContents()) {
            isEmpty &= filterItem == null;
            if (matchesFilter(filterItem, itemStack)) {
                if (isWhitelist) {
                    filter = filterItem;
                    break;
                } else {
                    return new AcceptResult(ResultType.DENY_BLACKLIST, filterItem);
                }
            }
        }

        if (block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, filter);
        } else if (!isEmpty && isWhitelist && filter == null) {
            return new AcceptResult(ResultType.DENY_WHITELIST, null);
        } else {
            return new AcceptResult(ResultType.ACCEPT, filter);
        }
    }

    /**
     * Check whether or not an item stack matches the filter of this output
     * @param filter    The filter item to match against
     * @param item      The item stack to check
     * @return          <tt>true</tt> if the filter is similar; <tt>false</tt> if not
     */
    public boolean matchesFilter(ItemStack filter, ItemStack item) {
        if (filter == null || item == null) {
            return false;
        }

        if (getOption(Options.DATA_FILTER)) {
            if (filter.hasItemMeta() != item.hasItemMeta()) {
                return false;
            }
            if (filter.hasItemMeta() && !filter.getItemMeta().equals(item.getItemMeta())) {
                return false;
            }
        }

        if (getOption(Options.MATERIAL_FILTER)) {
            if (filter.getType() != item.getType()) {
                return false;
            }
        }

        if (getOption(Options.DAMAGE_FILTER)) {
            if (filter.getDurability() != item.getDurability()) {
                return false;
            }
        }

        if (getOption(Options.DISPLAY_FILTER)) {
            if (filter.hasItemMeta() != item.hasItemMeta()) {
                return false;
            }
            if (filter.hasItemMeta()) {
                ItemMeta filterMeta = filter.getItemMeta();
                ItemMeta meta = item.getItemMeta();
                if (filterMeta.hasDisplayName() != meta.hasDisplayName()) {
                    return false;
                }
                if (filterMeta.hasDisplayName() && !filterMeta.getDisplayName().equals(meta.getDisplayName())) {
                    return false;
                }
                if (filterMeta.hasLore() != meta.hasLore()) {
                    return false;
                }
                if (filterMeta.hasLore() && !filterMeta.getLore().equals(meta.getLore())) {
                    return false;
                }
            }
        }

        if (getOption(Options.ENCHANTMENT_FILTER)) {
            if (!filter.getEnchantments().equals(item.getEnchantments())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String[] getGuiSetup() {
        return GUI_SETUP;
    }

    @Override
    protected Option<?>[] getOptions() {
        return Options.values();
    }

    @Override
    protected Option<?> getAvailableOption(String name) {
        return Options.get(name);
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeOutput) o).facing) : ((PipeOutput) o).facing != null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class AcceptResult {

        private final ResultType type;
        private final ItemStack filterItem;

        public AcceptResult(ResultType type, ItemStack filterItem) {
            this.type = type;
            this.filterItem = filterItem;
        }

        public ResultType getType() {
            return type;
        }

        public boolean isInFilter() {
            return filterItem != null;
        }

        public ItemStack getFilterItem() {
            return filterItem;
        }
    }

    public enum ResultType {
        ACCEPT,
        DENY_REDSTONE,
        DENY_WHITELIST,
        DENY_BLACKLIST,
        DENY_AMOUNT,
        DENY_INVALID;
    }

    public static class Options extends OptionsList {

        private static final Map<String, Option<?>> VALUES = new HashMap<>();

        /**
         * Whether or not this output is in whitelist mode. This changes how the filter items are used.
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output is in whitelist mode and should only let items through that are in the inventory</li>
         * <li><tt>false</tt> if this output is in blacklist mode and should only let items through that are <strong>not</strong> in the inventory</li></p>
         */
        public static final Option<Boolean> WHITELIST = add(new Option<>("WHITELIST", Option.GuiPosition.RIGHT, Value.TRUE, Value.FALSE));

        /**
         * Whether or not this output can overflow into other available outputs
         * <p><strong>Possible Values:</strong>
         * <li><tt>input</tt> if the input settings should decide the overflow rules</li>
         * <li><tt>enabled</tt> if the items should end up in the overflow</li>
         * <li><tt>disabled</tt> if this output should force items to end up here even 'though the target is full</li></p>
         */
        public static final Option<Overflow> OVERFLOW = add(new Option<>("OVERFLOW", Option.GuiPosition.LEFT, new Value<>(Overflow.INPUT), new Value<>(Overflow.TRUE), new Value<>(Overflow.FALSE)));

        /**
         * Whether or not to try to insert into the appropriate slots depending on the item type (like fuel)
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> Detect the item type and put it in the slot that it belongs to</li>
         * <li><tt>false</tt> Use the face that the output is facing to select the slot</li></p>
         */
        public static final Option<Boolean> SMART_INSERT = add(new Option<>("SMART_INSERT", Option.GuiPosition.LEFT, Value.TRUE, Value.FALSE));

        /**
         * Whether or not to respect the material of the filter item when filtering.
         */
        public static final Option<Boolean> MATERIAL_FILTER = add(new Option<>("MATERIAL_FILTER", Option.GuiPosition.RIGHT, Value.TRUE, Value.FALSE));

        /**
         * Whether or not to respect tool damage values when filtering
         */
        public static final Option<Boolean> DAMAGE_FILTER = add(new Option<>("DAMAGE_FILTER", Option.GuiPosition.RIGHT, Value.FALSE, Value.TRUE));

        /**
         * Whether or not to respect custom item names and lores when filtering
         */
        public static final Option<Boolean> DISPLAY_FILTER = add(new Option<>("DISPLAY_FILTER", Option.GuiPosition.RIGHT, Value.FALSE, Value.TRUE));

        /**
         * Whether or to respect enchantments when filtering
         */
        public static final Option<Boolean> ENCHANTMENT_FILTER = add(new Option<>("ENCHANTMENT_FILTER", Option.GuiPosition.RIGHT, Value.FALSE, Value.TRUE));

        /**
         * Filter all the data of the items exactly
         */
        public static final Option<Boolean> DATA_FILTER = add(new Option<>("DATA_FILTER", Option.GuiPosition.RIGHT, Value.FALSE, Value.TRUE));

        /**
         * Whether or not to use the amount of the filter item as the amount to which the target should be filled up to
         */
        public static final Option<Boolean> TARGET_AMOUNT = add(new Option<>("TARGET_AMOUNT", Option.GuiPosition.LEFT, Value.FALSE, Value.TRUE));

        /**
         * Whether or not to drop the item instead of adding to an inventory
         */
        public static final Option<Boolean> DROP = add(new Option<>("DROP", Option.GuiPosition.LEFT, Value.FALSE, Value.TRUE));

        protected static <T> Option<T> add(Option<T> option) {
            VALUES.put(option.name().toLowerCase(), option);
            return option;
        }

        public static Option<?> get(String name) {
            Option<?> option = VALUES.get(name.toLowerCase());
            if (option == null) {
                throw new IllegalArgumentException("No option with name " + name + " found!");
            }
            return option;
        }

        public static Option<?>[] values() {
            return VALUES.values().toArray(new Option<?>[0]);
        }

        public enum Overflow {
            INPUT,
            FALSE,
            TRUE
        }
    }
}
