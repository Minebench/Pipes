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

    public PipeInput(SimpleLocation location, BlockFace facing) {
        super(PipesItem.PIPE_INPUT, location);
        this.facing = facing;
    }

    public PipeInput(Block block) {
        this(new SimpleLocation(block.getLocation()), ((Directional) block.getState()).getFacing());
    }

    public SimpleLocation getTargetLocation() {
        return getLocation().getRelative(getFacing());
    }

    public BlockFace getFacing() {
        return facing;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeInput) o).facing) : ((PipeInput) o).facing != null);
    }
}
