package io.github.apfelcreme.Pipes.Exception;

import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;

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
public class ChunkNotLoadedException extends Exception {

    private SimpleLocation accessedLocation;

    public ChunkNotLoadedException(SimpleLocation accessedLocation) {
        this.accessedLocation = accessedLocation;
    }

    /**
     * returns the position from which the chunk was accessed
     * @return the position from which the chunk was accessed
     */
    public SimpleLocation getAccessedLocation() {
        return accessedLocation;
    }
}
