package io.github.apfelcreme.Pipes.Pipe;

import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Nameable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Directional;

import java.util.ArrayList;
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
public class PipeOutput extends AbstractPipePart {

    private final BlockFace facing;
    private boolean whiteList = true;
    private boolean overflowAllowed = false;

    public final static int[] GUI_ITEM_SLOTS = {3,4,5,12,13,14,21,22,23};

    public PipeOutput(SimpleLocation location, BlockFace facing) {
        super(PipesItem.PIPE_OUTPUT, location);
        this.facing = facing;
    }

    public PipeOutput(Block block) {
        this(new SimpleLocation(block.getLocation()), ((Directional) block.getState().getData()).getFacing());
        if (block.getState() instanceof Nameable) {
            String hidden = PipesUtil.getHiddenString(((Nameable) block.getState()).getCustomName());
            if (hidden != null) {
                for (String group : hidden.split(",")) {
                    String[] parts = group.split("=");
                    if (parts.length < 2) {
                        continue;
                    }
                    if ("whitelist".equals(parts[0])) {
                        whiteList = Boolean.parseBoolean(parts[1]);
                    } else if ("overflow".equals(parts[0])) {
                        overflowAllowed = Boolean.parseBoolean(parts[1]);
                    }
                }
            }
        }
    }

    /**
     * returns the InventoryHolder
     *
     * @return the InventoryHolder
     */
    public InventoryHolder getTargetHolder() {
        Block block = getTargetLocation().getBlock();
        if (block != null && block.getState() instanceof InventoryHolder) {
            return (InventoryHolder) block.getState();
        }
        return null;
    }

    public SimpleLocation getTargetLocation() {
        return getLocation().getRelative(getFacing());
    }

    public BlockFace getFacing() {
        return facing;
    }

    /**
     * returns the list of items in the dropper
     *
     * @return the list of items in the dropper
     */
    public List<ItemStack> getFilterItems() {
        List<ItemStack> sorterItems = new ArrayList<>();
        InventoryHolder holder = getHolder();
        if (holder != null) {
            for (ItemStack itemStack : holder.getInventory().getContents()) {
                if (itemStack != null) {
                    sorterItems.add(itemStack);
                }
            }
        }
        return sorterItems;
    }

    /**
     * Check whether or not this output can accept that item stack
     * @param itemStack The item to check
     * @return  A result that represents why or why not the item is accepted by this output
     */
    public AcceptResult accepts(ItemStack itemStack) {
        Block block = getLocation().getBlock();
        if (block == null || !(block.getState() instanceof InventoryHolder)) {
            return new AcceptResult(ResultType.DENY_INVALID, false);
        }
        if (isOverflowAllowed() && block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, false);
        }

        boolean inFilter = false;
        boolean isEmpty = true;
        for (ItemStack filterItem : ((InventoryHolder) block.getState()).getInventory().getContents()) {
            isEmpty &= filterItem == null;
            if (PipesUtil.isSimilarFuzzy(filterItem, itemStack)) {
                if (isWhiteList()) {
                    inFilter = true;
                    break;
                } else {
                    return new AcceptResult(ResultType.DENY_BLACKLIST, true);
                }
            }
        }

