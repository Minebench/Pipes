package io.github.apfelcreme.Pipes.Scheduler;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
public class ItemMoveScheduler {

    private int taskId = -1;

    private List<ScheduledItemTransfer> scheduledItemTransfers = new ArrayList<>();

    private int emptyRuns = 0;

    /**
     * starts a task
     */
    private void create() {
        taskId = Pipes.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(Pipes.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!scheduledItemTransfers.isEmpty()) {
                    for (ScheduledItemTransfer scheduledItemTransfer : scheduledItemTransfers) {
                        scheduledItemTransfer.execute();
                    }
                    scheduledItemTransfers.clear();
                } else {
                    emptyRuns++;
                    if (emptyRuns >= 3) {
                        kill();
                    }
                }
            }
        }, 20L, PipesConfig.getTransferCooldown());
    }

    /**
     * kills the task
     */
    private void kill() {
        Pipes.getInstance().getServer().getScheduler().cancelTask(taskId);
        taskId = -1;
        emptyRuns = 0;
    }

    /**
     * is the task running at the moment?
     *
     * @return true or false
     */
    public boolean isActive() {
        return taskId != -1;
    }

    /**
     * schedules an item move
     *
     * @param scheduledItemTransfer the item transfer
     */
    public void add(ScheduledItemTransfer scheduledItemTransfer) {
        emptyRuns = 0;
        scheduledItemTransfers.add(scheduledItemTransfer);
        if (!isActive()) {
            create();
        }
    }

}
