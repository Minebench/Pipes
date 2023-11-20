package io.github.apfelcreme.Pipes.Command;

import io.github.apfelcreme.Pipes.LoopDetection.Detection;
import io.github.apfelcreme.Pipes.Manager.DetectionManager;
import io.github.apfelcreme.Pipes.LoopDetection.TickingLocation;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class TpCommand implements SubCommand {

    /**
     * executes the command
     *
     * @param commandSender the sender
     * @param strings       the command args
     */
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;
            if (player.hasPermission("Pipes.tp")) {
                if (strings.length > 1 && PipesUtil.isNumeric(strings[1])) {
                    int target = Integer.parseInt(strings[1]);
                    Detection detection = DetectionManager.getInstance().getDetection(commandSender);
                    if (detection != null) {
                        List<TickingLocation> result = detection.getResult();
                        if (target < result.size()) {
                            TickingLocation targetLocation = result.get(target);
                            Location location = new Location(
                                    Pipes.getInstance().getServer().getWorld(targetLocation.getLocation().getWorldName()),
                                    targetLocation.getLocation().getX() + 0.5,
                                    targetLocation.getLocation().getY() + 1,
                                    targetLocation.getLocation().getZ() + 0.5);
                            player.teleport(location);
                            Pipes.sendMessage(commandSender, PipesConfig.getText(player, "info.tp.teleported"));
                        } else {
                            Pipes.sendMessage(commandSender, PipesConfig.getText(player, "error.notThatManyResults"));
                        }
                    } else {
                        Pipes.sendMessage(commandSender, PipesConfig.getText(player, "error.noDetection"));
                    }
                } else {
                    Pipes.sendMessage(commandSender, PipesConfig.getText(player, "error.wrongUsage.tp"));
                }
            } else {
                Pipes.sendMessage(commandSender, PipesConfig.getText(player, "error.noPermission"));
            }
        } else {
            Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "error.commandCanOnlyBeRunByAPlayer"));
        }
    }
}
