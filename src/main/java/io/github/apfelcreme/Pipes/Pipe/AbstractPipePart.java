package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;

/**
 * Copyright (C) 2017 Phoenix616 aka Max Lee
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
 */
public abstract class AbstractPipePart {

    private final PipesItem type;
    private final SimpleLocation location;

    protected AbstractPipePart(PipesItem type, SimpleLocation location) {
        this.type = type;
        this.location = location;
    }

    /**
     * Get the type of this pipe part
     *
     * @return The type of this pipe part
     */
    public PipesItem getType() {
        return type;
    }

    /**
     * returns the location of this pipe part
     *
     * @return the location of this pipe part
     */
    public SimpleLocation getLocation() {
        return location;
    }

    /**
     * returns the inventory holder of pipe part
     *
     * @return the inventory holder of pipe part
     */
    public InventoryHolder getHolder() {
        Block block = location.getBlock();
        if (type.check(block)) {
            return (InventoryHolder) block.getState();
        }
        return null;
    }

    /**
     * Check whether or not this part is powered
     * @return true if powered or when there doesn't exist a block anymore at that point
     */
    public boolean isPowered() {
        InventoryHolder holder = getHolder();
        return holder == null || ((BlockState) holder).getBlock().isBlockPowered();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getLocation() != null ? !getLocation().equals(((AbstractPipePart) o).getLocation()) : ((AbstractPipePart) o).getLocation() != null;
    }
}
