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

    /**
     * a list of locations that were found
     */
    private List<TickingLocation> tickingLocations;

    /**
     * constructor
     */
    public Detection() {
        tickingLocations = new ArrayList<>();
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

    /**
     * returns the sorted result
     *
     * @return the sorted result
     */
    public List<TickingLocation> getResult() {
        List<TickingLocation> result = new ArrayList<>(tickingLocations);
        Collections.sort(result);
        return result;
    }
}
