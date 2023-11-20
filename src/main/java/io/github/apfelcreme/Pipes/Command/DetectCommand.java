package io.github.apfelcreme.Pipes.Command;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.LoopDetection.TickingLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.List;

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
            DetectionManager.getInstance().createDetection(commandSender);
            Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "info.detect.started",
                    new DecimalFormat("0").format(duration / 20)));
            Pipes.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(Pipes.getInstance(), () -> {
                Detection detection = DetectionManager.getInstance().getDetection(commandSender);
                if (detection != null) {
                    List<TickingLocation> result = detection.getResult();
                    if (!result.isEmpty()) {
                        Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "info.detect.finished"));
                        int i = 0;
                        for (TickingLocation tickingLocation : result) {
                            Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "info.detect.element",
                                    String.valueOf(i),
                                    String.valueOf(tickingLocation.getLocation().getWorldName()),
                                    String.valueOf(tickingLocation.getLocation().getX()),
                                    String.valueOf(tickingLocation.getLocation().getY()),
                                    String.valueOf(tickingLocation.getLocation().getZ()),
                                    String.valueOf(tickingLocation.getTimesTicked())));
                            i++;
                        }
                    } else {
                        Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "info.detect.noElements"));
                    }
                }
            }, duration);
        } else {
            Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "error.noPermission"));
        }
    }
}
