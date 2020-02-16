package io.github.apfelcreme.Pipes.Manager;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

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
public class DetectionManager {

    /**
     * the DetectionManager instance
     */
    private static DetectionManager instance = null;

    /**
     * a map of all detections
     */
    private Map<CommandSender, Detection> detections;

    /**
     * constructor
     */
    private DetectionManager() {
        detections = new HashMap<>();
    }

    /**
     * adds a new detection
     *
     * @param sender the sender of the command
     */
    public void createDetection(CommandSender sender) {
        detections.put(sender, new Detection());
    }

    /**
     * returns a senders latest detection
     *
     * @param sender a sender
     * @return his latest detection
     */
    public Detection getDetection(CommandSender sender) {
        return detections.get(sender);
    }

    /**
     * returns a list of all detections
     *
     * @return a list of all detections
     */
    public Map<CommandSender, Detection> getDetections() {
        return detections;
    }

    /**
     * returns the DetectionManager instance
     *
     * @return the DetectionManager instance
     */
    public static DetectionManager getInstance() {
        if (instance == null) {
            instance = new DetectionManager();
        }
        return instance;
    }
}
