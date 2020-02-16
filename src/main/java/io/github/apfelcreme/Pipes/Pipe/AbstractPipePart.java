package io.github.apfelcreme.Pipes.Pipe;

import de.minebench.blockinfostorage.BlockInfoStorage;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.GuiStorageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.github.apfelcreme.Pipes.Pipes;
import io.github.apfelcreme.Pipes.PipesConfig;
import io.github.apfelcreme.Pipes.PipesItem;
import io.github.apfelcreme.Pipes.PipesUtil;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Nameable;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
public abstract class AbstractPipePart {

    public static final NamespacedKey TYPE_KEY = new NamespacedKey(Pipes.getInstance(), "type");
    public static final NamespacedKey OPTIONS_KEY = new NamespacedKey(Pipes.getInstance(), "options");
    public static final NamespacedKey STORED_TYPE_KEY = new NamespacedKey(Pipes.getInstance(), "stored_type");

    private final PipesItem type;
    private final SimpleLocation location;
    private Map<Option<?>, Value<?>> options = new HashMap<>();

    protected AbstractPipePart(PipesItem type, Block block) {
        this.type = type;
        this.location = new SimpleLocation(block.getLocation());
        loadOptions(block);
    }
    
    /**
     * Get the type of this pipe part
     *
     * @return The type of this pipe part
     */
    public PipesItem getType() {
        return type;
    }

    /**
     * returns the location of this pipe part
     *
     * @return the location of this pipe part
     */
    public SimpleLocation getLocation() {
        return location;
    }

    /**
     * returns the inventory holder of pipe part
     *
     * @return the inventory holder of pipe part
     */
    public Container getHolder() {
        BlockState state = location.getBlock().getState(false);
        if (type.check(state)) {
            return (Container) state;
        }
        return null;
    }

    /**
     * Get a certain option value of this pipe part
     * @param option    The option to get
     * @return          The value of the option or <tt>null</tt> if it wasn't set and there is no default one
     */
    public <T> Value<T> getValue(Option<T> option) {
        return getValue(option, option.getDefaultValue());
    }

    /**
     * Get a certain option value of this  pipe part
     * @param option        The option to get
     * @param defaultValue  The default value to return if the value wasn't found
     * @return              The value of the option or <tt>null</tt> if it wasn't set
     */
    public <T> Value<T> getValue(Option<T> option, Value<T> defaultValue) {
        Value<?> value = options.getOrDefault(option, defaultValue);
        if (!option.isValid(value)) {
            options.remove(option);
            Pipes.getInstance().getLogger().log(Level.SEVERE, "Removed stored value " + value + " for option " + option + " from part at " + getLocation() + " because it's not compatible with value type <" + option.getValueType().getSimpleName() + ">!");
            return null;
        }
        return (Value<T>) value;
    }

    /**
     * Get a certain option of this pipe part
     * @param option    The option to get
     * @return          The value of the option or <tt>null</tt> if it wasn't set and there is no default one
     */
    public <T> T getOption(Option<T> option) {
        return getOption(option, option.getDefaultValue());
    }

    /**
     * Get a certain option of this  pipe part
     * @param option        The option to get
     * @param defaultValue  The default value to return if the value wasn't found
     * @return              The value of the option or <tt>null</tt> if it wasn't set
     */
    public <T> T getOption(Option<T> option, Value<T> defaultValue) {
        return getValue(option, defaultValue).getValue();
    }

    /**
     * Set an option of this output. This also saves the options to the block
     * @param option    The option to set
     * @param value     The value to set the option to
     * @throws IllegalArgumentException When the values type is not compatible with the option
     */
    public <T> void setOption(Option<T> option, Value<T> value) throws IllegalArgumentException {
        setOption(option, value, true);
    }

