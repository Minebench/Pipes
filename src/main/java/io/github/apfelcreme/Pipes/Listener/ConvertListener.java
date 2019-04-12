package io.github.apfelcreme.Pipes.Listener;

/*
 * Pipes
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.minebench.blockinfostorage.BlockInfoStorage;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
import io.github.apfelcreme.Pipes.Pipes;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.HashSet;
import java.util.Set;

public class ConvertListener implements Listener {
    private final Pipes plugin;

    private Set<Long> chunkIds = new HashSet<>();

    public ConvertListener(Pipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        long id = getChunkId(event.getChunk());
        if (!chunkIds.contains(id)) {
            chunkIds.add(id);
            for (BlockState state : event.getChunk().getTileEntities(false)) {
                AbstractPipePart part = PipeManager.getInstance().getPipePart(state.getBlock());
                if (part != null) {
                    BlockInfoStorage.get().setBlockInfo(state.getLocation(), AbstractPipePart.TYPE_KEY, part.getType().name());
                }
            }
        }
    }

    private long getChunkId(Chunk chunk) {
        return chunk.getWorld().getUID().hashCode() * 31 + chunk.getChunkKey();
    }
}
