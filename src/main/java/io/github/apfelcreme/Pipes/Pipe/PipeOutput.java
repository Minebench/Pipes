package io.github.apfelcreme.Pipes.Pipe;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.GuiStorageElement;
import de.themoep.inventorygui.InventoryGui;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.ChatColor;
import org.bukkit.Nameable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Directional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Map<Option, Option.Value> options = new HashMap<>();

    public final static String[] GUI_SETUP = {
            "sssiiisss",
            "sssiiisss",
            "sssiiisss"
    };

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
                        options.put(option, new Option.Value<>(Boolean.parseBoolean(parts[1])));
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
            if (PipesUtil.isSimilarFuzzy(filterItem, itemStack)) {
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
     * Get a certain option of this output
     * @param option    The option to get
     * @return          The value of the option or <tt>null</tt> if it wasn't set and there is no default one
     */
    public Object getOption(Option option) {
        return getOption(option, option.getDefaultValue());
    }

    /**
     * Get a certain option of this output
     * @param option        The option to get
     * @param defaultValue  The default value to return if the value wasn't found
     * @return              The value of the option or <tt>null</tt> if it wasn't set
     */
    public Object getOption(Option option, Option.Value defaultValue) {
        Option.Value value = options.getOrDefault(option, defaultValue);
        return value != null ? value.getValue() : null;
    }

    /**
     * Set an option of this output. This also saves the options to the block
     * @param option    The option to set
     * @param value     The value to set the option to
     * @throws IllegalArgumentException When the values type is not compatible with the option
     */
    public void setOption(Option option, Option.Value value) throws IllegalArgumentException {
        if (!option.isValid(value)) {
            throw new IllegalArgumentException("The option " + option + "< " + option.getValueType().getSimpleName() + "> does not accept the value " + value + " values!");
        }
        options.put(option, value);
        saveOptions();
    }

    private void saveOptions() {
        BlockState state = getLocation().getBlock().getState();
        if (state.getType() == getType().getMaterial() && state instanceof Nameable) {
            ((Nameable) state).setCustomName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString(toString(), getType().getName()));
            state.update();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeOutput) o).facing) : ((PipeOutput) o).facing != null);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getType().toString());
        for (Map.Entry<Option, Option.Value> option : options.entrySet()) {
            s.append(',').append(option.getKey()).append('=').append(option.getValue());
        }
        return s.toString();
    }

    public void showGui(Player player) {
        InventoryHolder holder = getHolder();
        if (holder == null) {
            return;
        }

        InventoryGui gui = InventoryGui.get(holder);
        if (gui == null) {
            gui = new InventoryGui(Pipes.getInstance(), holder, holder.getInventory().getTitle(), GUI_SETUP);

            gui.addElement(new GuiStorageElement('i', holder.getInventory()));
            gui.setFiller(PipesConfig.getGuiFiller());
            GuiElementGroup optionsGroup = new GuiElementGroup('s');
            for (Option option : Option.values()) {
                optionsGroup.addElement(option.getElement(this));
            }
            optionsGroup.addElement(gui.getFiller());
        }
        gui.show(player);
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

    public enum Option {
        /**
         * Whether or not this output is in whitelist mode. This changes how the filter items are used.
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output is in whitelist mode and should only let items through that are in the inventory</li>
         * <li><tt>false</tt> if this output is in blacklist mode and should only let items through that are <strong>not</strong> in the inventory</li></p>
         */
        WHITELIST(Value.TRUE, Boolean.class),
        /**
         * Whether or not this output can overflow into other available outputs
         * <p><strong>Possible Values:</strong>
         * <li><tt>true</tt> if this output should force items to end up here even 'though the target is full</li>
         * <li><tt>false</tt> if the items should end up in the overflow</li></p>
         */
        OVERFLOW(Value.FALSE, Boolean.class);

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

        public boolean isValid(Value value) {
            if (value.getValue().getClass() != getValueType()) {
                return false;
            }
            if (possibleValues.length == 0) {
                return true;
            }
            for (Value v : possibleValues) {
                if (v.getValue().equals(value.getValue())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the class of the values that this option accepts
         * @return  The class of the values that this option accepts
         */
        public Class<?> getValueType() {
            return valueType;
        }

        /**
         * Get the default value for this option if it isn't set
         * @return  The default value of this option
         */
        public Value getDefaultValue() {
            return defaultValue;
        }

        /**
         * Get the array of possible values
         * @return  The array of possible values
         */
        public Value[] getPossibleValues() {
            return possibleValues;
        }

        /**
         * Get the GUI element of this option for a certain output
         * @param output    The output to get the element for
         * @return          The GuiStateElement of this option
         */
        public GuiStateElement getElement(PipeOutput output) {
            Object value = output.getOption(this);
            List<GuiStateElement.State> states = new ArrayList<>();
            int index = 0;
            for (int i = 0; i < possibleValues.length; i++) {
                if (possibleValues[i].getValue().equals(value)) {
                    index = i;
                    break;
                }
            }
            Value nextValue = possibleValues.length > index + 1 ? possibleValues[index + 1] : possibleValues[0];
            for (Value v : possibleValues) {
                states.add(new GuiStateElement.State(
                        click -> output.setOption(this, nextValue),
                        v.toString(),
                        PipesConfig.getItemStack("gui." + name().toLowerCase() + "." + v.toString()),
                        PipesConfig.getText("gui." + name().toLowerCase() + "." + v.toString())
                ));
            }
            return new GuiStateElement(name().charAt(0), index, states.toArray(new GuiStateElement.State[states.size()]));
        }

        public static class Value<T> {
            public static final Value TRUE = new Value<>(true);
            public static final Value FALSE = new Value<>(false);

            private final T value;

            public Value(T value) {
                this.value = value;
            }

            public T getValue() {
                return value;
            }

            public String toString() {
                return "Value<" + value.getClass().getSimpleName() + ">{value=" + value.toString() + "}";
            }
        }
    }
}
