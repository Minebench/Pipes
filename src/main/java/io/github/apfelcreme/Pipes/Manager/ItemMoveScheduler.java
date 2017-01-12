package io.github.apfelcreme.Pipes.Manager;

import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.Pipe.ScheduledItemTransfer;

import java.util.*;

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

    /**
     * the task id of the repeating task
     */
    private int taskId;

    /**
     * the queue that holds the items that are waiting to be transferred
     */
    private List<ScheduledItemTransfer> scheduledItemTransfers;

    /**
     * the number of consecutive runs without any transfers (cancels at 4)
     */
    private int emptyRuns;

    /**
     * the scheduler instance
     */
    private static ItemMoveScheduler instance = null;

    private ItemMoveScheduler() {
        taskId = -1;
        scheduledItemTransfers = new ArrayList<>();
        emptyRuns = 0;
    }

    /**
     * returns the scheduler instance
     *
     * @return the scheduler instance
     */
    public static ItemMoveScheduler getInstance() {
        if (instance == null) {
            instance = new ItemMoveScheduler();
        }
        return instance;
    }

    /**
     * starts a task
     */
    private void create() {
        taskId = Pipes.getInstance().getServer().getScheduler().scheduleSyncRepeatingTask(Pipes.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!scheduledItemTransfers.isEmpty()) {
                    Iterator<ScheduledItemTransfer> transfers = scheduledItemTransfers.iterator();
                    while (transfers.hasNext()) {
                        transfers.next().execute();
                        transfers.remove();
                    }
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
        if (!scheduledItemTransfers.contains(scheduledItemTransfer)) {
            scheduledItemTransfers.add(scheduledItemTransfer);
        }
        if (!isActive() && !scheduledItemTransfers.isEmpty()) {
            create();
        }
    }

}
