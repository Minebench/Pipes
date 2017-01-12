package io.github.apfelcreme.Pipes.Event;

/*
 * Copyright 2017 Max Lee (https://github.com/Phoenix616/)
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

import io.github.apfelcreme.Pipes.Pipe.Pipe;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PipeMoveItemEvent extends InventoryMoveItemEvent {
    private final Pipe pipe;

    public PipeMoveItemEvent(Pipe pipe, Inventory sourceInventory, ItemStack itemStack, Inventory destinationInventory, boolean didSourceInitiate) {
        super(sourceInventory, itemStack, destinationInventory, didSourceInitiate);
        this.pipe = pipe;
    }

    public Pipe getPipe() {
        return pipe;
    }
}
