package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Exception.ChunkNotLoadedException;
import io.github.apfelcreme.Pipes.Exception.PipeTooLongException;
import io.github.apfelcreme.Pipes.Exception.TooManyOutputsException;
import io.github.apfelcreme.Pipes.Manager.PipeManager;
import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
import io.github.apfelcreme.Pipes.Pipe.Pipe;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

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
public class PlayerListener implements Listener {

    private final Pipes plugin;

    public PlayerListener(Pipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String action = plugin.getRegisterRightClick(event.getPlayer());
            if ("info".equals(action)) {
                event.setCancelled(true);
                try {
                    plugin.unregisterRightClick(event.getPlayer());
                    Set<Pipe> pipes = PipeManager.getInstance().getPipes(event.getClickedBlock());
                    for (Pipe pipe : pipes) {
                        Pipes.sendMessage(event.getPlayer(), pipe.getString());
                        pipe.highlight(event.getPlayer());
                    }
                    if (pipes.isEmpty()) {
                        Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.noPipe"));
                    }
                } catch (ChunkNotLoadedException e) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.chunkNotLoaded"));
                } catch (TooManyOutputsException e) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.tooManyOutputs",
                            String.valueOf(PipesConfig.getMaxPipeOutputs())));
                } catch (PipeTooLongException e) {
                    Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("error.pipeTooLong",
                            String.valueOf(PipesConfig.getMaxPipeLength())));
                }
            } else if (!event.isCancelled() && (!event.getPlayer().isSneaking() || (
                    (event.getItem() == null || !event.getItem().getType().isBlock() && !event.getItem().getType().isEdible())
                            && !event.getPlayer().hasPermission("Pipes.gui.bypass")))) {
                AbstractPipePart pipePart = PipeManager.getInstance().getPipePart(event.getClickedBlock());
                if (pipePart != null) {
                    event.setCancelled(true);
                    pipePart.showGui(event.getPlayer());
                }
            }

        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking() && event.getPlayer().hasPermission("Pipes.applybook")) {
            if (!PipesItem.SETTINGS_BOOK.check(event.getItem())) {
                return;
            }
            AbstractPipePart part = PipeManager.getInstance().getPipePart(event.getClickedBlock());
            if (part == null) {
                return;
            }
            event.setCancelled(true);
            try {
                part.applyBook(event.getItem());
                Pipes.sendMessage(event.getPlayer(), PipesConfig.getText("info.settings.bookApplied"));
            } catch (IllegalArgumentException e){
                Pipes.sendMessage(event.getPlayer(), e.getMessage());
            }
        }
    }
}
