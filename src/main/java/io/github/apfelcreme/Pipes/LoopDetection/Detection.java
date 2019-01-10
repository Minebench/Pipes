package io.github.apfelcreme.Pipes.LoopDetection;

import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<SimpleLocation, TickingLocation> tickingLocations = new HashMap<>();

    /**
     * adds a location to the map of found locations
     *
     * @param location a location of a dispenser
     */
    public void addLocation(SimpleLocation location) {
        TickingLocation tickingLocation = tickingLocations.get(location);
        if (tickingLocation == null) {
            //no location found
            tickingLocations.put(location, new TickingLocation(location, 1));
        } else {
            tickingLocation.increment();
        }
    }

    /**
     * returns the sorted result
     *
     * @return the sorted result
     */
    public List<TickingLocation> getResult() {
        List<TickingLocation> result = new ArrayList<>(tickingLocations.values());
        Collections.sort(result);
        return result;
    }
}
