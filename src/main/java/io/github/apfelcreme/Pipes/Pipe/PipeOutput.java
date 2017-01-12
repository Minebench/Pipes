package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

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
public class PipeOutput {

    private final SimpleLocation location;
    private final SimpleLocation targetLocation;

    public PipeOutput(SimpleLocation dropperLocation, SimpleLocation inventoryHolderLocation) {
        this.location = dropperLocation;
        this.targetLocation = inventoryHolderLocation;
    }

    /**
     * returns the inventory holder blockstate of the block
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getOutputHolder() {
        Block block = location.getBlock();
        if (PipesItem.PIPE_OUTPUT.check(block)) {
            return (InventoryHolder) block.getState();
        }
        return null;
    }

    /**
     * returns the InventoryHolder
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getTargetHolder() {
        Block block = targetLocation.getBlock();
        if (block != null && block.getState() instanceof InventoryHolder) {
            return (InventoryHolder) block.getState();
        }
        return null;
    }

    /**
     * returns the location of this output block
     *
     * @return the location of the output block
     */
    public SimpleLocation getLocation() {
        return location;
    }

    /**
     * returns the inventoryholder location
     *
     * @return the inventoryholder location
     */
    public SimpleLocation getTargetLocation() {
        return targetLocation;
    }

    /**
     * returns the list of items in the dropper
     *
     * @return the list of items in the dropper
     */
    public List<ItemStack> getFilterItems() {
        List<ItemStack> sorterItems = new ArrayList<>();
        for (ItemStack itemStack : getOutputHolder().getInventory()) {
            if (itemStack != null) {
                sorterItems.add(itemStack);
            }
        }
        return sorterItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PipeOutput that = (PipeOutput) o;

        if (location != null ? !location.equals(that.location) : that.location != null)
            return false;
        return !(targetLocation != null ? !targetLocation.equals(that.targetLocation) : that.targetLocation != null);

    }
}