    /**
     * Set an option of this output.
     * @param option    The option to set
     * @param value     The value to set the option to
     * @param save      Whether or not to save the option after setting the value
     * @throws IllegalArgumentException When the values type is not compatible with the option
     */
    public <T> void setOption(Option<T> option, Value<T> value, boolean save) {
        if (!option.isValid(value)) {
            throw new IllegalArgumentException("The option " + option + "< " + option.getValueType().getSimpleName() + "> does not accept the value " + value + "!");
        }
        options.put(option, value);
        if (save) {
            Container holder = getHolder();
            if (holder != null) {
                if (value == null) {
                    holder.getPersistentDataContainer().remove(new NamespacedKey(Pipes.getInstance(), option.name()));
                } else {
                    holder.getPersistentDataContainer().set(
                            new NamespacedKey(Pipes.getInstance(), option.name()),
                            option.getTagType(),
                            value.getValue()
                    );
                }
            } else if (Pipes.hasBlockInfoStorage()) {
                Object v = value != null && value != option.getDefaultValue() ? value.getValue() : null;
                if (v instanceof Enum) {
                    v = ((Enum) v).name();
                }
                BlockInfoStorage.get().setBlockInfo(
                        getLocation().getLocation(),
                        new NamespacedKey(Pipes.getInstance(), option.name()),
                        v);
            }
        }
    }

