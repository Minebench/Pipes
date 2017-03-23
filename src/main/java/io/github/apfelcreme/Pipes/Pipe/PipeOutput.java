package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
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

    private final BlockFace facing;

    public PipeOutput(SimpleLocation location, BlockFace facing) {
        super(PipesItem.PIPE_OUTPUT, location);
        this.facing = facing;
    }

    public PipeOutput(Block block) {
        this(new SimpleLocation(block.getLocation()), ((Directional) block.getState()).getFacing());
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

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeOutput) o).facing) : ((PipeOutput) o).facing != null);
    }
}