        if (block.isBlockPowered()) {
            return new AcceptResult(ResultType.DENY_REDSTONE, inFilter);
        } else if (!isEmpty && isWhiteList() && !inFilter) {
            return new AcceptResult(ResultType.DENY_WHITELIST, false);
        } else {
            return new AcceptResult(ResultType.ACCEPT, inFilter);
        }
    }

    /**
     * Get if this output works in white- or blacklist mode. This changes how the filter items are used.
     * @return  <tt>true</tt> if this output is in whitelist mode; <tt>false</tt> if not
     */
    public boolean isWhiteList() {
        return whiteList;
    }

    /**
     * Set whether or not this output is in whitelist mode. This changes how the filter items are used.
     * @param whiteList <tt>true</tt> if this output is in whitelist mode; <tt>false</tt> if it is in blacklist mode
     */
    public void setWhiteList(boolean whiteList) {
        this.whiteList = whiteList;
        saveOptions();
    }

    /**
     * Get whether or not this output allows overflowing
     * @return  <tt>true</tt> if this output should force items to end up here even 'though the target is full;
     *          <tt>false</tt> if the items should end up in the overflow
     */
    public boolean isOverflowAllowed() {
        return overflowAllowed;
    }

    /**
     * Set whether or not this output can overflow
     * @param overflowAllowed   Whether or not this output oferflows if the target is full
     */
    public void setOverflowAllowed(boolean overflowAllowed) {
        this.overflowAllowed = overflowAllowed;
        saveOptions();
    }

    private void saveOptions() {
        BlockState state = getLocation().getBlock().getState();
        if (state.getType() == getType().getMaterial() && state instanceof Nameable) {
            ((Nameable) state).setCustomName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString(toString(), getType().getName()));
            state.update();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || super.equals(o)
                && !(facing != null ? !facing.equals(((PipeOutput) o).facing) : ((PipeOutput) o).facing != null);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(getType().toString());
        if (!whiteList) {
            s.append(",whitelist=" + whiteList);
        }
        if (overflowAllowed) {
            s.append(",overflow=" + overflowAllowed);
        }
        return s.toString();
    }

    public void showGui(Player player) {
        Inventory realInv = getHolder().getInventory();

        Inventory gui = Bukkit.createInventory(getHolder(), 27, realInv.getTitle());

        gui.setItem(0, getGuiWhitelistItem());
        gui.setItem(1, getGuiOverflowItem());

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, PipesConfig.getGuiFiller());
            }
        }

        for (int i = 0; i < GUI_ITEM_SLOTS.length; i++) {
            gui.setItem(GUI_ITEM_SLOTS[i], realInv.getItem(i));
        }
        player.openInventory(gui);
    }

    public ItemStack getGuiWhitelistItem() {
        ItemStack whitelistItem;
        if (isWhiteList()) {
            whitelistItem = new ItemStack(PipesConfig.getGuiEnabled());
            ItemMeta meta = whitelistItem.getItemMeta();
            meta.setDisplayName(PipesConfig.getText("gui.whitelist.enabled"));
            whitelistItem.setItemMeta(meta);
        } else {
            whitelistItem = new ItemStack(PipesConfig.getGuiDisabled());
            ItemMeta meta = whitelistItem.getItemMeta();
            meta.setDisplayName(PipesConfig.getText("gui.whitelist.disabled"));
            whitelistItem.setItemMeta(meta);
        }
        return whitelistItem;
    }

    public ItemStack getGuiOverflowItem() {
        ItemStack overflowItem;
        if (isOverflowAllowed()) {
            overflowItem = new ItemStack(PipesConfig.getGuiEnabled());
            ItemMeta meta = overflowItem.getItemMeta();
            meta.setDisplayName(PipesConfig.getText("gui.overflow.enabled"));
            overflowItem.setItemMeta(meta);
        } else {
            overflowItem = new ItemStack(PipesConfig.getGuiDisabled());
            ItemMeta meta = overflowItem.getItemMeta();
            meta.setDisplayName(PipesConfig.getText("gui.overflow.disabled"));
            overflowItem.setItemMeta(meta);
        }
        return overflowItem;
    }

    public class AcceptResult {

        private final ResultType type;
        private final boolean inFilter;

        public AcceptResult(ResultType type, boolean inFilter) {
            this.type = type;
            this.inFilter = inFilter;
        }

        public ResultType getType() {
            return type;
        }

        public boolean isInFilter() {
            return inFilter;
        }
    }

    public enum ResultType {
        ACCEPT,
        DENY_REDSTONE,
        DENY_WHITELIST,
        DENY_BLACKLIST,
        DENY_INVALID;
    }
}
