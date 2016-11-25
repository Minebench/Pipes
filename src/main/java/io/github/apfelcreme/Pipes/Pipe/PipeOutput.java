package io.github.apfelcreme.Pipes.Pipe;

import org.bukkit.block.Dropper;
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

    private final SimpleLocation dropperLocation;
    private final SimpleLocation inventoryHolderLocation;

    public PipeOutput(SimpleLocation dropperLocation, SimpleLocation inventoryHolderLocation) {
        this.dropperLocation = dropperLocation;
        this.inventoryHolderLocation = inventoryHolderLocation;
    }

    /**
     * returns the Dropper
     *
     * @return the Dropper
     */
    public Dropper getDropper() {
        if ((dropperLocation.getBlock() != null) && (dropperLocation.getBlock().getState() instanceof Dropper)) {
            return (Dropper) dropperLocation.getBlock().getState();
        }
        return null;
    }

    /**
     * returns the InventoryHolder
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getInventoryHolder() {
        if (inventoryHolderLocation.getBlock() != null) {
            return (InventoryHolder) inventoryHolderLocation.getBlock().getState();
        }
        return null;
    }

    /**
     * returns the dropper location
     *
     * @return the dropper location
     */
    public SimpleLocation getDropperLocation() {
        return dropperLocation;
    }

    /**
     * returns the inventoryholder location
     *
     * @return the inventoryholder location
     */
    public SimpleLocation getInventoryHolderLocation() {
        return inventoryHolderLocation;
    }

    /**
     * returns the list of items in the dropper
     *
     * @return the list of items in the dropper
     */
    public List<ItemStack> getFilterItems() {
        List<ItemStack> sorterItems = new ArrayList<>();
        for (ItemStack itemStack : getDropper().getInventory()) {
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

        if (dropperLocation != null ? !dropperLocation.equals(that.dropperLocation) : that.dropperLocation != null)
            return false;
        return !(inventoryHolderLocation != null ? !inventoryHolderLocation.equals(that.inventoryHolderLocation) : that.inventoryHolderLocation != null);

    }
}
