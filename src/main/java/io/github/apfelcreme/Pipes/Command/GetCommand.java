package io.github.apfelcreme.Pipes.Command;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Copyright (C) 2017 Max Lee (https://github.com/Phoenix616)
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
 * @author Max Lee (https://github.com/Phoenix616)
 */
public class GetCommand implements SubCommand {

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
            if (player.hasPermission("Pipes.get")) {
                if (strings.length > 1) {
                    try {
                        PipesItem pipesItem = PipesItem.valueOf(strings[1].toUpperCase());
                        if (pipesItem != PipesItem.CHUNK_LOADER || player.hasPermission("Pipes.placeChunkLoader")) {
                            if (player.getInventory().addItem(pipesItem.toItemStack()).isEmpty()) {
                                Pipes.sendMessage(player, PipesConfig.getText("info.get", pipesItem.getName()));
                            } else {
                                Pipes.sendMessage(player, PipesConfig.getText("error.notEnoughInventorySpace"));
                            }
                        } else {
                            Pipes.sendMessage(player, PipesConfig.getText("error.noPermission"));
                        }
                    } catch (IllegalArgumentException e) {
                        Pipes.sendMessage(commandSender, PipesConfig.getText("error.unknownPipesItem", strings[1].toLowerCase()));
                    }
                } else {
                    Pipes.sendMessage(commandSender, PipesConfig.getText("error.wrongUsage.get"));
                }
            } else {
                Pipes.sendMessage(player, PipesConfig.getText("error.noPermission"));
            }
        } else {
            Pipes.sendMessage(commandSender, PipesConfig.getText("error.commandCanOnlyBeRunByAPlayer"));
        }
    }
}
