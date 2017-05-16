package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Nameable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.List;
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
public class PipeOutput extends AbstractPipePart {

    private final BlockFace facing;

    public PipeOutput(SimpleLocation location, BlockFace facing) {
        super(PipesItem.PIPE_OUTPUT, location);
        this.facing = facing;
    }

    public PipeOutput(Block block) {
        this(new SimpleLocation(block.getLocation()), ((Directional) block.getState().getData()).getFacing());
        if (block.getState() instanceof Nameable) {
            String hidden = PipesUtil.getHiddenString(((Nameable) block.getState()).getCustomName());
            if (hidden != null) {
                for (String group : hidden.split(",")) {
                    String[] parts = group.split("=");
                    if (parts.length < 2) {
                        continue;
                    }
                    try {
                        Option option = Option.valueOf(parts[0].toUpperCase());
                        setOption(option, new Value<>(Boolean.parseBoolean(parts[1])), false);
                    } catch (IllegalArgumentException e) {
                        Pipes.getInstance().getLogger().log(Level.WARNING, "PipeOutput at " + block.getLocation() + " has an invalid option " + parts[0] + "=" + parts[1] + "?");
                    }
                }
            }
        }
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
            return new AcceptResult(ResultType.DENY_INVALID, false);
        }
        if ((boolean) getOption(PipeOutput.Option.OVERFLOW) && block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, false);
        }

        boolean inFilter = false;
        boolean isEmpty = true;
        boolean isWhitelist = (boolean) getOption(Option.WHITELIST);
        for (ItemStack filterItem : ((InventoryHolder) block.getState()).getInventory().getContents()) {
            isEmpty &= filterItem == null;
            if (matchesFilter(filterItem, itemStack)) {
                if (isWhitelist) {
                    inFilter = true;
                    break;
                } else {
                    return new AcceptResult(ResultType.DENY_BLACKLIST, true);
                }
            }
        }

        if (block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, inFilter);
        } else if (!isEmpty && isWhitelist && !inFilter) {
            return new AcceptResult(ResultType.DENY_WHITELIST, false);
        } else {
            return new AcceptResult(ResultType.ACCEPT, inFilter);
        }
    }

    /**
     * Check whether or not an item stack matches the filter of this output
     * @param filter    The filter item to match against
     * @param item      The item stack to check
     * @return          <tt>true</tt> if the filter is similar; <tt>false</tt> if not
     */
    private boolean matchesFilter(ItemStack filter, ItemStack item) {
        if (filter == null || item == null) {
            return false;
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

        return true;
    }

    @Override
    protected IOption[] getOptions() {
        return Option.values();
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeOutput) o).facing) : ((PipeOutput) o).facing != null);
    }

    @Override
    public String toString() {
        return getType().toString() + getOptionsString();
    }

    public class AcceptResult {

        private final ResultType type;
        private final boolean inFilter;

        public AcceptResult(ResultType type, boolean inFilter) {
            this.type = type;
            this.inFilter = inFilter;
        }

        public ResultType getType() {
            return type;
        }

        public boolean isInFilter() {
            return inFilter;
        }
    }

    public enum ResultType {
        ACCEPT,
        DENY_REDSTONE,
        DENY_WHITELIST,
        DENY_BLACKLIST,
        DENY_INVALID;
    }

    public enum Option implements IOption {
        /**
         * Whether or not this output is in whitelist mode. This changes how the filter items are used.
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output is in whitelist mode and should only let items through that are in the inventory</li>
         * <li><tt>false</tt> if this output is in blacklist mode and should only let items through that are <strong>not</strong> in the inventory</li></p>
         */
        WHITELIST(Value.TRUE, Value.FALSE),
        /**
         * Whether or not this output can overflow into other available outputs
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output should force items to end up here even 'though the target is full</li>
         * <li><tt>false</tt> if the items should end up in the overflow</li></p>
         */
        OVERFLOW(Value.FALSE, Value.TRUE),
        /**
         * Whether or not to try to insert into the appropriate slots depending on the item type (like fuel)
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> Detect the item type and put it in the slot that it belongs to</li>
         * <li><tt>false</tt> Use the face that the output is facing to select the slot</li></p>
         */
        SMART_INSERT(Value.TRUE, Value.FALSE),
        /**
         * Whether or not to respect the material of the filter item when filtering.
         */
        MATERIAL_FILTER(Value.TRUE, Value.FALSE),
        /**
         * Whether or not to respect tool damage values when filtering
         */
        DAMAGE_FILTER(Value.FALSE, Value.TRUE),
        /**
         * Whether or not to respect custom item names and lores when filtering
         */
        DISPLAY_FILTER(Value.FALSE, Value.TRUE),
        /**
         * Whether or not to respect filter item amounts when filtering. Will only result in transfers that are multiple of the filter item's amount
         * TODO: Implementation
         */
        AMOUNT_FILTER(Value.TRUE, Value.FALSE);

        private final Value defaultValue;
        private final Class<?> valueType;
        private final Value[] possibleValues;

        /**
         * An option that this pipe output can have
         * @param defaultValue  The default value when none is set
         * @param valueType     The class of the values that this option accepts
         */
        Option(Value defaultValue, Class<?> valueType) {
            this.defaultValue = defaultValue;
            this.valueType = valueType;
            possibleValues = new Value[0];
        }

        /**
         * An option that this pipe output can have
         * @param possibleValues    An array of possible values that this option accepts
         * @throws IllegalArgumentException Thrown when there are less than two possible values defined
         */
        Option(Value... possibleValues) throws IllegalArgumentException {
            if (possibleValues.length < 2) {
                throw new IllegalArgumentException("An option needs to have at least two values!");
            }
            this.possibleValues = possibleValues;
            defaultValue = possibleValues[0];
            valueType = defaultValue.getValue().getClass();
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
    }
}
