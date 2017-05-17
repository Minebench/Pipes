package io.github.apfelcreme.Pipes.Event;

import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
public class PipeDispenseEvent extends BlockDispenseEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Pipe pipe;
    private final PipeOutput output;

    public PipeDispenseEvent(Pipe pipe, PipeOutput output, ItemStack transferring, Vector motion) {
        super(output.getLocation().getBlock(), transferring, motion);
        this.pipe = pipe;
        this.output = output;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public PipeOutput getOutput() {
        return output;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
