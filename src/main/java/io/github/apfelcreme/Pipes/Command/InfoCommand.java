package io.github.apfelcreme.Pipes.Command;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class InfoCommand implements SubCommand {

    /**
     * executes the command
     *
     * @param commandSender the sender
     * @param strings       the command args
     */
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (commandSender instanceof Player) {
            final Player player = (Player) commandSender;
            if (player.hasPermission("Pipes.info")) {
                Pipes.getInstance().registerRightClick(player, "info");
                Pipes.sendMessage(player, PipesConfig.getText(player, "info.info.cooldownStarted"));
            } else {
                Pipes.sendMessage(player, PipesConfig.getText(player, "error.noPermission"));
            }
        } else {
            Pipes.sendMessage(commandSender, PipesConfig.getText(commandSender, "error.commandCanOnlyBeRunByAPlayer"));
        }
    }
}
