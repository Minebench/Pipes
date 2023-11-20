package io.github.apfelcreme.Pipes;

/*
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public enum PipesItem {
    PIPE_INPUT(Material.DISPENSER),
    PIPE_OUTPUT(Material.DROPPER),
    CHUNK_LOADER(Material.FURNACE),
    SETTINGS_BOOK(Material.WRITTEN_BOOK);

    private static final String IDENTIFIER = "Pipes";
    private static final NamespacedKey TYPE_KEY = new NamespacedKey(Pipes.getInstance(), "type");
    public static final NamespacedKey STORED_TYPE_KEY = new NamespacedKey(Pipes.getInstance(), "stored_type");

    private final Material material;
    private ItemStack item;

    PipesItem(Material material) {
        this.material = material;
    }

    public static String getIdentifier() {
        return IDENTIFIER;
    }

    public String getName() {
        return PipesConfig.getText(PipesConfig.getDefaultLocale(), "items." + toConfigKey() + ".name");
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
        List<Component> lore = Arrays.asList(
                Component.translatable("pipes.items." + toConfigKey() + ".lore", PipesConfig.getText(PipesConfig.getDefaultLocale(), "items." + toConfigKey() + ".lore")),
                Component.text(IDENTIFIER).style(Style.style(NamedTextColor.BLUE, TextDecoration.ITALIC))
        );
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
        Component displayName = Component.translatable("pipes.items." + toConfigKey() + ".name",
                PipesConfig.getText(PipesConfig.getDefaultLocale(), "items." + toConfigKey() + ".name"),
                Style.style().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).build());
        meta.displayName(displayName);
        meta.getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, toString());
        meta.setCustomModelData(PipesConfig.getCustomModelDataOffset() + ordinal());
        item.setItemMeta(meta);
        this.item = item;
        return new ItemStack(item);
    }
    
    public boolean check(BlockState blockState) {
        if (blockState == null || blockState.getType() != this.material || !(blockState instanceof Container) || (((Container) blockState).getPersistentDataContainer() == null)) {
            return false;
        }

        if (((Container) blockState).getPersistentDataContainer().has(TYPE_KEY, PersistentDataType.STRING)) {
            return true;
        }

        if (Pipes.hasBlockInfoStorage()) {
            return BlockInfoStorage.get().getBlockInfo(blockState.getLocation(), Pipes.getInstance()) != null;
        }

        return false;
    }

    public boolean check(ItemStack item) {
        if (item == null || item.getType() != this.material || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(TYPE_KEY, PersistentDataType.STRING)) {
            return toString().equals(meta.getPersistentDataContainer().get(TYPE_KEY, PersistentDataType.STRING));
        } else if (SETTINGS_BOOK.getMaterial() == item.getType() && meta.getPersistentDataContainer().has(STORED_TYPE_KEY, PersistentDataType.STRING)) {
            return true;
        }

        return false;
    }

    /**
     * Get the enum name as a lowercase string with underscores replaced with dashes
     * @return  The enum name as a config key
     */
    public String toConfigKey() {
        return toString().toLowerCase().replace('_', '-');
    }
}
