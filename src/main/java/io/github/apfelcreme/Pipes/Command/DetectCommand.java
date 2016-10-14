package io.github.apfelcreme.Pipes.Command;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.LoopDetection.TickingLocation;
import io.github.apfelcreme.Pipes.Pipe.SimpleLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.Collections;
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
public class DetectCommand implements SubCommand {

    /**
     * executes the command
     *
     * @param commandSender the sender
     * @param strings       the command args
     */
    @Override
    public void execute(final CommandSender commandSender, String[] strings) {
        if (commandSender.hasPermission("Pipes.detect")) {
            long duration = 200L;
            if (strings.length > 1 && PipesUtil.isNumeric(strings[1])) {
                duration = 20L * Integer.parseInt(strings[1]);
            }
            Pipes.getInstance().getRunningDetections().put(commandSender, new Detection());
            Pipes.sendMessage(commandSender, PipesConfig.getText("info.detect.started")
                    .replace("{0}", new DecimalFormat("0").format(duration / 20)));
            Pipes.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(Pipes.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Detection detection = Pipes.getInstance().getRunningDetections().get(commandSender);
                    if (detection != null) {
                        List<TickingLocation> tickingLocations = detection.getTickingLocations();
                        Collections.sort(tickingLocations);
                        Pipes.sendMessage(commandSender, PipesConfig.getText("info.detect.finished"));
                        int i = 1;
                        for (TickingLocation tickingLocation : tickingLocations) {
                            Pipes.sendMessage(commandSender, PipesConfig.getText("info.detect.element")
                                    .replace("{0}", String.valueOf(i))
                                    .replace("{1}", String.valueOf(tickingLocation.getLocation().getWorldName()))
                                    .replace("{2}", String.valueOf(tickingLocation.getLocation().getX()))
                                    .replace("{3}", String.valueOf(tickingLocation.getLocation().getY()))
                                    .replace("{4}", String.valueOf(tickingLocation.getLocation().getZ()))
                                    .replace("{5}", String.valueOf(tickingLocation.getTimesTicked())));
                            i++;
                        }
                    }
                }
            }, duration);
        } else {
            Pipes.sendMessage(commandSender, PipesConfig.getText("error.noPermission"));
        }
    }
}
