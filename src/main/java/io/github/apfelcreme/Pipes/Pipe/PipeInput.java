package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Directional;

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
public class PipeInput extends AbstractPipePart {

    private final BlockFace facing;

    public PipeInput(Block block) {
        super(PipesItem.PIPE_INPUT, block);
        this.facing = ((Directional) block.getState().getData()).getFacing();
    }

    public SimpleLocation getTargetLocation() {
        return getLocation().getRelative(getFacing());
    }

    public BlockFace getFacing() {
        return facing;
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
                && !(facing != null ? !facing.equals(((PipeInput) o).facing) : ((PipeInput) o).facing != null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public enum Option implements IOption {
        /**
         * Whether or not to spread the items equally over all outputs
         */
        SPREAD(Value.FALSE, Value.TRUE);

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
