package io.github.apfelcreme.Pipes.Listener;

import io.github.apfelcreme.Pipes.Pipe.AbstractPipePart;
import io.github.apfelcreme.Pipes.Pipe.PipeOutput;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;

/**
 * Copyright (C) 2017 Phoenix616 aka Max Lee
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
 */
public class GuiListener implements Listener {

    private final Pipes plugin;

    public GuiListener(Pipes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof BlockState) {
            BlockState state = (BlockState) holder;
            AbstractPipePart pipePart = PipesUtil.getPipesPart(state.getBlock());
            if (pipePart != null) {
                switch (pipePart.getType()) {
                    case PIPE_OUTPUT:
                        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                            event.setCancelled(true);
                            return;
                        }
                        PipeOutput pipeOutput = (PipeOutput) pipePart;
                        if (event.getClickedInventory() == event.getView().getTopInventory()) {
                            if (event.getSlot() == 0) {
                                event.setCancelled(true);
                                pipeOutput.setWhiteList(!pipeOutput.isWhiteList());
                                event.setCurrentItem(pipeOutput.getGuiWhitelistItem());
                            } else if (event.getSlot() == 1) {
                                event.setCancelled(true);
                                pipeOutput.setOverflowAllowed(!pipeOutput.isOverflowAllowed());
                                event.setCurrentItem(pipeOutput.getGuiOverflowItem());
                            } else if (Arrays.binarySearch(PipeOutput.GUI_ITEM_SLOTS, event.getSlot()) >= 0) {

                            } else if (event.getCurrentItem().isSimilar(PipesConfig.getGuiFiller())) {
                                event.setCancelled(true);
                            }
                        }
                        break;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof BlockState) {
            BlockState state = (BlockState) holder;
            AbstractPipePart pipePart = PipesUtil.getPipesPart(state.getBlock());
            if (pipePart != null) {
                switch (pipePart.getType()) {
                    case PIPE_OUTPUT:
                        if (event.getInventory().getType() == InventoryType.CHEST) {
                            for (int slot : event.getRawSlots()) {
                                if (slot < event.getView().getTopInventory().getSize() && Arrays.binarySearch(PipeOutput.GUI_ITEM_SLOTS, slot) < 0) {
                                    event.setCancelled(true);
                                    break;
                                }
                            }
                        }
                        break;
                }
            }
        }

    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof BlockState) {
            BlockState state = (BlockState) holder;
            AbstractPipePart pipePart = PipesUtil.getPipesPart(state.getBlock());
            if (pipePart != null) {
                switch (pipePart.getType()) {
                    case PIPE_OUTPUT:
                        storeFilterItems(event.getView().getTopInventory());
                        break;
                }
            }
        }
    }

    private void storeFilterItems(Inventory inventory) {
        for (int i = 0; i < PipeOutput.GUI_ITEM_SLOTS.length; i++) {
            inventory.getHolder().getInventory().setItem(i, inventory.getItem(PipeOutput.GUI_ITEM_SLOTS[i]));
        }
    }
}
