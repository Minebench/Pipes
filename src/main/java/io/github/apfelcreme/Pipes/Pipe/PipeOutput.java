package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.List;

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
        this.facing = ((Directional) block.getState().getData()).getFacing();
    }

    /**
     * returns the InventoryHolder
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getTargetHolder() {
        Block block = getTargetLocation().getBlock();
        if (block != null && block.getState() instanceof InventoryHolder) {
            return (InventoryHolder) block.getState();
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
     * @param itemStack The item to check
     * @return  A result that represents why or why not the item is accepted by this output
     */
    public AcceptResult accepts(ItemStack itemStack) {
        Block block = getLocation().getBlock();
        if (block == null || !(block.getState() instanceof InventoryHolder)) {
            return new AcceptResult(ResultType.DENY_INVALID, null);
        }
        if ((boolean) getOption(PipeOutput.Option.OVERFLOW) && block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, null);
        }

        ItemStack filter = null;
        boolean isEmpty = true;
        boolean isWhitelist = (boolean) getOption(Option.WHITELIST);
        for (ItemStack filterItem : ((InventoryHolder) block.getState()).getInventory().getContents()) {
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

        if ((boolean) getOption(Option.DATA_FILTER)) {
            if (filter.hasItemMeta() != item.hasItemMeta()) {
                return false;
            }
            if (filter.hasItemMeta() && !filter.getItemMeta().equals(item.getItemMeta())) {
                return false;
            }
        }

        if ((boolean) getOption(Option.MATERIAL_FILTER)) {
            if (!filter.getData().equals(item.getData())) {
                return false;
            }
        }

        if ((boolean) getOption(Option.DAMAGE_FILTER)) {
            if (filter.getDurability() != item.getDurability()) {
                return false;
            }
        }

        if ((boolean) getOption(Option.DISPLAY_FILTER)) {
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

        if ((boolean) getOption(Option.ENCHANTMENT_FILTER)) {
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
    protected IOption[] getOptions() {
        return Option.values();
    }

    @Override
    protected IOption getAvailableOption(String name) {
        return Option.valueOf(name);
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

    public enum Option implements IOption {
        /**
         * Whether or not this output is in whitelist mode. This changes how the filter items are used.
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output is in whitelist mode and should only let items through that are in the inventory</li>
         * <li><tt>false</tt> if this output is in blacklist mode and should only let items through that are <strong>not</strong> in the inventory</li></p>
         */
        WHITELIST(GuiPosition.RIGHT, Value.TRUE, Value.FALSE),
        /**
         * Whether or not this output can overflow into other available outputs
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if the items should end up in the overflow</li>
         * <li><tt>false</tt> if this output should force items to end up here even 'though the target is full</li></p>
         */
        OVERFLOW(GuiPosition.LEFT, Value.FALSE, Value.TRUE),
        /**
         * Whether or not to try to insert into the appropriate slots depending on the item type (like fuel)
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> Detect the item type and put it in the slot that it belongs to</li>
         * <li><tt>false</tt> Use the face that the output is facing to select the slot</li></p>
         */
        SMART_INSERT(GuiPosition.LEFT, Value.TRUE, Value.FALSE),
        /**
         * Whether or not to respect the material of the filter item when filtering.
         */
        MATERIAL_FILTER(GuiPosition.RIGHT, Value.TRUE, Value.FALSE),
        /**
         * Whether or not to respect tool damage values when filtering
         */
        DAMAGE_FILTER(GuiPosition.RIGHT, Value.FALSE, Value.TRUE),
        /**
         * Whether or not to respect custom item names and lores when filtering
         */
        DISPLAY_FILTER(GuiPosition.RIGHT, Value.FALSE, Value.TRUE),
        /**
         * Whether or to respect enchantments when filtering
         */
        ENCHANTMENT_FILTER(GuiPosition.RIGHT, Value.FALSE, Value.TRUE),
        /**
         * Filter all the data of the items exactly
         */
        DATA_FILTER(GuiPosition.RIGHT, Value.FALSE, Value.TRUE),
        /**
         * Whether or not to use the amount of the filter item as the amount to which the target should be filled up to
         */
        TARGET_AMOUNT(GuiPosition.LEFT, Value.FALSE, Value.TRUE),
        /**
         * Whether or not to drop the item instead of adding to an inventory
         */
        DROP(GuiPosition.LEFT, Value.FALSE, Value.TRUE);

        private final Value defaultValue;
        private final Class<?> valueType;
        private final Value[] possibleValues;
        private final GuiPosition guiPosition;

        /**
         * An option that this pipe output can have
         * @param defaultValue  The default value when none is set
         * @param valueType     The class of the values that this option accepts
         */
        Option(GuiPosition guiPosition, Value defaultValue, Class<?> valueType) {
            this.defaultValue = defaultValue;
            this.valueType = valueType;
            possibleValues = new Value[0];
            this.guiPosition = guiPosition;
        }

        /**
         * An option that this pipe output can have
         * @param possibleValues    An array of possible values that this option accepts
         * @throws IllegalArgumentException Thrown when there are less than two possible values defined
         */
        Option(GuiPosition guiPosition, Value... possibleValues) throws IllegalArgumentException {
            if (possibleValues.length < 2) {
                throw new IllegalArgumentException("An option needs to have at least two values!");
            }
            this.possibleValues = possibleValues;
            defaultValue = possibleValues[0];
            valueType = defaultValue.getValue().getClass();
            this.guiPosition = guiPosition;
        }

        public Class<?> getValueType() {
            return valueType;
        }

        public Value getDefaultValue() {
            return defaultValue;
        }

        public Value[] getPossibleValues() {
            return possibleValues;
        }

        @Override
        public GuiPosition getGuiPosition() {
            return guiPosition;
        }
    }
}