    /**
     * Generate a mapped string of the options to write to the block's name
     * @return  The options as a string, mapped as option=value
     */
    protected String getOptionsString() {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Option<?>, Value<?>> option : options.entrySet()) {
            s.append(',').append(option.getKey().name()).append('=').append(option.getValue().getValue());
        }
        return s.toString();
    }

    public void showGui(Player player) {
        Container holder = getHolder();
        if (holder == null) {
            return;
        }

        InventoryGui gui = InventoryGui.get(holder);
        if (gui == null) {
            gui = new InventoryGui(Pipes.getInstance(), holder, holder.getCustomName(), getGuiSetup());

            gui.addElement(new GuiStorageElement('i', holder.getInventory()));
            gui.setFiller(PipesConfig.getGuiItemStack(getType().toConfigKey() + ".filler"));

            if (getOptions().length > 0) {
                gui.addElement(new StaticGuiElement('c',
                        PipesConfig.getGuiItemStack(getType().toConfigKey() + ".copy"),
                        click -> {
                            if (click.getEvent().getCursor() == null) {
                                Pipes.sendActionBar(click.getEvent().getWhoClicked(), PipesConfig.getText("error.notABook"));
                                
                            } else if (click.getEvent().getCursor().getType() == Material.BOOK
                                    || click.getEvent().getCursor().getType() == Material.WRITABLE_BOOK) {
                                ItemStack book = saveOptionsToBook();
                                book.setAmount(click.getEvent().getCursor().getAmount());
                                click.getEvent().getView().setCursor(book);
                                ((Player) click.getEvent().getWhoClicked()).updateInventory();
                                Pipes.sendActionBar(click.getEvent().getWhoClicked(), PipesConfig.getText("info.settings.bookCreated"));
                                
                            } else if (PipesItem.SETTINGS_BOOK.check(click.getEvent().getCursor())) {
                                if (click.getType() == ClickType.LEFT || click.getType() == ClickType.SHIFT_LEFT) {
                                    try {
                                        applyBook(click.getEvent().getCursor());
                                        click.getGui().draw();
                                        Pipes.sendMessage(click.getEvent().getWhoClicked(), PipesConfig.getText("info.settings.bookApplied"));
                                    } catch (IllegalArgumentException e){
                                        Pipes.sendMessage(click.getEvent().getWhoClicked(), e.getMessage());
                                    }
                                    
                                } else if (click.getType() == ClickType.RIGHT || click.getType() == ClickType.SHIFT_RIGHT) {
                                    ItemStack book = saveOptionsToBook();
                                    book.setAmount(click.getEvent().getCursor().getAmount());
                                    click.getEvent().getView().setCursor(book);
                                    ((Player) click.getEvent().getWhoClicked()).updateInventory();
                                    Pipes.sendActionBar(click.getEvent().getWhoClicked(), PipesConfig.getText("info.settings.bookUpdated"));
                                    
                                }
                            } else {
                                Pipes.sendActionBar(click.getEvent().getWhoClicked(), PipesConfig.getText("error.notABook"));
                            }
                            return true;
                        },
                        PipesConfig.getText("gui." + getType().toConfigKey() + ".copy")
                ));
            }

            GuiElementGroup optionsGroupLeft = new GuiElementGroup('s');
            gui.addElement(optionsGroupLeft);
            GuiElementGroup optionsGroupRight = new GuiElementGroup('z');
            gui.addElement(optionsGroupRight);
            for (Option<?> option : getOptions()) {
                if (option.getGuiPosition() == Option.GuiPosition.NONE) {
                    continue;
                }
                if (optionsGroupLeft.size() < optionsGroupLeft.getSlots().length
                        && (option.getGuiPosition() == Option.GuiPosition.LEFT
                        || option.getGuiPosition() == Option.GuiPosition.ANYWHERE
                        || optionsGroupRight.size() >= optionsGroupRight.getSlots().length)) {
                    optionsGroupLeft.addElement(option.getElement(this));
                } else {
                    optionsGroupRight.addElement(option.getElement(this));
                }
            }
            optionsGroupLeft.setFiller(gui.getFiller());
            optionsGroupRight.setFiller(gui.getFiller());
        }
        gui.show(player);
    }
    
    /**
     * Get the setup of the GUI with the character 'i' for the block's inventory, 's' and 'z' for settings, 'c' for the copy book
     * @return  The setup for the GUI
     */
    public abstract String[] getGuiSetup();

    /**
     * Get all possible options
     * @return  All possible options of this part
     */
    protected abstract Option<?>[] getOptions();

    /**
     * Returns the enum constant of this type with the specified name.
     * The string must match exactly an identifier used to declare an enum constant in this type.
     * (Extraneous whitespace characters are not permitted.)
     * @return  the enum constant with the specified name
     * @throws IllegalArgumentException if this enum type has no constant with the specified name
     */
    protected Option<?> getAvailableOption(String name) throws IllegalArgumentException {
        for (Option<?> option : getOptions()) {
            if (option.name().equalsIgnoreCase(name)) {
                return option;
            }
        }
        throw new IllegalArgumentException("No option with the name '" + name + "' defined!");
    }

    /**
     * Load the options from the storage
     * @param block The block to load the options from
     */
    private void loadOptions(Block block) {
        BlockState state = block.getState(false);
        if (state instanceof PersistentDataHolder && loadOptions((PersistentDataHolder) state)) {
            return;
        }

        if (Pipes.hasBlockInfoStorage()) {
            ConfigurationSection blockInfo = BlockInfoStorage.get().getBlockInfo(block, Pipes.getInstance());
            if (blockInfo != null) {
                for (String optionName : blockInfo.getKeys(false)) {
                    if ("type".equalsIgnoreCase(optionName)) {
                        if (state instanceof PersistentDataHolder) {
                            ((PersistentDataHolder) state).getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, getType().name());
                        }
                        continue;
                    }
                    try {
                        Option<?> option = getAvailableOption(optionName.toUpperCase());
                        Value value = option.parseValue(blockInfo.get(optionName));
                        if (value == null) continue;
                        setOption(option, value, false);
                    } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        throw new IllegalArgumentException(PipesConfig.getText("error.invalidSettingsBook",
                                "Invalid option" + optionName + "=" + blockInfo.get(optionName)));
                    }
                }
                if (state instanceof PersistentDataHolder) {
                    BlockInfoStorage.get().removeBlockInfo(block, Pipes.getInstance());
                }
                return;
            }
        }

        if (state instanceof Nameable) {
            String hidden = PipesUtil.getHiddenString(((Nameable) state).getCustomName());
            if (hidden != null) {
                try {
                    applyOptions(hidden);
                    if (state instanceof PersistentDataHolder) {
                        ((PersistentDataHolder) state).getPersistentDataContainer().set(TYPE_KEY, PersistentDataType.STRING, getType().name());
                    } else if (Pipes.hasBlockInfoStorage()) {
                        BlockInfoStorage.get().setBlockInfo(state.getLocation(), TYPE_KEY, getType().name());
                    }
                } catch (IllegalArgumentException e) {
                    Pipes.getInstance().getLogger().log(Level.WARNING, "Error while loading pipe part at " + getLocation() + "! " + e.getMessage());
                }
            }
        }
    }

    private boolean loadOptions(PersistentDataHolder dataHolder) {
        boolean found = false;
        PersistentDataContainer optionsContainer = dataHolder.getPersistentDataContainer().get(OPTIONS_KEY, PersistentDataType.TAG_CONTAINER);
        for (Option<?> option : getOptions()) {
            NamespacedKey key = new NamespacedKey(Pipes.getInstance(), option.name);
            if (optionsContainer != null && optionsContainer.has(key, option.getTagType())) {
                Object object = optionsContainer.get(key, option.getTagType());
                try {
                    Value value = option.parseValue(object);
                    setOption(option, value);
                    found = true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return found;
    }

    /**
     * Apply options from a string
     * @param optionString the string that the options are encoded in
     * @throws IllegalArgumentException thrown if the string is invalid
     */
    @Deprecated
    private void applyOptions(String optionString) throws IllegalArgumentException {
        boolean isBook = false;
        for (String group : optionString.split(",")) {
            String[] parts = group.split("=");
            if (parts.length < 2) {
                if (parts.length > 0) {
                    if (!isBook && parts[0].equals(PipesItem.SETTINGS_BOOK.toString())) {
                        isBook = true;
                    } else if (isBook && !parts[0].equals(getType().toString())) {
                        PipesItem storedItem;
                        try {
                            storedItem = PipesItem.valueOf(parts[0]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(PipesConfig.getText("error.unknownPipesItem",
                                    parts[0]));
                        }
                        throw new IllegalArgumentException(PipesConfig.getText("error.wrongBookType",
                                PipesConfig.getText("items." + storedItem.toConfigKey() + ".name")));
                    }
                }
                continue;
            }
            try {
                Option<?> option = getAvailableOption(parts[0].toUpperCase());
                Value value;
                if (option.getValueType() == Boolean.class) {
                    value = new Value<>(Boolean.parseBoolean(parts[1]));
                } else if (option.getValueType().isEnum()) {
                    Enum<?> e = (Enum) option.getDefaultValue().getValue();
                    value = new Value<>(e.valueOf(e.getDeclaringClass(), parts[1].toUpperCase()));
                } else if (option.getValueType() == String.class) {
                    value = new Value<>(parts[1]);
                } else {
                    // If all fails try to get a static method that can get the value
                    Method get = null;
                    for (String method : new String[]{"valueOf", "fromString", "parse"}) {
                        try {
                            get = option.getValueType().getMethod(method, String.class);
                            if (Modifier.isStatic(get.getModifiers())) {
                                break;
                            }
                        } catch (NoSuchMethodException ignored) { }
                    }
                    if (get == null) {
                        throw new IllegalArgumentException("Values of type " + option.getValueType() + " are not supported!");
                    }
                    value = new Value<>(get.invoke(null, parts[1]));
                }
                setOption(option, value);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(PipesConfig.getText("error.invalidSettingsBook",
                        "Invalid option" + parts[0] + "=" + parts[1]));
            }
        }
    }
    
    /**
     * Apply the settings stored in a book to this pipe part
     * @param book the book to apply
     * @throws IllegalArgumentException if the item is not a book or the settings stored are invalid
     */
    public void applyBook(ItemStack book) throws IllegalArgumentException {
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof BookMeta)) {
            throw new IllegalArgumentException("ItemStack needs to be a book!");
        }

        if (!meta.getPersistentDataContainer().has(OPTIONS_KEY, PersistentDataType.TAG_CONTAINER)) {
            if (meta.hasLore()) {
                List<String> lore = meta.getLore();
                String hidden = PipesUtil.getHiddenString(lore.get(lore.size() - 1));
                applyOptions(hidden);
                return;
            }
            throw new IllegalArgumentException("ItemStack does not have custom tags nor a custom lore!");
        }

        if (!meta.getPersistentDataContainer().has(STORED_TYPE_KEY, PersistentDataType.STRING)) {
            throw new IllegalArgumentException(PipesConfig.getText("error.unknownPipesItem", "null"));
        }

        String storedType = meta.getPersistentDataContainer().get(STORED_TYPE_KEY, PersistentDataType.STRING);
        if (storedType == null) {
            throw new IllegalArgumentException(PipesConfig.getText("error.unknownPipesItem", "null"));
        }

        if (!storedType.equals(getType().toString())) {
            PipesItem storedItem;
            try {
                storedItem = PipesItem.valueOf(storedType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(PipesConfig.getText("error.unknownPipesItem", storedType));
            }
            throw new IllegalArgumentException(PipesConfig.getText("error.wrongBookType",
                    PipesConfig.getText("items." + storedItem.toConfigKey() + ".name")));
        }

        loadOptions(meta);
    }
    
    /**
     * Save the options of this part to a book
     * @return The changed item stack
     * @throws IllegalArgumentException if the item passed is not a book
     */
    private ItemStack saveOptionsToBook() {
    
        ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) bookItem.getItemMeta();
        meta.setAuthor(PipesItem.getIdentifier());
        meta.setDisplayName(ChatColor.RESET + "" + ChatColor.WHITE + PipesUtil.hideString(
                toString(),
                PipesConfig.getText("items." + PipesItem.SETTINGS_BOOK.toConfigKey() + ".name", getType().getName())
        ));
        
        List<String> optionsLore = new ArrayList<>();
        List<BaseComponent[]> pages = new ArrayList<>();
        List<ComponentBuilder> optionsPage = new ArrayList();
        optionsPage.add(new ComponentBuilder(""));

        meta.getPersistentDataContainer().set(STORED_TYPE_KEY, PersistentDataType.STRING, getType().toString());

        PersistentDataContainer optionsContainer = meta.getPersistentDataContainer().getAdapterContext().newPersistentDataContainer();
        for (Option<?> option : getOptions()) {
            option.store(this, optionsContainer);

            String shortDesc = PipesConfig.getText("options." + getType().toConfigKey() + "." + option.toConfigKey() + ".description");
            Value<?> value = getValue(option);

            if (value.getValue() instanceof Boolean) {
                optionsLore.add(((Boolean) value.getValue() ? ChatColor.GREEN : ChatColor.RED) + shortDesc);
            } else {
                shortDesc = ChatColor.DARK_PURPLE + shortDesc + ": " + ChatColor.BLUE + value.getValue().toString();
                optionsLore.add(shortDesc);
            }
    
            BaseComponent[] optionEntry = TextComponent.fromLegacyText(shortDesc);
            for (BaseComponent c : optionEntry) {
                if (value.getValue() instanceof Boolean) {
                        c.setColor(((Boolean) value.getValue() ? ChatColor.DARK_GREEN : ChatColor.DARK_RED));
                }
                c.setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        TextComponent.fromLegacyText(PipesConfig.getText("options." + getType().toConfigKey() + "." + option.toConfigKey().toLowerCase() + "." + value.getValue().toString().toLowerCase()))
                ));
            }
    
            ComponentBuilder pageBuilder = optionsPage.get(optionsPage.size() - 1);
            pageBuilder.append("\n");
            
            String pageStr = TextComponent.toPlainText(pageBuilder.create()) + TextComponent.toPlainText(optionEntry);
            if (pageStr.length() > 255 || pageStr.split("\n").length > 13) {
                pages.add(pageBuilder.create());
                optionsPage.add(pageBuilder = new ComponentBuilder(""));
            }
            
            pageBuilder.append(optionEntry);
        }

        meta.getPersistentDataContainer().set(OPTIONS_KEY, PersistentDataType.TAG_CONTAINER, optionsContainer);
        
        if (!optionsPage.isEmpty()) {
            pages.add(optionsPage.get(optionsPage.size() - 1).create());
        }
        
        meta.spigot().setPages(pages);
        
        List<String> lore = new ArrayList<>();
        lore.addAll(Arrays.asList(
                PipesConfig.getText("items." + PipesItem.SETTINGS_BOOK.toConfigKey() + ".lore",
                        getType().getName(), optionsLore.stream().collect(Collectors.joining("\n"))
                ).split("\n")
        ));
        lore.add(ChatColor.BLUE + "" + ChatColor.ITALIC + PipesUtil.hideString(
                PipesItem.SETTINGS_BOOK.toString() + "," + toString(),
                PipesItem.getIdentifier()
        ));
        
        meta.setLore(lore);
        bookItem.setItemMeta(meta);
        
        return bookItem;
    }

    private <T, Z> void containerSet(PersistentDataContainer container, NamespacedKey key, PersistentDataType<Z, T> type, Value<T> value) {
        container.set(key, type, value.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return getLocation() != null ? !getLocation().equals(((AbstractPipePart) o).getLocation()) : ((AbstractPipePart) o).getLocation() != null;
    }

    @Override
    public String toString() {
        return getType().toString() + getOptionsString();
    }

    @Override
    public int hashCode() {
        return getLocation().hashCode();
    }

    public static class Option<T> {
        private final String name;
        private final Value<T> defaultValue;
        private final Class<?> valueType;
        private final Value<T>[] possibleValues;
        private final GuiPosition guiPosition;

        /**
         * An option that this pipe part can have
         * @param defaultValue  The default value when none is set
         */
        Option(String name, GuiPosition guiPosition, Value<T> defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.valueType = defaultValue.getValue().getClass();
            possibleValues = new Value[0];
            this.guiPosition = guiPosition;
        }

        /**
         * An option that this pipe part can have
         * @param defaultValue  The default value when none is set
         */
        Option(String name,  Value<T> defaultValue) throws IllegalArgumentException {
            this(name, GuiPosition.ANYWHERE, defaultValue);
        }

        /**
         * An option that this pipe part can have
         * @param possibleValues    An array of possible values that this option accepts
         * @throws IllegalArgumentException Thrown when there are less than two possible values defined
         */
        Option(String name, GuiPosition guiPosition, Value<T>... possibleValues) throws IllegalArgumentException {
            if (possibleValues.length < 2) {
                throw new IllegalArgumentException("An option needs to have at least two values!");
            }
            this.name = name;
            this.possibleValues = possibleValues;
            defaultValue = possibleValues[0];
            valueType = defaultValue.getValue().getClass();
            this.guiPosition = guiPosition;
        }

        /**
         * An option that this pipe part can have
         * @param possibleValues    An array of possible values that this option accepts
         * @throws IllegalArgumentException Thrown when there are less than two possible values defined
         */
        Option(String name,  Value<T>... possibleValues) throws IllegalArgumentException {
            this(name, GuiPosition.ANYWHERE, possibleValues);
        }

        public String name() {
            return name;
        }

        /**
         * Get the class of the values that this option accepts
         * @return  The class of the values that this option accepts
         */
        public Class<?> getValueType() {
            return valueType;
        }

        /**
         * Get the default value for this option if it isn't set
         * @return  The default value of this option
         */
        public Value<T> getDefaultValue() {
            return defaultValue;
        }

        /**
         * Get the array of possible values
         * @return  The array of possible values
         */
        public Value<T>[] getPossibleValues() {
            return possibleValues;
        }

        public <Z> PersistentDataType<Z, T> getTagType() {
            return getDefaultValue().getTagType();
        }


        /**
         * Get the enum name as a lowercase string with underscores replaced with dashes
         * @return  The enum name as a config key
         */
        public String toConfigKey() {
            return name().toLowerCase().replace('_', '-');
        }

        /**
         * Check whether or not this option can be set to a value
         * @param value The value to check
         * @return      <tt>true</tt> if this option accepts it; <tt>false</tt> otherwhise
         */
        public boolean isValid(Value<?> value) {
            if (value.getValue().getClass() != getValueType()) {
                return false;
            }
            if (getPossibleValues().length == 0) {
                return true;
            }
            for (Value v : getPossibleValues()) {
                if (v.getValue().equals(value.getValue())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the GUI element of this option for a certain pipe part
         * @param pipePart  The pipe part to get the element for
         * @return          The GuiStateElement of this option
         */
        public GuiStateElement getElement(AbstractPipePart pipePart) {
            List<GuiStateElement.State> states = new ArrayList<>();
            for (Value v : getPossibleValues()) {
                ItemStack icon = PipesConfig.getGuiItemStack(pipePart.getType().toConfigKey() + "." + toConfigKey().toLowerCase() + "." + v.getValue().toString().toLowerCase());

                states.add(new GuiStateElement.State(
                        change -> pipePart.setOption(this, v),
                        v.getValue().toString(),
                        icon,
                        PipesConfig.getText("options." + pipePart.getType().toConfigKey() + "." + toConfigKey().toLowerCase() + "." + v.getValue().toString().toLowerCase())
                ));
            }
            return new GuiStateElement(
                    name().charAt(0),
                    () -> pipePart.getOption(this).toString(),
                    states.toArray(new GuiStateElement.State[states.size()])
            );
        }

        /**
         * Get the position where to display this option in the GUI
         * @return  The position to display this option in the GUI in
         */
        public GuiPosition getGuiPosition() {
            return guiPosition;
        }

        public void store(AbstractPipePart pipePart, PersistentDataContainer container) {
            Value<T> value = pipePart.getValue(this);
            if (value == null || value.getValue() == null) {
                container.remove(new NamespacedKey(Pipes.getInstance(), name));
            } else {
                container.set(new NamespacedKey(Pipes.getInstance(), name()), getTagType(), value.getValue());
            }
        }

        public Value<T> parseValue(Object object) throws IllegalAccessException, InvocationTargetException {
            Value<T> value;
            if (object == null) {
                return null;
            } else if (object.getClass() == getValueType()) {
                value = new Value(object);
            } else if (getValueType() == Boolean.class && object instanceof Boolean) {
                value = new Value<>((T) object);
            } else if (getValueType().isEnum()) {
                Enum<?> e = (Enum) getDefaultValue().getValue();
                value = new Value<>((T) e.valueOf(e.getDeclaringClass(), object.toString().toUpperCase()));
            } else if (getValueType() == String.class) {
                value = new Value<>((T) object.toString());
            } else {
                // If all fails try to get a static method that can get the value
                Method get = null;
                for (String method : new String[]{"valueOf", "fromString", "parse"}) {
                    try {
                        get = getValueType().getMethod(method, String.class);
                        if (Modifier.isStatic(get.getModifiers())) {
                            break;
                        }
                    } catch (NoSuchMethodException ignored) { }
                }
                if (get == null) {
                    throw new IllegalArgumentException("Values of type " + getValueType() + " are not supported!");
                }
                value = new Value<>((T) get.invoke(null, object.toString()));
            }
            return value;
        }

        enum GuiPosition {
            ANYWHERE, LEFT, RIGHT, NONE
        }
    }

    public static abstract class OptionsList {
    }

    public static class Value<T> {
        public static final Value<Boolean> TRUE = new Value<>(true);
        public static final Value<Boolean> FALSE = new Value<>(false);
        public static final PersistentDataType<Byte,Boolean> BOOLEAN_TAG_TYPE = new PersistentDataType<Byte, Boolean>() {
            @Override
            public Class<Byte> getPrimitiveType() {
                return Byte.class;
            }

            @Override
            public Class<Boolean> getComplexType() {
                return Boolean.class;
            }

            @Override
            public Byte toPrimitive(Boolean complex, PersistentDataAdapterContext context) {
                return complex != null && complex ? (byte) 1 : (byte) 0;
            }

            @Override
            public Boolean fromPrimitive(Byte primitive, PersistentDataAdapterContext context) {
                return primitive != null && primitive == 1;
            }
        };

        private final T value;

        public Value(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public String toString() {
            return "Value<" + value.getClass().getSimpleName() + ">{value=" + value.toString() + "}";
        }

        public <Z> PersistentDataType<Z, T> getTagType() {
            if (getValue() instanceof Boolean) {
                return (PersistentDataType<Z, T>) Value.BOOLEAN_TAG_TYPE;
            } else if (getValue() instanceof Byte) {
                return (PersistentDataType<Z, T>) PersistentDataType.BYTE;
            } else if (getValue() instanceof Short) {
                return (PersistentDataType<Z, T>) PersistentDataType.SHORT;
            } else if (getValue() instanceof Long) {
                return (PersistentDataType<Z, T>) PersistentDataType.LONG;
            } else if (getValue() instanceof Integer) {
                return (PersistentDataType<Z, T>) PersistentDataType.INTEGER;
            } else if (getValue() instanceof Float) {
                return (PersistentDataType<Z, T>) PersistentDataType.FLOAT;
            } else if (getValue() instanceof Double) {
                return (PersistentDataType<Z, T>) PersistentDataType.DOUBLE;
            } else if (getValue() instanceof String) {
                return (PersistentDataType<Z, T>) PersistentDataType.STRING;
            } else if (getValue() instanceof Enum) {
                return (PersistentDataType<Z, T>) new EnumTagType((Class<? extends Enum>) getValue().getClass());
            }
            throw new IllegalArgumentException(getValue().getClass() + " types are not supported!");
        }

        public abstract static class TagType<P, X> implements PersistentDataType<P, X> {
            private final Class<P> primitiveType;
            private final Class<X> complexType;

            protected TagType(Class<P> primitiveType, Class<X> complexType) {
                this.primitiveType = primitiveType;
                this.complexType = complexType;
            }

            @Override
            public Class<P> getPrimitiveType() {
                return primitiveType;
            }

            @Override
            public Class<X> getComplexType() {
                return complexType;
            }
        }

        public static class EnumTagType extends TagType<String, Enum> {

            protected EnumTagType(Class<? extends Enum> enumType) {
                super(String.class, (Class<Enum>) enumType);
            }

            @Override
            public String toPrimitive(Enum complex, PersistentDataAdapterContext context) {
                return complex.name();
            }

            @Override
            public Enum fromPrimitive(String primitive, PersistentDataAdapterContext context) {
                try {
                    return Enum.valueOf(getComplexType(), primitive.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
    }
}
