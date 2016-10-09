package io.github.apfelcreme.Pipes;

import io.github.apfelcreme.Pipes.Command.InfoCommand;
import io.github.apfelcreme.Pipes.Command.ReloadCommand;
import io.github.apfelcreme.Pipes.Command.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

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
public class PipeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        SubCommand subCommand = null;
        if (strings.length > 0) {
            Operation operation = Operation.getOperation(strings[0]);
            if (operation != null) {
                switch (operation) {
                    case INFO:
                        subCommand = new InfoCommand();
                        break;
                    case RELOAD:
                        subCommand = new ReloadCommand();
                        break;
                }
            }
            if (subCommand != null) {
                subCommand.execute(commandSender, strings);
            }
        }
        return true;
    }

    /**
     * all possible sub-commands for /rr
     *
     * @author Jan
     */
    public enum Operation {

        INFO,
        RELOAD;

        /**
         * returns the matching operation
         *
         * @param operationString the input
         * @return the matching enum constant or null
         */
        public static Operation getOperation(String operationString) {
            for (Operation operation : Operation.values()) {
                if (operation.name().equalsIgnoreCase(operationString)) {
                    return operation;
                }
            }
            return null;
        }
    }
}
