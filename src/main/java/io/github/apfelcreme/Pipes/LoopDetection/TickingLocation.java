package io.github.apfelcreme.Pipes.LoopDetection;

import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;

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
public class TickingLocation implements Comparable<TickingLocation> {

    private SimpleLocation location;
    private int timesTicked;

    public TickingLocation(SimpleLocation location, int timeTicked) {
        this.location = location;
        this.timesTicked = timeTicked;
    }

    /**
     * returns the location
     *
     * @return the location
     */
    public SimpleLocation getLocation() {
        return location;
    }

    /**
     * returns the time the location was found already
     *
     * @return the time the location was found
     */
    public int getTimesTicked() {
        return timesTicked;
    }

    /**
     * increases the counter
     */
    public void increment() {
        timesTicked++;
    }

    /**
     * compares this tickingLocation to another tickingLocation
     *
     * @param other the other ticking location
     * @return -1 if this one is smaller, 0 if they're equal, 1 if this one is bigger
     */
    @Override
    public int compareTo(TickingLocation other) {
        return Integer.compare(other.getTimesTicked(), this.timesTicked);
    }
}
