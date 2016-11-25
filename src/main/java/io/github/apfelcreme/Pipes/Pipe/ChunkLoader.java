package io.github.apfelcreme.Pipes.Pipe;

import org.bukkit.block.Furnace;

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
public class ChunkLoader {

    private SimpleLocation chunkLoaderLocation;

    public ChunkLoader(SimpleLocation chunkLoaderLocation) {
        this.chunkLoaderLocation = chunkLoaderLocation;
    }


    /**
     * returns the chunkLoader
     *
     * @return the chunkLoader
     */
    public Furnace getChunkLoader() {
        if ((chunkLoaderLocation.getBlock() != null) && (chunkLoaderLocation.getBlock().getState() instanceof Furnace)) {
            return (Furnace) chunkLoaderLocation.getBlock().getState();
        }
        return null;
    }

    /**
     * returns the chunkLoader location
     *
     * @return the chunkLoader location
     */
    public SimpleLocation getChunkLoaderLocation() {
        return chunkLoaderLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkLoader chunkLoader = (ChunkLoader) o;

        return !(chunkLoaderLocation != null ? !chunkLoaderLocation.equals(chunkLoader.chunkLoaderLocation) : chunkLoader.chunkLoaderLocation != null);

    }

}
