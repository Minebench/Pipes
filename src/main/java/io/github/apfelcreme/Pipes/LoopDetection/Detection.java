package io.github.apfelcreme.Pipes.LoopDetection;

import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;

import java.util.*;

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
public class Detection {

    private List<TickingLocation> tickingLocations;

    public Detection() {
        tickingLocations = new ArrayList<>();
    }

    /**
     * returns the list of found dispensers that were active while the detection was running
     *
     * @return the list of found dispensers that were active while the detection was running
     */
    public List<TickingLocation> getTickingLocations() {
        return tickingLocations;
    }

    /**
     * adds a location to the map of found locations
     *
     * @param location a location of a dispenser
     */
    public void addLocation(SimpleLocation location) {
        for (TickingLocation tickingLocation : tickingLocations) {
            if (tickingLocation.getLocation().equals(location)) {
                tickingLocation.increment();
                return;
            }
        }
        //no location found
        tickingLocations.add(new TickingLocation(location, 1));
    }
}
