package io.github.apfelcreme.Pipes;

/**
 * Copyright (C) 2016 Max lLe aka Phoenix616
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
 * @author Max lee aka Phoenix616
 */

import de.minebench.blockinfostorage.BlockInfoStorage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;

import java.util.Arrays;
import java.util.List;

public enum PipesItem {
    PIPE_INPUT(Material.DISPENSER),
    PIPE_OUTPUT(Material.DROPPER),
    CHUNK_LOADER(Material.FURNACE),
    SETTINGS_BOOK(Material.WRITTEN_BOOK);

    private static final String IDENTIFIER = "Pipes";
    private static final NamespacedKey TYPE_KEY = new NamespacedKey(Pipes.getInstance(), "type");

    private final Material material;
    private ItemStack item;

    PipesItem(Material material) {
        this.material = material;
    }

    public static String getIdentifier() {
        return IDENTIFIER;
    }

    public String getName() {
        return PipesConfig.getText("items." + toConfigKey() + ".name");
    }

    public Material getMaterial() {
        return material;
    }

    public ItemStack toItemStack() {
        if (item != null) {
            return new ItemStack(item);
        }
        ItemStack item = new ItemStack(this.material);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = Arrays.asList(PipesConfig.getText("items." + toConfigKey() + ".lore"),
                ChatColor.BLUE + "" + ChatColor.ITALIC + IDENTIFIER);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString(
                toString(),
                PipesConfig.getText("items." + toConfigKey() + ".name")
        ));
        meta.getCustomTagContainer().setCustomTag(TYPE_KEY, ItemTagType.STRING, toString());
        item.setItemMeta(meta);
        this.item = item;
        return new ItemStack(item);
    }
    
    @Deprecated
    public boolean check(Block block) {
        return block != null && check(block.getState(false));
    }
    
    public boolean check(BlockState blockState) {
        if (blockState == null || blockState.getType() != this.material || !(blockState instanceof InventoryHolder)) {
            return false;
        }

        String hidden = PipesUtil.getHiddenString(((Nameable) blockState).getCustomName());

        if (hidden != null && toString().equals(hidden.split(",")[0]) || IDENTIFIER.equals(hidden)) {
            return true;
        }

        return BlockInfoStorage.get().getBlockInfo(blockState.getLocation(), Pipes.getInstance()) != null;
    }

    public boolean check(ItemStack item) {
        if (item == null || item.getType() != this.material || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (!meta.getCustomTagContainer().isEmpty() && meta.getCustomTagContainer().hasCustomTag(TYPE_KEY, ItemTagType.STRING)) {
            return toString().equals(meta.getCustomTagContainer().getCustomTag(TYPE_KEY, ItemTagType.STRING));
        }

        List<String> lore = item.getItemMeta().getLore();
        String hidden = PipesUtil.getHiddenString(lore.get(lore.size() - 1));

        return hidden != null && (hidden.startsWith(toString()) || hidden.startsWith(IDENTIFIER));
    }

    /**
     * Get the enum name as a lowercase string with underscores replaced with dashes
     * @return  The enum name as a config key
     */
    public String toConfigKey() {
        return toString().toLowerCase().replace('_', '-');
    }
}
