package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

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
public class ChunkLoader extends AbstractPipePart {

    public final static String[] GUI_SETUP = {
            "sss i zzz",
            "sss   zzz",
            "sss c zzz"
    };

    public ChunkLoader(BlockState state) {
        super(PipesItem.CHUNK_LOADER, state.getLocation());
    }

    @Override
    public void showGui(Player player) {
        InventoryHolder holder = getHolder();
        if (holder != null) {
            player.openInventory(holder.getInventory());
        }
    }

    @Override
    public String[] getGuiSetup() {
        return new String[0];
    }

    @Override
    protected Option<?>[] getOptions() {
        return new Option<?>[0];
    }

    @Override
    public boolean equals(Object o) {
        return this == o || super.equals(o);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
