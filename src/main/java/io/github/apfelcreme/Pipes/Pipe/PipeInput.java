package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Directional;

import java.util.HashMap;
import java.util.Map;

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
public class PipeInput extends AbstractPipePart {

    public final static String[] GUI_SETUP = {
            "ss iii   ",
            "ss iii c ",
            "ss iii   "
    };

    private final BlockFace facing;

    public PipeInput(Block block) {
        super(PipesItem.PIPE_INPUT, block);
        this.facing = ((Directional) block.getState(false).getData()).getFacing();
    }

    public SimpleLocation getTargetLocation() {
        return getLocation().getRelative(getFacing());
    }

    public BlockFace getFacing() {
        return facing;
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
                && !(facing != null ? !facing.equals(((PipeInput) o).facing) : ((PipeInput) o).facing != null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static class Options extends OptionsList {

        private static final Map<String, Option<?>> VALUES = new HashMap<>();

        /**
         * Whether or not to spread the items equally over all outputs
         */
        public static final Option<Boolean> SPREAD = add(new Option<>("SPREAD", Value.FALSE, Value.TRUE));
        /**
         * Whether or not this transfers from this input can overflow into other available outputs
         * <p><strong>Possible Values:</strong></p>
         * <ul>
         * <li><code>true</code> if the items should end up in the overflow</li>
         * <li><code>false</code> if this output should force items to end up in the filtered output even 'though the target is full</li>
         * </ul>
         */
        public static final Option<Boolean> OVERFLOW = add(new Option<>("OVERFLOW", Value.FALSE, Value.TRUE));
        /**
         * Whether to merge item stacks in the input after a transfer attempt or not
         */
        public static final Option<Boolean> MERGE = add(new Option<>("MERGE", Value.TRUE, Value.FALSE));

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
    }
}
